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

import de.jpx3.intave.adapter.MinecraftVersions;

public final class AnimationReader extends EntityReader {
	public Animation animation() {
		return decodeAnimation(packet().getIntegers().read(1), MinecraftVersions.VER26_3.atOrAbove());
	}

	static Animation decodeAnimation(int id, boolean modern) {
		// 26.3 moved swings to ClientboundSwingAnimationPacket and renumbered the remaining actions.
		if (modern) {
			switch (id) {
				case 0: return Animation.WAKEUP;
				case 1: return Animation.CRIT;
				case 2: return Animation.CRIT_MAGIC;
				default: return null;
			}
		}
		return id >= 0 && id < Animation.values().length ? Animation.values()[id] : null;
	}

	public enum Animation {
		SWING,
		HURT,
		WAKEUP,
		SWING_OFFHAND,
		CRIT,
		CRIT_MAGIC
	};
}
