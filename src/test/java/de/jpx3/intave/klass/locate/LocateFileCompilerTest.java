package de.jpx3.intave.klass.locate;

import de.jpx3.intave.adapter.MinecraftVersion;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class LocateFileCompilerTest {
  @Test
  void bundledResourcePreservesWorkerThreadChunkMapping() throws Exception {
    try (InputStream resource = getClass().getResourceAsStream("/locate")) {
      assertNotNull(resource);
      Locations locations;
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource, StandardCharsets.UTF_8))) {
        locations = reader.lines().collect(LocateFileCompiler.resourceCollector());
      }
      List<MethodLocation> mappings = locations.methodLocations()
        .filterByClassKey("ChunkProviderServer")
        .filter(location -> location.key().equals("c(II)RLightChunk;"))
        .stream().toList();
      assertEquals(1, mappings.size(), "The bundled resource must retain the chunk lookup mapping");
      MethodLocation mapping = mappings.get(0);
      assertEquals("getChunkForLighting(II)RLightChunk;", mapping.target());
      for (String version : List.of("26.1.1", "26.2", "26.3")) {
        assertTrue(mapping.matchesVersion(new MinecraftVersion(version)), version);
      }
    }
  }
}
