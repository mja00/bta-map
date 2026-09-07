package dev.mja00.btamap;

import dev.mja00.btamap.gui.ScreenWaypointEdit;
import dev.mja00.btamap.gui.ScreenWorldMap;
import dev.mja00.btamap.map.MapManager;
import dev.mja00.btamap.waypoint.Waypoint;
import dev.mja00.btamap.waypoint.WaypointStore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.InputDevice;

/** In-game key handling; runs inside the vanilla input loop so press events resolve against the current event. */
public final class BtaMapInput {
	private BtaMapInput() {
	}

	public static boolean handle(Minecraft mc, InputDevice device) {
		if (mc.thePlayer == null || mc.currentWorld == null) {
			return false;
		}
		if (BtaMapOptions.KEY_OPEN_MAP.isPressEvent(device)) {
			mc.displayScreen(new ScreenWorldMap(null));
			return true;
		}
		if (BtaMapOptions.KEY_NEW_WAYPOINT.isPressEvent(device)) {
			WaypointStore store = MapManager.INSTANCE.waypoints();
			if (store != null) {
				Waypoint waypoint = new Waypoint("", (int) Math.floor(mc.thePlayer.x), (int) Math.floor(mc.thePlayer.y), (int) Math.floor(mc.thePlayer.z),
					MapManager.INSTANCE.dimension(), store.nextColor());
				mc.displayScreen(new ScreenWaypointEdit(null, store, waypoint, true));
			}
			return true;
		}
		if (BtaMapOptions.KEY_TOGGLE_MINIMAP.isPressEvent(device)) {
			BtaMapOptions.MINIMAP_ENABLED.toggle();
			return true;
		}
		if (BtaMapOptions.KEY_ZOOM_IN.isPressEvent(device)) {
			BtaMapOptions.zoomMinimap(1);
			return true;
		}
		if (BtaMapOptions.KEY_ZOOM_OUT.isPressEvent(device)) {
			BtaMapOptions.zoomMinimap(-1);
			return true;
		}
		return false;
	}
}
