package de.jpx3.intave.block.fluid;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FluidFlowBlockingTest {
  @Test
  void matches263FluidFlowTagForSolidBlocksSignsAndTransparentBlocks() {
    for (String name : new String[]{"STONE", "GLASS", "OAK_LEAVES", "OAK_SIGN", "OAK_WALL_SIGN", "POPLAR_HANGING_SIGN", "STRAW_BED"}) {
      assertTrue(FluidFlowBlocking.contains(Material.valueOf(name)), name);
    }
    for (String name : new String[]{"AIR", "WATER", "LAVA", "COBWEB", "REDSTONE_WIRE"}) {
      assertFalse(FluidFlowBlocking.contains(Material.valueOf(name)), name);
    }
  }
}
