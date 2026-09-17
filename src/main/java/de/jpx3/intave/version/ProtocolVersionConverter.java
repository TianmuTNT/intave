package de.jpx3.intave.version;

import de.jpx3.intave.adapter.MinecraftVersion;
import de.jpx3.intave.resource.Resource;
import de.jpx3.intave.resource.Resources;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public final class ProtocolVersionConverter {
  private static final class Ranges {
    private static final ProtocolVersionRanges RANGES = loadRanges(() ->
      Resources.localServiceCacheResource("protocolversions", "protocolversions", TimeUnit.DAYS.toMillis(14)));
  }

  static ProtocolVersionRanges loadRanges(Supplier<Resource> primary) {
    ProtocolVersionRanges bundled = Resources.resourceFromJarOrBuild("protocolversions")
      .collectLines(ProtocolVersionRangesCompiler.resourceCollector());
    try {
      // Finish the read before parsing so an interrupted download cannot supply partial mappings.
      Resource downloaded = Resources.memoryResource();
      downloaded.write(primary.get().readAll());
      ProtocolVersionRanges ranges = downloaded.collectLines(ProtocolVersionRangesCompiler.resourceCollector());
      // An older cache must not hide releases already included in this build.
      int bundledLatest = bundled.newest().map(ProtocolVersionRange::to).orElse(Integer.MIN_VALUE);
      if (ranges.newest().isPresent() && ranges.newest().get().to() >= bundledLatest) {
        return ranges.merge(bundled);
      }
    } catch (IOException | RuntimeException ignored) {
      // The service and its cache may be unavailable on an offline installation.
    }
    return bundled;
  }

  /** Returns -1 for unlisted or development versions instead of selecting a nearby packet layout. */
  public static int exactProtocolVersionBy(MinecraftVersion version) {
    return Ranges.RANGES.exactProtocolVersion(version);
  }

  public static String versionByProtocolVersion(int version) {
    return Ranges.RANGES.byProtocolVersion(version);
  }

  public static int protocolVersionBy(MinecraftVersion version) {
    return protocolVersionBy(version.getVersion());
  }

  public static int protocolVersionBy(String version) {
    return Ranges.RANGES.nearestProtocolVersion(version);
  }
}
