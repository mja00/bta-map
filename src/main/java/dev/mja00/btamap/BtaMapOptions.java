package dev.mja00.btamap;

import net.minecraft.client.input.InputDevice;
import net.minecraft.client.option.GameSettings;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.OptionBoolean;
import net.minecraft.client.option.OptionEnum;
import net.minecraft.client.option.OptionRange;
import org.lwjgl.input.Keyboard;

public final class BtaMapOptions {
	public enum Shape {
		SQUARE,
		ROUND
	}

	/** Pixels per block for each zoom step of the minimap. */
	public static final float[] ZOOM_LEVELS = {0.5F, 1.0F, 2.0F, 4.0F};

	public static final OptionBoolean MINIMAP_ENABLED = new OptionBoolean("btamap.minimapEnabled", true);
	public static final OptionEnum<Shape> MINIMAP_SHAPE = new OptionEnum<>("btamap.minimapShape", Shape.class, Shape.SQUARE);
	// OptionRange compares boxed Integers by identity, so every value must stay inside the Integer cache (<= 127).
	public static final OptionRange MINIMAP_SIZE = new OptionRange("btamap.minimapSize", 80, 48, 120).withStep(8);
	public static final OptionRange MINIMAP_ZOOM = new OptionRange("btamap.minimapZoom", 1, 0, ZOOM_LEVELS.length - 1);
	public static final OptionBoolean MINIMAP_ROTATE = new OptionBoolean("btamap.minimapRotate", false);
	public static final OptionBoolean SHOW_COORDS = new OptionBoolean("btamap.showCoords", true);
	public static final OptionBoolean SHOW_WAYPOINTS = new OptionBoolean("btamap.showWaypoints", true);
	public static final OptionBoolean WAYPOINT_BEAMS = new OptionBoolean("btamap.waypointBeams", true);

	public static final KeyBinding KEY_OPEN_MAP = new KeyBinding("key.btamap.openMap").setDefault(InputDevice.keyboard, Keyboard.KEY_M);
	public static final KeyBinding KEY_NEW_WAYPOINT = new KeyBinding("key.btamap.newWaypoint").setDefault(InputDevice.keyboard, Keyboard.KEY_B);
	public static final KeyBinding KEY_TOGGLE_MINIMAP = new KeyBinding("key.btamap.toggleMinimap");
	public static final KeyBinding KEY_ZOOM_IN = new KeyBinding("key.btamap.zoomIn").setDefault(InputDevice.keyboard, Keyboard.KEY_EQUALS);
	public static final KeyBinding KEY_ZOOM_OUT = new KeyBinding("key.btamap.zoomOut").setDefault(InputDevice.keyboard, Keyboard.KEY_MINUS);

	private BtaMapOptions() {
	}

	public static void register() {
		GameSettings.register(MINIMAP_ENABLED);
		GameSettings.register(MINIMAP_SHAPE);
		GameSettings.register(MINIMAP_SIZE);
		GameSettings.register(MINIMAP_ZOOM);
		GameSettings.register(MINIMAP_ROTATE);
		GameSettings.register(SHOW_COORDS);
		GameSettings.register(SHOW_WAYPOINTS);
		GameSettings.register(WAYPOINT_BEAMS);
		GameSettings.register(KEY_OPEN_MAP);
		GameSettings.register(KEY_NEW_WAYPOINT);
		GameSettings.register(KEY_TOGGLE_MINIMAP);
		GameSettings.register(KEY_ZOOM_IN);
		GameSettings.register(KEY_ZOOM_OUT);
	}

	public static float minimapPixelsPerBlock() {
		int index = Math.max(0, Math.min(ZOOM_LEVELS.length - 1, MINIMAP_ZOOM.value));
		return ZOOM_LEVELS[index];
	}

	public static void zoomMinimap(int delta) {
		MINIMAP_ZOOM.value = Math.max(0, Math.min(ZOOM_LEVELS.length - 1, MINIMAP_ZOOM.value + delta));
	}
}
