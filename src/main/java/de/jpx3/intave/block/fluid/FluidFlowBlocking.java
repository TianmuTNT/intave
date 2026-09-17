package de.jpx3.intave.block.fluid;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import de.jpx3.intave.resource.Resources;
import org.bukkit.Material;

import java.util.EnumSet;
import java.util.Set;

final class FluidFlowBlocking {
  private static final Set<Material> BLOCKS = load();

  private static Set<Material> load() {
    Set<Material> blocks = EnumSet.noneOf(Material.class);
    String json = Resources.resourceFromJarOrBuild("registry/blocks_fluid_flow/26.3.json").readAsString();
    for (JsonElement entry : new JsonParser().parse(json).getAsJsonArray()) {
      Material material = Material.getMaterial(entry.getAsString().toUpperCase(java.util.Locale.ROOT));
      if (material != null) blocks.add(material);
    }
    return blocks;
  }

  static boolean contains(Material material) {
    return BLOCKS.contains(material);
  }
}
