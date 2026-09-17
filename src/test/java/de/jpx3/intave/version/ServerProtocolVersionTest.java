package de.jpx3.intave.version;

import de.jpx3.intave.adapter.MinecraftVersion;
import de.jpx3.intave.adapter.MinecraftVersions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ServerProtocolVersionTest {
    @Test
    void liveServerLookupUsesSharedVersionData() {
        try {
            MinecraftVersion.setCurrent(MinecraftVersion.fromServerVersion("git-Paper-445 (MC: 1.8.8)"));
            assertEquals(47, ServerProtocolVersion.current());
        } finally {
            MinecraftVersion.setCurrent(MinecraftVersions.VER1_21_4);
        }
    }

    @Test
    void mapsEveryLocalClientSourceReleaseToItsExactProtocol() {
        String[] versions = {"1.8.9", "1.9.4", "1.10.2", "1.11.2", "1.12.2", "1.13.2", "1.14.4", "1.15.2",
            "1.16.5", "1.17.1", "1.18.2", "1.19.4", "1.20", "1.20.1", "1.20.2", "1.20.4", "1.20.5",
            "1.20.6", "1.21.1", "1.21.2", "1.21.3", "1.21.4", "1.21.5", "1.21.6", "1.21.7", "1.21.8",
            "1.21.9", "1.21.10", "1.21.11", "26.1.2", "26.2", "26.3"};
        int[] protocols = {47, 110, 210, 316, 340, 404, 498, 578, 754, 756, 758, 762, 763, 763, 764, 765, 766,
            766, 767, 768, 768, 769, 770, 771, 772, 772, 773, 773, 774, 775, 776, 777};
        for (int i = 0; i < versions.length; i++) {
            assertEquals(protocols[i], ServerProtocolVersion.of(new MinecraftVersion(versions[i])), versions[i]);
        }
        assertEquals(108, ServerProtocolVersion.of(new MinecraftVersion("1.9.1")));
        assertEquals(315, ServerProtocolVersion.of(new MinecraftVersion("1.11")));
    }

    @Test
    void unknownAndDevelopmentVersionsCannotSelectAnUnrelatedPacketLayout() {
        for (String version : new String[] {"1.7.10", "1.4.2", "1.8.10", "1.22", "26.3-rc-3", "1.21.2-pre1"}) {
            assertEquals(-1, ServerProtocolVersion.of(new MinecraftVersion(version)), version);
        }
    }
}
