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

package de.jpx3.intave.packet.reader;

import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.BukkitConverters;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import de.jpx3.intave.adapter.MinecraftVersions;

import java.util.*;

public final class PlayerInfoReader extends AbstractPacketReader {
  private static final boolean MULTIPLE_ACTIONS = MinecraftVersions.VER1_19_3.atOrAbove();

  public Set<EnumWrappers.PlayerInfoAction> playerInfoActions() {
    try {
      if (MULTIPLE_ACTIONS) {
        Set<EnumWrappers.PlayerInfoAction> actions = packet().getPlayerInfoActions().readSafely(0);
        return actions != null ? actions : Collections.emptySet();
      }
      EnumWrappers.PlayerInfoAction action = packet().getPlayerInfoAction().readSafely(0);
      return action != null ? Collections.singleton(action) : Collections.emptySet();
    } catch (RuntimeException ignored) {
      return Collections.emptySet();
    }
  }

  // Null means the packet cannot be inspected safely; the caller must leave it unchanged.
  public List<PlayerInfoEntry> playerInfoEntries() {
    try {
      List<?> rawEntries = (List<?>) packet().getModifier().withType(List.class).readSafely(0);
      return inspectEntries(rawEntries, MULTIPLE_ACTIONS);
    } catch (RuntimeException ignored) {
      return null;
    }
  }

  static List<PlayerInfoEntry> inspectEntries(List<?> rawEntries, boolean multipleActions) {
    if (rawEntries == null) {
      return null;
    }
    List<PlayerInfoEntry> entries = new ArrayList<>(rawEntries.size());
    for (Object rawEntry : rawEntries) {
      if (rawEntry == null) {
        return null;
      }
      UUID profileId = profileId(rawEntry, multipleActions);
      if (profileId == null) {
        return null;
      }
      entries.add(new PlayerInfoEntry(rawEntry, profileId));
    }
    return entries;
  }

  public boolean writePlayerInfoEntries(List<PlayerInfoEntry> entries) {
    try {
      List<Object> rawEntries = new ArrayList<>(entries.size());
      for (PlayerInfoEntry entry : entries) {
        rawEntries.add(entry.rawEntry);
      }
      packet().getModifier().withType(List.class).write(0, rawEntries);
      return true;
    } catch (RuntimeException ignored) {
      return false;
    }
  }

  private static UUID profileId(Object rawEntry, boolean multipleActions) {
    StructureModifier<Object> modifier = new StructureModifier<>(rawEntry.getClass(), null, false)
      .withTarget(rawEntry);
    if (multipleActions) {
      return modifier.<UUID>withType(UUID.class).readSafely(0);
    }
    WrappedGameProfile profile = modifier.withType(
      MinecraftReflection.getGameProfileClass(), BukkitConverters.getWrappedGameProfileConverter()
    ).readSafely(0);
    return profile != null ? profile.getUUID() : null;
  }

  public static final class PlayerInfoEntry {
    private final Object rawEntry;
    private final UUID profileId;

    private PlayerInfoEntry(Object rawEntry, UUID profileId) {
      this.rawEntry = rawEntry;
      this.profileId = profileId;
    }

    public UUID profileId() {
      return profileId;
    }
  }
}
