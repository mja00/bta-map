package dev.mja00.btamap.waypoint;

public final class Waypoint {
	public static final int[] PALETTE = {
		0xFFE53935, 0xFFFB8C00, 0xFFFDD835, 0xFF43A047, 0xFF00ACC1, 0xFF1E88E5, 0xFF8E24AA, 0xFFF06292, 0xFFFFFFFF
	};

	public String name;
	public int x;
	public int y;
	public int z;
	public int dimension;
	public int color;
	public boolean visible = true;

	public Waypoint(String name, int x, int y, int z, int dimension, int color) {
		this.name = name;
		this.x = x;
		this.y = y;
		this.z = z;
		this.dimension = dimension;
		this.color = color;
	}

	public String initial() {
		return name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase();
	}

	public double distanceTo(double px, double pz) {
		double dx = x + 0.5 - px;
		double dz = z + 0.5 - pz;
		return Math.sqrt(dx * dx + dz * dz);
	}
}
