package dev.mja00.btamap.map;

import dev.mja00.btamap.waypoint.Waypoint;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.render.font.FontRenderer;
import net.minecraft.client.render.renderer.BlendFactor;
import net.minecraft.client.render.renderer.DrawMode;
import net.minecraft.client.render.renderer.GLRenderer;
import net.minecraft.client.render.renderer.Shaders;
import net.minecraft.client.render.renderer.State;
import net.minecraft.client.render.tessellator.TessellatorShader;
import org.lwjgl.opengl.GL41;

/** GUI-space drawing helpers shared by the minimap and the fullscreen map. */
public final class MapRenderer {
	public static final int UNEXPLORED = 0xFF1B1B1B;

	private MapRenderer() {
	}

	/**
	 * Draws every region overlapping the view rectangle as one textured quad each.
	 * World block (centerX, centerZ) lands on screen pixel (screenCx, screenCy); scale is pixels per block.
	 */
	public static void drawRegions(MapWorld map, double centerX, double centerZ, double scale, double screenCx, double screenCy, int viewX, int viewY, int viewW, int viewH) {
		double blocksW = viewW / scale;
		double blocksH = viewH / scale;
		double minBx = centerX - (screenCx - viewX) / scale;
		double minBz = centerZ - (screenCy - viewY) / scale;
		int rx0 = (int) Math.floor(minBx) >> MapRegion.SHIFT;
		int rz0 = (int) Math.floor(minBz) >> MapRegion.SHIFT;
		int rx1 = (int) Math.floor(minBx + blocksW) >> MapRegion.SHIFT;
		int rz1 = (int) Math.floor(minBz + blocksH) >> MapRegion.SHIFT;

		GLRenderer.pushFrame();
		GLRenderer.setShader(Shaders.INTERFACE);
		GLRenderer.setColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		GLRenderer.enableState(State.BLEND);
		GLRenderer.setBlendFunc(BlendFactor.SRC_ALPHA, BlendFactor.ONE_MINUS_SRC_ALPHA);
		TessellatorShader t = GLRenderer.getTessellator();
		for (int rz = rz0; rz <= rz1; rz++) {
			for (int rx = rx0; rx <= rx1; rx++) {
				MapRegion region = map.region(rx, rz);
				double x0 = screenCx + ((double) (rx << MapRegion.SHIFT) - centerX) * scale;
				double y0 = screenCy + ((double) (rz << MapRegion.SHIFT) - centerZ) * scale;
				double x1 = x0 + MapRegion.SIZE * scale;
				double y1 = y0 + MapRegion.SIZE * scale;
				GL41.glBindTexture(GL41.GL_TEXTURE_2D, region.texture());
				t.startDrawingQuads();
				t.addVertexWithUV(x0, y1, 0.0, 0.0, 1.0);
				t.addVertexWithUV(x1, y1, 0.0, 1.0, 1.0);
				t.addVertexWithUV(x1, y0, 0.0, 1.0, 0.0);
				t.addVertexWithUV(x0, y0, 0.0, 0.0, 0.0);
				t.draw();
			}
		}
		GLRenderer.popFrame();
	}

	/** Solid triangle pointing along heading (radians, 0 = up, clockwise) centered on (cx, cy). */
	public static void drawArrow(double cx, double cy, double heading, double size, int argb) {
		double sin = Math.sin(heading);
		double cos = Math.cos(heading);
		GLRenderer.pushFrame();
		GLRenderer.setShader(Shaders.COLOR);
		GLRenderer.enableState(State.BLEND);
		GLRenderer.setBlendFunc(BlendFactor.SRC_ALPHA, BlendFactor.ONE_MINUS_SRC_ALPHA);
		TessellatorShader t = GLRenderer.getTessellator();
		t.startDrawing(DrawMode.TRIANGLES);
		t.setColor1i(argb);
		addRotated(t, cx, cy, 0.0, -size, sin, cos);
		addRotated(t, cx, cy, -size * 0.7, size * 0.8, sin, cos);
		addRotated(t, cx, cy, 0.0, size * 0.35, sin, cos);
		addRotated(t, cx, cy, 0.0, -size, sin, cos);
		addRotated(t, cx, cy, 0.0, size * 0.35, sin, cos);
		addRotated(t, cx, cy, size * 0.7, size * 0.8, sin, cos);
		t.draw();
		GLRenderer.popFrame();
	}

	private static void addRotated(TessellatorShader t, double cx, double cy, double x, double y, double sin, double cos) {
		t.addVertex(cx + x * cos - y * sin, cy + x * sin + y * cos, 0.0);
	}

	/** Small colored diamond with dark outline, optionally labelled below with the waypoint initial. */
	public static void drawWaypointMarker(Gui gui, FontRenderer font, Waypoint waypoint, int x, int y, boolean label) {
		gui.drawRect(x - 3, y - 3, x + 4, y + 4, 0xC0000000);
		gui.drawRect(x - 2, y - 2, x + 3, y + 3, waypoint.color);
		if (label) {
			gui.drawStringCenteredShadow(font, waypoint.initial(), x, y - 12, 0xFFFFFFFF);
		}
	}

	public static void fillBackground(Gui gui, int x, int y, int w, int h) {
		gui.drawRect(x, y, x + w, y + h, UNEXPLORED);
	}
}
