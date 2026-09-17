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

package de.jpx3.intave.check.combat.heuristics.combatpatterns.accuracy;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import de.jpx3.intave.check.combat.Heuristics;
import de.jpx3.intave.check.combat.heuristics.ClassicHeuristic;
import de.jpx3.intave.check.combat.heuristics.HeuristicsClassicType;
import de.jpx3.intave.math.MathHelper;
import de.jpx3.intave.module.linker.packet.PacketSubscription;
import de.jpx3.intave.module.tracker.entity.Entity;
import de.jpx3.intave.packet.reader.EntityUseReader;
import de.jpx3.intave.packet.reader.PacketReaders;
import de.jpx3.intave.user.User;
import de.jpx3.intave.user.meta.AttackMetadata;
import de.jpx3.intave.user.meta.CheckCustomMetadata;
import org.bukkit.entity.Player;

import static de.jpx3.intave.module.linker.packet.PacketId.Client.*;

public final class AccuracyLongTermHeuristic extends ClassicHeuristic<AccuracyLongTermHeuristic.ClickAccuracyMeta> {
  public AccuracyLongTermHeuristic(Heuristics parentCheck) {
    super(parentCheck, HeuristicsClassicType.ATTACK_ACCURACY, ClickAccuracyMeta.class);
  }

  @PacketSubscription(
    packetsIn = {
      ATTACK_ENTITY, USE_ENTITY, ARM_ANIMATION
    }
  )
  public void evaluateFightAccuracy(PacketEvent event) {
    Player player = event.getPlayer();
    User user = userOf(player);
    if (!user.meta().protocol().sendsAttackSwingPackets()) return;
    AttackMetadata attackData = user.meta().attack();
    ClickAccuracyMeta heuristicMeta = metaOf(user);
    PacketType packetType = event.getPacketType();
    PacketContainer packet = event.getPacket();
    Entity entity = attackData.lastAttackedEntity();
    if (entity == null || !entity.moving(0.05) || entity.ticksAlive < 200) {
      return;
    }
    if (!attackData.recentlyAttacked(500) || attackData.recentlySwitchedEntity(1000)) {
      return;
    }
    if (packetType == PacketType.Play.Client.ARM_ANIMATION) {
      heuristicMeta.swings++;
    } else {
      boolean isAttack;
      try (EntityUseReader reader = PacketReaders.readerOf(packet)) {
        isAttack = reader.isAttackPacket();
      } catch (Exception e) {
	      throw new RuntimeException(e);
      }
	    if (isAttack) {
        heuristicMeta.attacks++;
        heuristicMeta.swings--;
        double failRate = (heuristicMeta.swings / heuristicMeta.attacks) * 100.0;
//        Synchronizer.synchronize(() -> player.sendMessage(String.valueOf(failRate)));
        if (heuristicMeta.attacks > 80) {
          if (failRate >= 0 && failRate < 3) {
            flag(
              user,
              "maintains high attack accuracy",
              "fail rate: " + MathHelper.formatDouble(failRate, 2) + "%"
            );
          }
          heuristicMeta.attacks = 0;
          heuristicMeta.swings = 0;
        }
      }
    }
  }

  public static class ClickAccuracyMeta extends CheckCustomMetadata {
    public double attacks;
    public double swings;
  }
}
