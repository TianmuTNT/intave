/*
 * Copyright 2026 Intave
 *
 * This software is licensed under the PolyForm Perimeter License 1.0.0.
 * You may use this software for any purpose, except for providing to
 * others any product that competes with the software.
 *
 * A copy of the license is available at:
 *   https://polyformproject.org/licenses/perimeter/1.0.0/
 */

package de.jpx3.intave.module.dispatch;

import de.jpx3.intave.share.Position;
import de.jpx3.intave.share.PositionMoveRotation;
import de.jpx3.intave.share.Teleport;
import de.jpx3.intave.user.meta.MovementMetadata;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DesyncWatchdogTest {

  @Test
  void rawPositionDisagreementIsStillDetectedOutsideTeleports() {
    DesyncWatchdog.PositionBundle positions = positions(
      new Position(1, 2, 3),
      new Position(1, 2, 3),
      new Position(100, 200, 300),
      false
    );

    assertTrue(positions.anyDesynced());
    assertTrue(positions.serverAndPrefilteredPendingPositionDesynced());
    assertTrue(positions.intaveAcceptedAndPrefilteredPendingPositionDesynced());
  }

  @Test
  void serverAndVerifiedDifferenceIsDetected() {
    DesyncWatchdog.PositionBundle positions = positions(
      new Position(0, 0, 0),
      new Position(0, 0, 4.0001),
      new Position(0, 0, 4.0001),
      false
    );

    assertTrue(positions.anyDesynced());
  }

  @Test
  void fourBlockDifferenceIsWithinTheAllowedBoundary() {
    DesyncWatchdog.PositionBundle positions = positions(
      new Position(0, 0, 0),
      new Position(0, 0, 4),
      new Position(0, 0, 4),
      false
    );

    assertFalse(positions.anyDesynced());
  }

  @Test
  void vehiclePositionDoesNotRequireResynchronization() {
    DesyncWatchdog.PositionBundle positions = positions(
      new Position(0, 0, 0),
      new Position(100, 100, 100),
      new Position(100, 100, 100),
      true
    );

    assertFalse(positions.anyDesynced());
  }

  @Test
  void teleportConfirmationWindowIsPending() {
    MovementMetadata movement = new MovementMetadata(null, null);

    assertFalse(DesyncWatchdog.teleportPending(movement));
    movement.pendingTeleports.get().add(new Teleport(0, OptionalInt.of(1),
      PositionMoveRotation.noMotionRelativePosition(new Position(1, 2, 3)), Collections.emptySet()));
    assertTrue(DesyncWatchdog.teleportPending(movement));

    movement.pendingTeleports.get().add(new Teleport(1, OptionalInt.of(2),
      PositionMoveRotation.noMotionRelativePosition(new Position(4, 5, 6)), Collections.emptySet()));
    movement.pendingTeleports.get().removeFirst();
    assertTrue(DesyncWatchdog.teleportPending(movement));

    movement.pendingTeleports.get().removeFirst();
    assertFalse(DesyncWatchdog.teleportPending(movement));
  }

  private DesyncWatchdog.PositionBundle positions(
    Position server,
    Position verified,
    Position raw,
    boolean inVehicle
  ) {
    return new DesyncWatchdog.PositionBundle(server, verified, raw, inVehicle);
  }
}
