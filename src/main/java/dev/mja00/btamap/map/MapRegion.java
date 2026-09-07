package dev.mja00.btamap.map;

import dev.mja00.btamap.BtaMap;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.IntBuffer;
import javax.imageio.ImageIO;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL41;

/** A 512x512 block tile of explored map colors, persisted as PNG and mirrored into a GL texture on demand. */
public final class MapRegion {
	public static final int SHIFT = 9;
	public static final int SIZE = 1 << SHIFT;
	private static final int MASK = SIZE - 1;

	private static IntBuffer uploadBuffer;

	public final int rx;
	public final int rz;
	private final File file;
	private final int[] colors = new int[SIZE * SIZE];
	private boolean saveDirty;
	private boolean textureDirty;
	private int textureId = -1;

	MapRegion(File dir, int rx, int rz) {
		this.rx = rx;
		this.rz = rz;
		this.file = new File(dir, "r." + rx + "." + rz + ".png");
		load();
	}

	public static long key(int rx, int rz) {
		return (long) rx << 32 | rz & 0xFFFFFFFFL;
	}

	public int get(int bx, int bz) {
		return colors[(bz & MASK) << SHIFT | bx & MASK];
	}

	public void set(int bx, int bz, int argb) {
		int index = (bz & MASK) << SHIFT | bx & MASK;
		if (colors[index] != argb) {
			colors[index] = argb;
			saveDirty = true;
			textureDirty = true;
		}
	}

	public boolean isEmpty() {
		for (int color : colors) {
			if (color != 0) {
				return false;
			}
		}
		return true;
	}

	private void load() {
		if (!file.isFile()) {
			return;
		}
		try {
			BufferedImage image = ImageIO.read(file);
			if (image == null || image.getWidth() != SIZE || image.getHeight() != SIZE) {
				BtaMap.LOGGER.warn("Ignoring malformed map region {}", file);
				return;
			}
			image.getRGB(0, 0, SIZE, SIZE, colors, 0, SIZE);
			textureDirty = true;
		} catch (IOException e) {
			BtaMap.LOGGER.warn("Failed to read map region {}", file, e);
		}
	}

	public void save() {
		if (!saveDirty) {
			return;
		}
		saveDirty = false;
		File parent = file.getParentFile();
		if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
			BtaMap.LOGGER.warn("Could not create map directory {}", parent);
			return;
		}
		BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
		image.setRGB(0, 0, SIZE, SIZE, colors, 0, SIZE);
		File tmp = new File(parent, file.getName() + ".tmp");
		try {
			ImageIO.write(image, "png", tmp);
			if (!tmp.renameTo(file)) {
				if (!file.delete() || !tmp.renameTo(file)) {
					BtaMap.LOGGER.warn("Could not replace map region {}", file);
				}
			}
		} catch (IOException e) {
			BtaMap.LOGGER.warn("Failed to write map region {}", file, e);
		}
	}

	/** Returns a GL texture holding the current colors; must be called on the render thread. */
	public int texture() {
		if (textureId < 0) {
			textureId = GL41.glGenTextures();
			GL41.glBindTexture(GL41.GL_TEXTURE_2D, textureId);
			GL41.glTexParameteri(GL41.GL_TEXTURE_2D, GL41.GL_TEXTURE_MIN_FILTER, GL41.GL_NEAREST);
			GL41.glTexParameteri(GL41.GL_TEXTURE_2D, GL41.GL_TEXTURE_MAG_FILTER, GL41.GL_NEAREST);
			GL41.glTexParameteri(GL41.GL_TEXTURE_2D, GL41.GL_TEXTURE_WRAP_S, GL41.GL_CLAMP_TO_EDGE);
			GL41.glTexParameteri(GL41.GL_TEXTURE_2D, GL41.GL_TEXTURE_WRAP_T, GL41.GL_CLAMP_TO_EDGE);
			GL41.glTexImage2D(GL41.GL_TEXTURE_2D, 0, GL41.GL_RGBA8, SIZE, SIZE, 0, GL41.GL_BGRA, GL41.GL_UNSIGNED_INT_8_8_8_8_REV, (IntBuffer) null);
			textureDirty = true;
		}
		if (textureDirty) {
			textureDirty = false;
			upload(textureId, colors, SIZE, SIZE);
		}
		return textureId;
	}

	/** Uploads ARGB ints straight into a bound RGBA texture; BGRA + 8_8_8_8_REV matches Java's int layout. */
	public static void upload(int textureId, int[] argb, int width, int height) {
		if (uploadBuffer == null || uploadBuffer.capacity() < argb.length) {
			uploadBuffer = BufferUtils.createIntBuffer(argb.length);
		}
		uploadBuffer.clear();
		uploadBuffer.put(argb, 0, width * height);
		uploadBuffer.flip();
		GL41.glBindTexture(GL41.GL_TEXTURE_2D, textureId);
		GL41.glTexSubImage2D(GL41.GL_TEXTURE_2D, 0, 0, 0, width, height, GL41.GL_BGRA, GL41.GL_UNSIGNED_INT_8_8_8_8_REV, uploadBuffer);
	}

	public void deleteTexture() {
		if (textureId >= 0) {
			GL41.glDeleteTextures(textureId);
			textureId = -1;
		}
	}
}
