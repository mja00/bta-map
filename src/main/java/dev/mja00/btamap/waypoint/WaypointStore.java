package dev.mja00.btamap.waypoint;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import dev.mja00.btamap.BtaMap;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/** All waypoints of one world (every dimension), persisted as JSON next to the map regions. */
public final class WaypointStore {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private final File file;
	private final List<Waypoint> waypoints = new ArrayList<>();
	private boolean dirty;

	public WaypointStore(File file) {
		this.file = file;
		load();
	}

	public List<Waypoint> all() {
		return waypoints;
	}

	public List<Waypoint> inDimension(int dimension) {
		List<Waypoint> result = new ArrayList<>();
		for (Waypoint waypoint : waypoints) {
			if (waypoint.dimension == dimension) {
				result.add(waypoint);
			}
		}
		return result;
	}

	public void add(Waypoint waypoint) {
		waypoints.add(waypoint);
		dirty = true;
	}

	public void remove(Waypoint waypoint) {
		if (waypoints.remove(waypoint)) {
			dirty = true;
		}
	}

	public void markDirty() {
		dirty = true;
	}

	public int nextColor() {
		return Waypoint.PALETTE[waypoints.size() % Waypoint.PALETTE.length];
	}

	private void load() {
		if (!file.isFile()) {
			return;
		}
		try {
			String json = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
			List<Waypoint> loaded = GSON.fromJson(json, new TypeToken<List<Waypoint>>() {}.getType());
			if (loaded != null) {
				for (Waypoint waypoint : loaded) {
					if (waypoint != null) {
						if (waypoint.name == null) {
							waypoint.name = "";
						}
						waypoints.add(waypoint);
					}
				}
			}
		} catch (IOException | JsonParseException e) {
			BtaMap.LOGGER.warn("Failed to read waypoints {}", file, e);
		}
	}

	public void save() {
		if (!dirty) {
			return;
		}
		dirty = false;
		File parent = file.getParentFile();
		if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
			BtaMap.LOGGER.warn("Could not create waypoint directory {}", parent);
			return;
		}
		try {
			Files.write(file.toPath(), GSON.toJson(waypoints).getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			BtaMap.LOGGER.warn("Failed to write waypoints {}", file, e);
		}
	}
}
