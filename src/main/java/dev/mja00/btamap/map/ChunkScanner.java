package dev.mja00.btamap.map;

import net.minecraft.core.block.Block;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.world.World;
import net.minecraft.core.world.chunk.Chunk;
import net.minecraft.core.world.pos.TilePos;
import net.minecraft.core.world.type.WorldType;

/** Turns a loaded chunk into 16x16 shaded map colors. */
public final class ChunkScanner {
	private static final int MAX_WATER_DEPTH = 24;

	private final int[] colors = new int[256];
	private final int[] heights = new int[256];
	private final TilePos pos = new TilePos();

	public void scan(World world, Chunk chunk, MapWorld map) {
		WorldType type = world.getWorldType();
		int minY = type.getMinY(world);
		int maxY = type.getMaxY(world);
		boolean ceiling = type.hasCeiling();
		int baseX = chunk.pos.x << 4;
		int baseZ = chunk.pos.z << 4;

		for (int z = 0; z < 16; z++) {
			for (int x = 0; x < 16; x++) {
				int start = ceiling ? maxY : Math.min(maxY, chunk.getHeightValue(x, z));
				scanColumn(world, chunk, x, z, baseX + x, baseZ + z, start, minY, ceiling);
			}
		}

		for (int z = 0; z < 16; z++) {
			for (int x = 0; x < 16; x++) {
				int index = z << 4 | x;
				int color = colors[index];
				if (color == 0) {
					continue;
				}
				// Compare against the column to the north so slopes get a light/shadow edge like a relief map.
				int neighbor = z > 0 ? heights[index - 16] : heights[index + 16];
				int h = heights[index];
				float shade = h > neighbor ? 1.12F : h < neighbor ? 0.82F : 1.0F;
				map.set(baseX + x, baseZ + z, BlockColors.scale(color, shade));
			}
		}
	}

	private void scanColumn(World world, Chunk chunk, int cx, int cz, int wx, int wz, int start, int minY, boolean ceiling) {
		int index = cz << 4 | cx;
		colors[index] = 0;
		heights[index] = minY;
		int y = start;
		if (ceiling) {
			// Skip the bedrock roof so nether-style dimensions show the caverns underneath instead of a flat slab.
			while (y > minY && chunk.getBlockID(cx, y, cz) != 0) {
				y--;
			}
		}

		int waterColor = 0;
		int waterTop = 0;
		for (; y >= minY; y--) {
			int id = chunk.getBlockID(cx, y, cz);
			if (id == 0) {
				continue;
			}
			Block<?> block = Blocks.getBlock(id);
			int meta = chunk.getBlockMetadata(cx, y, cz);
			pos.x = wx;
			pos.y = y;
			pos.z = wz;
			int color = BlockColors.worldColor(world, pos, block, meta);
			if (color == 0) {
				continue;
			}
			if (block.getMaterial().isLiquid()) {
				if (waterColor == 0) {
					waterColor = color;
					waterTop = y;
				} else if (waterTop - y >= MAX_WATER_DEPTH) {
					finish(index, waterColor, waterTop);
					return;
				}
				continue;
			}
			if (waterColor != 0) {
				float depth = Math.min(1.0F, (waterTop - y) / (float) MAX_WATER_DEPTH);
				// Deeper water hides more of the floor, which is what makes lakes readable at a glance.
				int blended = BlockColors.blend(waterColor, color, 0.45F + depth * 0.5F);
				finish(index, blended, waterTop);
				return;
			}
			finish(index, color, y);
			return;
		}
		if (waterColor != 0) {
			finish(index, waterColor, waterTop);
		}
	}

	private void finish(int index, int color, int height) {
		colors[index] = color;
		heights[index] = height;
	}
}
