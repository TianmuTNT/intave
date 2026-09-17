package de.jpx3.intave.version;

import de.jpx3.intave.resource.ClasspathResource;
import de.jpx3.intave.adapter.MinecraftVersion;
import de.jpx3.intave.resource.MemoryResource;
import de.jpx3.intave.resource.Resource;
import de.jpx3.intave.resource.Resources;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProtocolVersionFallbackTest {
  @Test
  void usesBundledMappingsWhenTheServiceAndCacheAreEmpty() {
    assertBundledMappings(ProtocolVersionConverter.loadRanges(Resources::memoryResource));
  }

  @Test
  void usesBundledMappingsWhenResourceCreationFailsOffline() {
    assertBundledMappings(ProtocolVersionConverter.loadRanges(() -> {
      throw new UncheckedIOException(new IOException("Offline"));
    }));
  }

  @Test
  void usesBundledMappingsWhenTheDownloadFailsDuringReading() {
    AtomicBoolean closed = new AtomicBoolean();
    Resource interrupted = new ClasspathResource("protocolversions") {
      @Override
      public InputStream read() {
        return new InputStream() {
          private final InputStream prefix = new ByteArrayInputStream("up to 778 is 26.4\n".getBytes(UTF_8));

          @Override
          public int read() throws IOException {
            int value = prefix.read();
            if (value != -1) return value;
            throw new IOException("Connection lost");
          }

          @Override
          public void close() {
            closed.set(true);
          }
        };
      }
    };
    assertBundledMappings(ProtocolVersionConverter.loadRanges(() -> interrupted));
    assertTrue(closed.get());
  }

  @Test
  void usesBundledMappingsWhenTheLocalCachePredatesThisBuild() {
    MemoryResource cached = new MemoryResource();
    cached.write("up to 776 is 26.2\n");
    assertBundledMappings(ProtocolVersionConverter.loadRanges(() -> cached));
  }

  @Test
  void prefersMappingsFromTheServiceOrCache() {
    MemoryResource downloaded = new MemoryResource();
    downloaded.write("up to 777 is 26.3\nup to 778 is 26.4\n");
    ProtocolVersionRanges ranges = ProtocolVersionConverter.loadRanges(() -> downloaded);
    assertEquals("26.4", ranges.byProtocolVersion(778));
    assertEquals(778, ranges.nearestProtocolVersion("26.4"));
  }

  @Test
  void sharedResourceProvidesExactNativeReleaseAliasesOffline() {
    ProtocolVersionRanges ranges = ProtocolVersionConverter.loadRanges(Resources::memoryResource);
    assertEquals(47, exact(ranges, "1.8"));
    assertEquals(47, exact(ranges, "1.8.8"));
    assertEquals(110, exact(ranges, "1.9.4"));
    assertEquals(769, exact(ranges, "1.21.4"));
    assertEquals(770, exact(ranges, "1.21.5"));
    assertEquals(775, exact(ranges, "26.1.2"));
    assertEquals(777, exact(ranges, "26.3.0"));
  }

  @Test
  void newerRemoteRangesRetainPreciseBundledReleases() {
    MemoryResource downloaded = new MemoryResource();
    // Future release/protocol values here are test fixtures, not shipping version declarations.
    downloaded.write("up to 46 is 1.8\nup to 777 is 26.3\nup to 1234 is 26.4\n");
    ProtocolVersionRanges ranges = ProtocolVersionConverter.loadRanges(() -> downloaded);
    assertEquals(1234, exact(ranges, "26.4"));
    assertEquals(47, exact(ranges, "1.8"));
    assertEquals(770, exact(ranges, "1.21.5"));
    assertEquals(775, exact(ranges, "26.1.2"));
  }

  @Test
  void remoteExplicitReleasesOverrideBundledDeclarations() {
    MemoryResource downloaded = new MemoryResource();
    downloaded.write("up to 777 is 26.3\nrelease 1234 is 26.3\n");
    assertEquals(1234, exact(ProtocolVersionConverter.loadRanges(() -> downloaded), "26.3"));
  }

  @Test
  void unlistedAndDevelopmentReleasesDoNotSelectNearestProtocol() {
    ProtocolVersionRanges ranges = ProtocolVersionConverter.loadRanges(Resources::memoryResource);
    for (String version : new String[]{"1.8.10", "1.22", "26.4", "26.3.1", "26.3-rc-3"}) {
      assertEquals(-1, exact(ranges, version), version);
    }
  }

  @Test
  void malformedOrConflictingReleaseDeclarationsFallBackToBundledData() {
    for (String declaration : new String[]{"release 0 is 26.3", "release 1234 is 26.3,",
      "release 1234 is 26.3.0.1", "release 1234 is 26.3-rc-3",
      "release 1234 is 26.3\nrelease 1235 is 26.3"}) {
      MemoryResource downloaded = new MemoryResource();
      downloaded.write("up to 777 is 26.3\n" + declaration + "\n");
      assertBundledMappings(ProtocolVersionConverter.loadRanges(() -> downloaded));
    }
  }

  private static int exact(ProtocolVersionRanges ranges, String version) {
    return ranges.exactProtocolVersion(new MinecraftVersion(version));
  }

  private static void assertBundledMappings(ProtocolVersionRanges ranges) {
    assertEquals(770, exact(ranges, "1.21.5"));
    assertEquals(777, exact(ranges, "26.3"));
    assertEquals("26.3", ranges.byProtocolVersion(777));
    assertEquals(777, ranges.nearestProtocolVersion("26.3.0"));
    assertEquals("26.2", ranges.byProtocolVersion(776));
    assertEquals(776, ranges.nearestProtocolVersion("26.2"));
    assertEquals("1.8.9", ranges.byProtocolVersion(47));
    assertEquals(47, ranges.nearestProtocolVersion("1.8.9"));
  }
}
