package dev.mja00.btamap.map;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import java.awt.image.BufferedImage;
import net.minecraft.client.render.block.color.BlockColorDispatcher;
import net.minecraft.client.render.block.model.BlockModel;
import net.minecraft.client.render.block.model.BlockModelDispatcher;
import net.minecraft.client.render.texture.stitcher.IconCoordinate;
import net.minecraft.client.render.texture.stitcher.TextureRegistry;
import net.minecraft.core.block.Block;
import net.minecraft.core.util.helper.Side;
import net.minecraft.core.world.WorldSource;
import net.minecraft.core.world.pos.TilePos;

/**
 * Derives map colors from the stitched block atlas so texture packs and modded blocks are picked up automatically.
 * Colors are the average of the block's top face; 0 means the block is see-through on the map.
 */
public final class BlockColors {
	private static final Int2IntOpenHashMap CACHE = new Int2IntOpenHashMap();
	private static final int MISSING = 0;

	static {
		CACHE.defaultReturnValue(-2);
	}

	private BlockColors() {
	}

	public static void invalidate() {
		CACHE.clear();
	}

	/** Opaque ARGB base color for a block state, or 0 when the top face is mostly transparent. */
	public static int baseColor(Block<?> block, int meta) {
		int key = block.id() << 4 | meta & 15;
		int cached = CACHE.get(key);
		if (cached != -2) {
			return cached;
		}
		int color = computeBaseColor(block, meta);
		CACHE.put(key, color);
		return color;
	}

	/** Base color multiplied by the biome tint the renderer would apply at this position. */
	public static int worldColor(WorldSource world, TilePos pos, Block<?> block, int meta) {
		int base = baseColor(block, meta);
		if (base == MISSING) {
			return MISSING;
		}
		int tint = BlockColorDispatcher.getInstance().getDispatch(block).getWorldColor(world, pos, 0);
		return tint == -1 ? base : multiply(base, tint);
	}

	private static int computeBaseColor(Block<?> block, int meta) {
		BlockModel<?> model = BlockModelDispatcher.getInstance().getDispatch(block);
		if (model == null) {
			return MISSING;
		}
		IconCoordinate icon = model.getParticleTexture(Side.TOP, meta);
		if (icon == null) {
			return MISSING;
		}
		BufferedImage atlas = icon.parentAtlas.colorImage;
		if (atlas == null) {
			return MISSING;
		}
		// Animated textures are stacked vertically; only the first frame is a square icon.
		int size = Math.min(icon.width, icon.height);
		long r = 0;
		long g = 0;
		long b = 0;
		long a = 0;
		int count = 0;
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				int ax = icon.iconX + x;
				int ay = icon.iconY + y;
				if (ax < 0 || ay < 0 || ax >= atlas.getWidth() || ay >= atlas.getHeight()) {
					continue;
				}
				int argb = atlas.getRGB(ax, ay);
				int alpha = argb >>> 24;
				a += alpha;
				r += (argb >> 16 & 0xFF) * alpha;
				g += (argb >> 8 & 0xFF) * alpha;
				b += (argb & 0xFF) * alpha;
				count++;
			}
		}
		if (count == 0 || a == 0) {
			return MISSING;
		}
		// Treat sprites that are mostly holes (plants, torches, rails) as transparent so the ground shows instead.
		if (a / count < 96) {
			return MISSING;
		}
		return 0xFF000000 | (int) (r / a) << 16 | (int) (g / a) << 8 | (int) (b / a);
	}

	public static int multiply(int argb, int tint) {
		int r = (argb >> 16 & 0xFF) * (tint >> 16 & 0xFF) / 255;
		int g = (argb >> 8 & 0xFF) * (tint >> 8 & 0xFF) / 255;
		int b = (argb & 0xFF) * (tint & 0xFF) / 255;
		return argb & 0xFF000000 | r << 16 | g << 8 | b;
	}

	public static int scale(int argb, float factor) {
		int r = Math.min(255, (int) ((argb >> 16 & 0xFF) * factor));
		int g = Math.min(255, (int) ((argb >> 8 & 0xFF) * factor));
		int b = Math.min(255, (int) ((argb & 0xFF) * factor));
		return argb & 0xFF000000 | r << 16 | g << 8 | b;
	}

	public static int blend(int top, int bottom, float topWeight) {
		float bottomWeight = 1.0F - topWeight;
		int r = (int) ((top >> 16 & 0xFF) * topWeight + (bottom >> 16 & 0xFF) * bottomWeight);
		int g = (int) ((top >> 8 & 0xFF) * topWeight + (bottom >> 8 & 0xFF) * bottomWeight);
		int b = (int) ((top & 0xFF) * topWeight + (bottom & 0xFF) * bottomWeight);
		return 0xFF000000 | r << 16 | g << 8 | b;
	}
}
