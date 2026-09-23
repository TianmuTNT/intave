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

package de.jpx3.intave.share;

public final class MovementCorrection {
  private final PositionMoveRotation change;
  private final boolean onGround;

  public MovementCorrection(PositionMoveRotation change, boolean onGround) {
    this.change = change;
    this.onGround = onGround;
  }

  public PositionMoveRotation change() {
    return change;
  }

  public boolean onGround() {
    return onGround;
  }

	@Override
	public String toString() {
		return "MovementCorrection{" +
			"change=" + change +
			", onGround=" + onGround +
			'}';
	}

  @Override
  public int hashCode() {
    int result = change != null ? change.hashCode() : 0;
		result = 31 * result + (onGround ? 1 : 0);
		return result;
  }

  @Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null || getClass() != obj.getClass()) return false;

		MovementCorrection that = (MovementCorrection) obj;

		if (onGround != that.onGround) return false;
		return change != null ? change.equals(that.change) : that.change == null;
	}
}
