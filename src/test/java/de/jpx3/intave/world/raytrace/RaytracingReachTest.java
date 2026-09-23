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

package de.jpx3.intave.world.raytrace;

import de.jpx3.intave.adapter.MinecraftVersion;
import de.jpx3.intave.adapter.MinecraftVersions;
import de.jpx3.intave.block.cache.MockFullBlockStaticPlane;
import de.jpx3.intave.check.combat.AttackRaytrace;
import de.jpx3.intave.player.attribute.Attribute;
import de.jpx3.intave.player.attribute.AttributeModifier;
import de.jpx3.intave.share.BoundingBox;
import de.jpx3.intave.share.Position;
import de.jpx3.intave.test.FakePlayerFactory;
import de.jpx3.intave.user.User;
import de.jpx3.intave.user.UserFactory;
import de.jpx3.intave.user.UserRepository;
import de.jpx3.intave.user.meta.AbilityMetadata;
import de.jpx3.intave.user.meta.ProtocolMetadata;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static de.jpx3.intave.player.attribute.AttributeModifier.Operation.ADD_NUMBER;
import static de.jpx3.intave.world.raytrace.EntityRaytraceBlockConstraint.IGNORE_BLOCKS;
import static org.junit.jupiter.api.Assertions.*;

class RaytracingReachTest {
  @BeforeEach
  void setUp() {
    MinecraftVersion.setCurrent(MinecraftVersions.VER1_21_4);
  }

  @Test
  void registersInteractionRangeUnderIts1205Key() {
    MinecraftVersion.setCurrent(MinecraftVersions.VER1_20_5);
    AbilityMetadata abilities = new AbilityMetadata(playerIn(GameMode.SURVIVAL));

    Attribute attribute = abilities.findAttribute("player.entity_interaction_range");
    assertNotNull(attribute);
    assertEquals("player.entity_interaction_range", attribute.attributeKey());
    assertEquals(3.0D, abilities.entityInteractionRange());
  }

  @Test
  void registersBlockInteractionRangeUnderIts1205Key() {
    MinecraftVersion.setCurrent(MinecraftVersions.VER1_20_5);
    AbilityMetadata abilities = new AbilityMetadata(playerIn(GameMode.SURVIVAL));

    Attribute attribute = abilities.findAttribute("player.block_interaction_range");
    assertNotNull(attribute);
    assertEquals("player.block_interaction_range", attribute.attributeKey());
    assertEquals(4.5D, abilities.blockInteractionRange());
  }

  @Test
  void remapsInteractionRangeToIts1212Key() {
    MinecraftVersion.setCurrent(MinecraftVersions.VER1_21_2);
    AbilityMetadata abilities = new AbilityMetadata(playerIn(GameMode.SURVIVAL));

    Attribute attribute = abilities.findAttribute("player.entity_interaction_range");
    assertNotNull(attribute);
    assertEquals("entity_interaction_range", attribute.attributeKey());
    assertSame(attribute, abilities.findAttribute("entity_interaction_range"));
  }

  @Test
  void remapsBlockInteractionRangeToIts1212Key() {
    MinecraftVersion.setCurrent(MinecraftVersions.VER1_21_2);
    AbilityMetadata abilities = new AbilityMetadata(playerIn(GameMode.SURVIVAL));

    Attribute attribute = abilities.findAttribute("player.block_interaction_range");
    assertNotNull(attribute);
    assertEquals("block_interaction_range", attribute.attributeKey());
    assertSame(attribute, abilities.findAttribute("block_interaction_range"));
  }

  @Test
  void usesTrackedInteractionRangeModifiersForModernClients() {
    AbilityMetadata abilities = new AbilityMetadata(playerIn(GameMode.SURVIVAL));
    addReachModifier(abilities, 2.0D);

    assertEquals(5.0D, abilities.entityInteractionRange());
    assertEquals(5.0F, Raytracing.reachDistanceOf(abilities, protocol(ProtocolMetadata.VER_1_20_5)));
  }

  @Test
  void usesTrackedBlockInteractionRangeForModernClients() {
    AbilityMetadata abilities = new AbilityMetadata(playerIn(GameMode.SURVIVAL));
    abilities.modifyBaseValue("player.block_interaction_range", 8.0D);

    assertEquals(8.0D, abilities.blockInteractionRange());
    assertEquals(8.0D, Raytracing.blockReachDistanceOf(abilities, protocol(ProtocolMetadata.VER_1_20_5)));
  }

  @Test
  void keepsLegacyClientReachSemantics() {
    AbilityMetadata abilities = new AbilityMetadata(playerIn(GameMode.CREATIVE));
    abilities.modifyBaseValue("player.entity_interaction_range", 40.0D);

    assertEquals(5.0F, Raytracing.reachDistanceOf(abilities, protocol(ProtocolMetadata.VER_1_20_3)));
  }

  @Test
  void keepsLegacyClientBlockReachSemantics() {
    AbilityMetadata abilities = new AbilityMetadata(playerIn(GameMode.CREATIVE));
    abilities.modifyBaseValue("player.block_interaction_range", 40.0D);

    assertEquals(5.0D, Raytracing.blockReachDistanceOf(abilities, protocol(ProtocolMetadata.VER_1_20_3)));
  }

  @Test
  void fallsBackWhenTheServerHasNoInteractionRangeAttribute() {
    MinecraftVersion.setCurrent(MinecraftVersions.VER1_20_2);
    AbilityMetadata abilities = new AbilityMetadata(playerIn(GameMode.SURVIVAL));

    assertEquals(3.0D, abilities.entityInteractionRange());
    assertEquals(3.0F, Raytracing.reachDistanceOf(abilities, protocol(ProtocolMetadata.VER_1_20_5)));
  }

  @Test
  void fallsBackWhenTheServerHasNoBlockInteractionRangeAttribute() {
    MinecraftVersion.setCurrent(MinecraftVersions.VER1_20_2);
    AbilityMetadata abilities = new AbilityMetadata(playerIn(GameMode.SURVIVAL));

    assertEquals(4.5D, abilities.blockInteractionRange());
    assertEquals(4.5D, Raytracing.blockReachDistanceOf(abilities, protocol(ProtocolMetadata.VER_1_20_5)));
  }

  @Test
  void clampsInteractionRangeLikeTheClientAttribute() {
    AbilityMetadata abilities = new AbilityMetadata(playerIn(GameMode.SURVIVAL));
    abilities.modifyBaseValue("player.entity_interaction_range", 80.0D);

    assertEquals(64.0D, abilities.entityInteractionRange());
    assertEquals(64.0F, Raytracing.reachDistanceOf(abilities, protocol(ProtocolMetadata.VER_1_20_5)));
  }

  @Test
  void clampsBlockInteractionRangeLikeTheClientAttribute() {
    AbilityMetadata abilities = new AbilityMetadata(playerIn(GameMode.SURVIVAL));
    abilities.modifyBaseValue("player.block_interaction_range", 80.0D);

    assertEquals(64.0D, abilities.blockInteractionRange());
    assertEquals(64.0D, Raytracing.blockReachDistanceOf(abilities, protocol(ProtocolMetadata.VER_1_20_5)));
  }

  @Test
  void distinguishesTenBlockHitFromMiss() {
    Raytrace hit = new Raytrace(new Position(), new Position(0, 0, 10), 10.0D);
    Raytrace miss = new Raytrace(new Position(), null, Raytrace.MISS_DISTANCE);

    assertFalse(hit.missed());
    assertEquals(AttackRaytrace.RaytraceResult.VALID, AttackRaytrace.RaytraceResult.of(hit, 12.0D));
    assertTrue(miss.missed());
    assertEquals(AttackRaytrace.RaytraceResult.MISS, AttackRaytrace.RaytraceResult.of(miss, 12.0D));
  }

  @Test
  void tracesPastTheOldSixBlockLimit() {
    Player player = playerIn(GameMode.SURVIVAL);
    MockFullBlockStaticPlane blocks = MockFullBlockStaticPlane.createWithHorizontalPlaneAt(-10);
    User user = UserFactory.createTestUserFor(player, (ignored, key) -> switch (key) {
      case "blockCache" -> blocks;
      case "protocolVersion" -> ProtocolMetadata.VER_1_21_4;
      default -> null;
    });
    UserRepository.manuallyRegisterUser(player, user);
    try {
      user.meta().abilities().modifyBaseValue("player.entity_interaction_range", 12.0D);
      BoundingBox target = new BoundingBox(-0.3D, 0.0D, 9.5D, 0.3D, 2.0D, 10.1D);

      Raytrace result = Raytracing.entityRaytrace(
        player, target, 0.0D,
        0.0D, 0.0D, 0.0D,
        0.0F, 0.0F, 0.0D,
        IGNORE_BLOCKS
      );

      assertFalse(result.missed());
      assertTrue(result.reach() > 6.0D);
      assertEquals(9.5D, result.reach(), 1.0E-6D);
    } finally {
      UserRepository.unregisterUser(player);
    }
  }

  @Test
  void tracesWithWeaponReachWithoutChangingInteractionRange() {
    Player player = playerIn(GameMode.SURVIVAL);
    MockFullBlockStaticPlane blocks = MockFullBlockStaticPlane.createWithHorizontalPlaneAt(-10);
    User user = UserFactory.createTestUserFor(player, (ignored, key) -> switch (key) {
      case "blockCache" -> blocks;
      case "protocolVersion" -> ProtocolMetadata.VER_1_21_11;
      default -> null;
    });
    UserRepository.manuallyRegisterUser(player, user);
    try {
      BoundingBox target = new BoundingBox(-0.3D, 0.0D, 9.5D, 0.3D, 2.0D, 10.1D);
      assertEquals(3.0F, Raytracing.reachDistanceOf(user));

      Raytrace result = Raytracing.entityRaytrace(
        player, target, 0.0D,
        0.0D, 0.0D, 0.0D,
        0.0F, 0.0F, 0.0D,
        IGNORE_BLOCKS, 12.0D
      );

      assertFalse(result.missed());
      assertEquals(9.5D, result.reach(), 1.0E-6D);
      assertEquals(3.0F, Raytracing.reachDistanceOf(user));
    } finally {
      UserRepository.unregisterUser(player);
    }
  }

  private static void addReachModifier(AbilityMetadata abilities, double amount) {
    Attribute attribute = abilities.findAttribute("player.entity_interaction_range");
    AttributeModifier modifier = AttributeModifier.newBuilder(UUID.randomUUID())
      .withName("test-reach")
      .withOperation(ADD_NUMBER)
      .withAmount(amount)
      .build();
    abilities.modifiersOf(attribute).add(modifier);
  }

  private static ProtocolMetadata protocol(int protocolVersion) {
    return new ProtocolMetadata(null, protocolVersion);
  }

  private static Player playerIn(GameMode gameMode) {
    return FakePlayerFactory.createPlayer((methodName, ignored) -> switch (methodName) {
      case "getGameMode" -> gameMode;
      case "getLocation" -> new Location(null, 0.0D, 0.0D, 0.0D);
      case "getWorld" -> null;
      default -> null;
    });
  }
}
