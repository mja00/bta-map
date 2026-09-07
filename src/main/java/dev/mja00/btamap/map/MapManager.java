package dev.mja00.btamap.map;

import dev.mja00.btamap.waypoint.WaypointStore;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.io.File;
import net.minecraft.client.Minecraft;
import net.minecraft.client.option.GameSettings;
import net.minecraft.client.world.WorldClientMP;
import net.minecraft.core.world.World;
import net.minecraft.core.world.chunk.Chunk;
import net.minecraft.core.world.pos.ChunkPos;
import net.minecraft.core.world.save.LevelStorage;
import net.minecraft.core.world.save.LevelStorageBase;
import dev.mja00.btamap.mixin.LevelStorageBaseAccessor;

/** Owns the map data for whatever world the client is currently in and keeps it up to date from loaded chunks. */
public final class MapManager {
	public static final MapManager INSTANCE = new MapManager();

	private static final int CHUNKS_PER_TICK = 6;
	private static final int NEAR_RADIUS = 2;
	private static final int NEAR_RESCAN_TICKS = 20;
	private static final int FAR_RESCAN_TICKS = 200;
	private static final int AUTOSAVE_TICKS = 600;

	private final ChunkScanner scanner = new ChunkScanner();
	private final Long2IntOpenHashMap lastScan = new Long2IntOpenHashMap();
	private final ChunkPos chunkPos = new ChunkPos();

	private String serverAddress = "unknown";
	private World world;
	private String worldKey;
	private MapWorld map;
	private WaypointStore waypoints;
	private int ticks;

	private MapManager() {
		// Sentinel far in the past but safe from subtraction overflow, unlike Integer.MIN_VALUE.
		lastScan.defaultReturnValue(Integer.MIN_VALUE / 2);
	}

	public void setServerAddress(String address) {
		this.serverAddress = address;
	}

	public MapWorld map() {
		return map;
	}

	public WaypointStore waypoints() {
		return waypoints;
	}

	public int dimension() {
		return world == null ? 0 : world.dimension.id;
	}

	public void tick(Minecraft mc) {
		World current = mc.currentWorld;
		if (current == null) {
			unload();
			return;
		}
		if (current != world) {
			switchWorld(mc, current);
		}
		ticks++;
		if (mc.thePlayer != null && !mc.isGamePaused) {
			scanAround(mc);
		}
		if (ticks % AUTOSAVE_TICKS == 0) {
			save();
		}
	}

	public void save() {
		if (map != null) {
			map.saveAll();
		}
		if (waypoints != null) {
			waypoints.save();
		}
	}

	/** Flushes and drops the current world; safe to call repeatedly. */
	public void unload() {
		if (map != null) {
			map.dispose();
			map = null;
		}
		if (waypoints != null) {
			waypoints.save();
			waypoints = null;
		}
		world = null;
		worldKey = null;
		lastScan.clear();
	}

	private void switchWorld(Minecraft mc, World current) {
		String key = worldKey(current);
		// Dimension hops keep the same waypoint file; only the map tiles change directory.
		if (waypoints != null && !key.equals(worldKey)) {
			waypoints.save();
			waypoints = null;
		}
		if (map != null) {
			map.dispose();
		}
		File worldDir = new File(new File(mc.getMinecraftDir(), "btamap"), key);
		map = new MapWorld(new File(worldDir, "dim" + current.dimension.id));
		if (waypoints == null) {
			waypoints = new WaypointStore(new File(worldDir, "waypoints.json"));
		}
		world = current;
		worldKey = key;
		lastScan.clear();
	}

	private String worldKey(World current) {
		String raw;
		if (current instanceof WorldClientMP) {
			raw = "mp_" + serverAddress;
		} else {
			LevelStorage storage = current.getLevelStorage();
			raw = storage instanceof LevelStorageBase ? "sp_" + ((LevelStorageBaseAccessor) storage).btamap$getWorldDirName() : "sp_unknown";
		}
		return raw.replaceAll("[^A-Za-z0-9._-]", "_");
	}

	private void scanAround(Minecraft mc) {
		int pcx = (int) Math.floor(mc.thePlayer.x) >> 4;
		int pcz = (int) Math.floor(mc.thePlayer.z) >> 4;
		int radius = Math.min(16, Math.max(4, GameSettings.RENDER_DISTANCE.value));
		int budget = CHUNKS_PER_TICK;
		// Walk outward ring by ring so the chunks the player can actually see refresh first.
		for (int ring = 0; ring <= radius && budget > 0; ring++) {
			int interval = ring <= NEAR_RADIUS ? NEAR_RESCAN_TICKS : FAR_RESCAN_TICKS;
			for (int dz = -ring; dz <= ring && budget > 0; dz++) {
				boolean edgeRow = Math.abs(dz) == ring;
				for (int dx = -ring; dx <= ring && budget > 0; dx += edgeRow ? 1 : ring * 2) {
					if (scanChunk(pcx + dx, pcz + dz, interval)) {
						budget--;
					}
					if (ring == 0) {
						break;
					}
				}
			}
		}
	}

	private boolean scanChunk(int cx, int cz, int interval) {
		long key = MapRegion.key(cx, cz);
		if (ticks - lastScan.get(key) < interval) {
			return false;
		}
		chunkPos.x = cx;
		chunkPos.z = cz;
		if (!world.isChunkLoaded(chunkPos)) {
			return false;
		}
		Chunk chunk = world.getChunk(chunkPos);
		if (chunk.isChunkEmpty()) {
			return false;
		}
		lastScan.put(key, ticks);
		scanner.scan(world, chunk, map);
		return true;
	}
}
