package de.jpx3.intave.module.dispatch;

import de.jpx3.intave.adapter.MinecraftVersion;
import de.jpx3.intave.adapter.MinecraftVersions;
import de.jpx3.intave.share.Position;
import de.jpx3.intave.share.PositionMoveRotation;
import de.jpx3.intave.share.Teleport;
import de.jpx3.intave.user.User;
import de.jpx3.intave.user.UserFactory;
import de.jpx3.intave.user.meta.MovementMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.*;

class HazardRecoveryTest {
  private final TeleportController controller = new TeleportController();
  private User user;
  private MovementMetadata movement;

  @BeforeEach
  void setup() {
    MinecraftVersion.setCurrent(MinecraftVersions.VER1_21_4);
    user = UserFactory.createFallback();
    movement = user.meta().movement();
  }

  @Test
  void ordinaryTeleportOnlyWithholdsServerDelivery() {
    movement.pendingTeleports.get().add(teleport(0));
    assertTrue(controller.processMovementPackets(user));
    assertFalse(controller.forwardMovementPackets(user));
  }

  @Test
  void correctionBlocksBeforeItsPacketIsSentAndRejectsDuplicateRecovery() {
    long recovery = movement.beginRecovery();
    assertTrue(recovery > 0);
    assertFalse(controller.processMovementPackets(user));
    assertFalse(controller.forwardMovementPackets(user));
    assertTrue(movement.dropPostTickMotionProcessing);
    assertFalse(controller.setbackTeleportsAllowed(user));
    assertEquals(-1, movement.beginRecovery());
    assertTrue(movement.isRecovering(recovery));
  }

  @Test
  void onlyConfirmedFinalCorrectionCanReleaseMovement() {
    long recovery = movement.beginRecovery();
    Teleport intermediate = teleport(0);
    intermediate.accept();
    assertFalse(movement.completeRecovery(intermediate));

    movement.teleportSequence = 1;
    movement.finishRecoveryOnNextTeleport(recovery);
    assertFalse(movement.isRecovering(recovery));
    assertFalse(movement.completeRecovery(intermediate));

    Teleport last = teleport(1);
    assertFalse(movement.completeRecovery(last));
    last.accept();
    movement.pendingTeleports.get().add(last);
    assertFalse(movement.completeRecovery(last));
    assertFalse(controller.processMovementPackets(user));
    movement.pendingTeleports.get().removeFirst();
    assertTrue(movement.completeRecovery(last));
    assertTrue(controller.processMovementPackets(user));
    assertTrue(controller.forwardMovementPackets(user));
    assertTrue(controller.setbackTeleportsAllowed(user));
  }

  @Test
  void retryCanCompleteRecoveryButAnEarlierAcknowledgementCannot() {
    long recovery = movement.beginRecovery();
    movement.teleportSequence = 5;
    movement.finishRecoveryOnNextTeleport(recovery);
    Teleport old = teleport(4);
    old.accept();
    assertFalse(movement.completeRecovery(old));
    Teleport unrelated = teleport(6);
    unrelated.accept();
    assertFalse(movement.completeRecovery(unrelated));
    Teleport retry = teleport(5);
    retry.accept();
    assertTrue(movement.completeRecovery(retry));
  }

  @Test
  void staleScheduledWorkCannotFinishANewerRecovery() {
    long oldRecovery = movement.beginRecovery();
    movement.replaceRecoveryWithExternalTeleport();
    assertFalse(movement.isRecovering(oldRecovery));
    Teleport external = teleport(0);
    external.accept();
    assertTrue(movement.completeRecovery(external));
    long newRecovery = movement.beginRecovery();
    movement.finishRecoveryOnNextTeleport(oldRecovery);
    assertFalse(movement.isRecovering(oldRecovery));
    assertTrue(movement.isRecovering(newRecovery));
    assertFalse(movement.completeRecovery(external));
  }

  private Teleport teleport(long sequence) {
    return new Teleport(sequence, OptionalInt.of((int) sequence),
      PositionMoveRotation.noMotionRelativePosition(new Position(0, 84, 0)), Collections.emptySet());
  }
}
