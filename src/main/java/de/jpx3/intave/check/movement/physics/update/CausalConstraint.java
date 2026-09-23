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

package de.jpx3.intave.check.movement.physics.update;

import de.jpx3.intave.check.movement.physics.environment.SimulationEnvironment;
import de.jpx3.intave.user.meta.MovementMetadata;

public final class CausalConstraint {
	private final long notBeforeMove;
	private final long notAfterMove;
	private final long sequenceNum;

	public CausalConstraint(long notBeforeMove, long notAfterMove, long sequenceNum) {
		this.notBeforeMove = notBeforeMove;
		this.notAfterMove = notAfterMove;
		this.sequenceNum = sequenceNum;
	}

	public long notAfter() {
		return notAfterMove;
	}

	public long notBefore() {
		return notBeforeMove;
	}

	public long sequenceNumber() {
		return sequenceNum;
	}

	public boolean mustHappenThisTick(SimulationEnvironment environment) {
		return environment.currentTick() == notAfterMove;
	}

	public CausalConstraint notAfter(
		long newNotAfter
	) {
		return new CausalConstraint(notBeforeMove, newNotAfter, sequenceNum);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof CausalConstraint) {
			CausalConstraint other = (CausalConstraint) obj;
			return this.notBeforeMove == other.notBeforeMove && this.notAfterMove == other.notAfterMove && this.sequenceNum == other.sequenceNum;
		}
		return false;
	}

	public static CausalConstraint onlyThisTick(MovementMetadata metadata) {
		return new CausalConstraint(metadata.currentTick(), metadata.currentTick(), metadata.newSequenceNumber());
	}

	@Override
	public int hashCode() {
		int result = 17;
		result = 31 * result + Long.hashCode(notBeforeMove);
		result = 31 * result + Long.hashCode(notAfterMove);
		result = 31 * result + Long.hashCode(sequenceNum);
		return result;
	}

	public boolean isOpenBounded() {
		return notAfterMove == Long.MAX_VALUE;
	}

	@Override
	public String toString() {
		String interval = isOpenBounded()
			? "[" + notBeforeMove + ", ∞)"
			: "[" + notBeforeMove + ", " + notAfterMove + "]";
		return "T?U" + interval + "@" + sequenceNum;
	}

	public boolean possible(
		SimulationEnvironment environment
	) {
		return environment.currentTick() >= notBeforeMove &&
			environment.currentTick() <= notAfterMove && environment.activeSequence() < sequenceNum;
	}

	public boolean expired(SimulationEnvironment environment) {
		return environment.currentTick() + 20 > notAfterMove;
	}

	public static CausalConstraint openEnded(
		long notBefore,
		long sequence
	) {
		return new CausalConstraint(notBefore, Long.MAX_VALUE, sequence);
	}
}
