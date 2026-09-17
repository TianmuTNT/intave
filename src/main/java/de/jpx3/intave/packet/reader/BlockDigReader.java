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

import com.comphenix.protocol.wrappers.EnumWrappers;
import de.jpx3.intave.annotate.Nullable;

public final class BlockDigReader extends BlockPositionReader {
  public @Nullable EnumWrappers.PlayerDigType action() {
    try {
      return packet().getPlayerDigTypes().readSafely(0);
    } catch (IllegalArgumentException exception) {
      // New actions such as 26.3's CHANGE_DESTROY_DIRECTION may precede ProtocolLib's enum.
      return null;
    }
  }
}
