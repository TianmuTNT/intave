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

package de.jpx3.intave.version;

import de.jpx3.intave.adapter.MinecraftVersion;

import static de.jpx3.intave.adapter.MinecraftVersions.*;

public final class ServerProtocolVersion {
	private ServerProtocolVersion() {}

	public static int current() {
		return of(MinecraftVersion.current());
	}

	public static int of(MinecraftVersion version) {
		if (!version.isAtLeast(VER1_8_0)) {
			return -1;
		}
		return ProtocolVersionConverter.exactProtocolVersionBy(version);
	}
}
