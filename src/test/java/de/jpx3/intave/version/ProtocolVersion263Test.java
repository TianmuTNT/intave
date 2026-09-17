package de.jpx3.intave.version;

import de.jpx3.intave.adapter.MinecraftVersion;
import de.jpx3.intave.adapter.MinecraftVersions;
import de.jpx3.intave.user.meta.ProtocolMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;

import static org.junit.jupiter.api.Assertions.*;

class ProtocolVersion263Test {
  @BeforeAll
  static void setupRuntime() {
    MinecraftVersion.setCurrent(MinecraftVersions.VER1_21_4);
  }

  @Test
  void resolvesReleaseVersion() {
    MinecraftVersion version = MinecraftVersion.fromServerVersion("26.3-5-ace932e (MC: 26.3)");
    assertEquals(MinecraftVersions.VER26_3, version);
    assertTrue(version.isAtLeast(MinecraftVersions.VER26_2));
    assertEquals(777, ServerProtocolVersion.of(version));
  }

  @Test
  void swingChecksRequireBothClientAndServerToExposeTheOldPacket() {
    MinecraftVersion previous = MinecraftVersion.current();
    try {
      MinecraftVersion.setCurrent(MinecraftVersions.VER26_2);
      ProtocolMetadata protocol = new ProtocolMetadata(null, 776);
      assertTrue(protocol.sendsAttackSwingPackets());
      protocol.setProtocolVersion(777);
      assertFalse(protocol.sendsAttackSwingPackets());
      MinecraftVersion.setCurrent(MinecraftVersions.VER26_3);
      assertFalse(protocol.sendsAttackSwingPackets());
      protocol.setProtocolVersion(776);
      assertFalse(protocol.sendsAttackSwingPackets());
    } finally {
      MinecraftVersion.setCurrent(previous);
    }
  }
}
