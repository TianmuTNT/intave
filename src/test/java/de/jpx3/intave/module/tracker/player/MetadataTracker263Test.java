package de.jpx3.intave.module.tracker.player;

import de.jpx3.intave.share.BlockPosition;
import de.jpx3.intave.share.Position;
import de.jpx3.intave.user.meta.ProtocolMetadata;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MetadataTracker263Test {
  @Test
  void strawBedsUseTheFootHeightWhileNormalAndTranslatedBedsKeepTheirOffset() {
    BlockPosition bed = new BlockPosition(10, 64, -5);
    Position straw = MetadataTracker.positionFromBedPosition(bed, Material.valueOf("STRAW_BED"), ProtocolMetadata.VER_26_3);
    assertEquals(10.5D, straw.getX());
    assertEquals(64.375D, straw.getY());
    assertEquals(-4.5D, straw.getZ());
    assertEquals(64.6875D, MetadataTracker.positionFromBedPosition(bed, Material.valueOf("RED_BED"), ProtocolMetadata.VER_26_3).getY());
    assertEquals(64.6875D, MetadataTracker.positionFromBedPosition(bed, Material.valueOf("STRAW_BED"), ProtocolMetadata.VER_26_2).getY());
  }
}
