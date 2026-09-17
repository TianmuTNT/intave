package de.jpx3.intave.version;

import de.jpx3.intave.resource.BulkLineCollector;
import de.jpx3.intave.adapter.MinecraftVersion;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collector;

final class ProtocolVersionRangesCompiler {
  public static ProtocolVersionRanges apply(List<String> lines) {
    int lastEnd = Integer.MIN_VALUE;
    List<ProtocolVersionRange> ranges = new ArrayList<>();
    Map<MinecraftVersion, Integer> releases = new HashMap<>();
    for (int i = 0; i < lines.size(); i++) {
      String line = lines.get(i).trim();
      if (line.isEmpty() || line.startsWith("#")) {
        continue;
      }
      if (line.startsWith("release ")) {
        String[] parts = line.substring("release ".length()).split(" is ", 2);
        if (parts.length != 2) throw new IllegalArgumentException("Invalid release mapping: " + line);
        int protocol = Integer.parseInt(parts[0]);
        if (protocol <= 0) throw new IllegalArgumentException("Invalid release protocol: " + line);
        for (String name : parts[1].split(",", -1)) {
          String release = name.trim();
          if (!release.matches("[0-9]+\\.[0-9]+(?:\\.[0-9]+)?")) {
            throw new IllegalArgumentException("Invalid release name: " + name);
          }
          MinecraftVersion version = new MinecraftVersion(release);
          if (version.isSnapshot() || version.getDevelopmentStage() != null) {
            throw new IllegalArgumentException("Expected a released version: " + line);
          }
          Integer previous = releases.put(version, protocol);
          if (previous != null && previous != protocol) {
            throw new IllegalArgumentException("Conflicting release mapping: " + name);
          }
        }
        continue;
      }
      if (line.startsWith("up to")) {
        // format is "up to <number> is <version>"
        String[] split = line.split(" ");
        if (split.length != 5) {
          System.out.println("Invalid line format: " + line + " at line " + i);
          Thread.dumpStack();
          continue;
        }
        int protocolVersion = Integer.parseInt(split[2]);
        String version = split[4];
        ranges.add(new ProtocolVersionRange(lastEnd + 1, protocolVersion, version));
        lastEnd = protocolVersion;
      } else {
        // format is "<number> is <version>"
        String[] split = line.split(" is ");
        if (split.length != 2) {
          System.out.println("Invalid line format: " + line + " at line " + i);
          Thread.dumpStack();
          continue;
        }
        int protocolVersion = Integer.parseInt(split[0]);
        String version = split[1];
        if (protocolVersion <= lastEnd) {
          System.out.println("Invalid line format: " + line + " at line " + i);
          Thread.dumpStack();
          continue;
        }
        ranges.add(new ProtocolVersionRange(protocolVersion, protocolVersion, version));
        lastEnd = protocolVersion;
      }
    }
    return new ProtocolVersionRanges(ranges, releases);
  }

  private static final Collector<String, ?, ProtocolVersionRanges> RESOURCE_COLLECTOR = BulkLineCollector.withFinisher(ProtocolVersionRangesCompiler::apply);

  public static Collector<String, ?, ProtocolVersionRanges> resourceCollector() {
    return RESOURCE_COLLECTOR;
  }
}
