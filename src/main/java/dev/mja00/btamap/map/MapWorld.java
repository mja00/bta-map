package dev.mja00.btamap.map;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.io.File;
import java.util.Collection;

/** Explored map data for one dimension of one world, split into lazily loaded regions. */
public final class MapWorld {
	private final File dir;
	private final Long2ObjectOpenHashMap<MapRegion> regions = new Long2ObjectOpenHashMap<>();

	public MapWorld(File dir) {
		this.dir = dir;
	}

	public File dir() {
		return dir;
	}

	public MapRegion region(int rx, int rz) {
		long key = MapRegion.key(rx, rz);
		MapRegion region = regions.get(key);
		if (region == null) {
			region = new MapRegion(dir, rx, rz);
			regions.put(key, region);
		}
		return region;
	}

	/** Region already in memory or null; used by renderers so painting never triggers disk reads. */
	public MapRegion loadedRegion(int rx, int rz) {
		return regions.get(MapRegion.key(rx, rz));
	}

	public int color(int bx, int bz) {
		MapRegion region = regions.get(MapRegion.key(bx >> MapRegion.SHIFT, bz >> MapRegion.SHIFT));
		return region == null ? 0 : region.get(bx, bz);
	}

	public void set(int bx, int bz, int argb) {
		region(bx >> MapRegion.SHIFT, bz >> MapRegion.SHIFT).set(bx, bz, argb);
	}

	public Collection<MapRegion> regions() {
		return regions.values();
	}

	public void saveAll() {
		for (MapRegion region : regions.values()) {
			region.save();
		}
	}

	public void dispose() {
		for (MapRegion region : regions.values()) {
			region.save();
			region.deleteTexture();
		}
		regions.clear();
	}
}
