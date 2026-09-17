package de.jpx3.intave.share;

import de.jpx3.intave.codec.StreamCodec;
import io.netty.buffer.ByteBuf;

import java.util.Objects;

public final class PositionAndRotation {
	private final double x, y, z;
	private final float yaw, pitch;

	public static final StreamCodec<ByteBuf, ByteBuf, PositionAndRotation> STREAM_CODEC = StreamCodec.compound(
		Position.STREAM_CODEC, PositionAndRotation::position,
		Rotation.STREAM_CODEC, PositionAndRotation::rotation,
		PositionAndRotation::new
	);

	public PositionAndRotation(double x, double y, double z, float yaw, float pitch) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.yaw = yaw;
		this.pitch = pitch;
	}

	public double x() {
		return x;
	}

	public double y() {
		return y;
	}

	public double z() {
		return z;
	}

	public float yaw() {
		return yaw;
	}

	public float pitch() {
		return pitch;
	}

	public PositionAndRotation(Position position, Rotation rotation) {
		this(position.getX(), position.getY(), position.getZ(), rotation.yaw(), rotation.pitch());
	}

	public Position position() {
		return new Position(x, y, z);
	}

	public Rotation rotation() {
		return new Rotation(yaw, pitch);
	}

	public PositionAndRotation withPosition(Position position) {
		return new PositionAndRotation(position.getX(), position.getY(), position.getZ(), yaw, pitch);
	}

	public PositionAndRotation withRotation(Rotation rotation) {
		return new PositionAndRotation(x, y, z, rotation.yaw(), rotation.pitch());
	}

	public boolean isFinite() {
		return Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z)
			&& Float.isFinite(yaw) && Float.isFinite(pitch);
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) return true;
		if (!(other instanceof PositionAndRotation)) return false;
		PositionAndRotation value = (PositionAndRotation) other;
		return Double.compare(x, value.x) == 0 && Double.compare(y, value.y) == 0
			&& Double.compare(z, value.z) == 0 && Float.compare(yaw, value.yaw) == 0
			&& Float.compare(pitch, value.pitch) == 0;
	}

	@Override
	public int hashCode() {
		return Objects.hash(x, y, z, yaw, pitch);
	}

	@Override
	public String toString() {
		return "PositionAndRotation{x=" + x + ", y=" + y + ", z=" + z
			+ ", yaw=" + yaw + ", pitch=" + pitch + '}';
	}
}
