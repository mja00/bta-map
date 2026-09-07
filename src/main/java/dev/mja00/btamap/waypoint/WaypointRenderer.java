package dev.mja00.btamap.waypoint;

import dev.mja00.btamap.BtaMapOptions;
import dev.mja00.btamap.map.MapManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.camera.ICamera;
import net.minecraft.client.render.font.FontRenderer;
import net.minecraft.client.render.renderer.BlendFactor;
import net.minecraft.client.render.renderer.GLRenderer;
import net.minecraft.client.render.renderer.Shaders;
import net.minecraft.client.render.renderer.State;
import net.minecraft.client.render.tessellator.TessellatorShader;

/** Draws a translucent beam and floating label at each waypoint of the current dimension. */
public final class WaypointRenderer {
	private static final double MAX_DISTANCE = 1024.0;
	private static final float BEAM_HALF_WIDTH = 0.25F;
	private static final float LABEL_SCALE = 0.026666667F;

	private WaypointRenderer() {
	}

	public static void render(Minecraft mc, float partialTick) {
		if (!BtaMapOptions.WAYPOINT_BEAMS.value || mc.thePlayer == null || mc.activeCamera == null) {
			return;
		}
		WaypointStore store = MapManager.INSTANCE.waypoints();
		if (store == null) {
			return;
		}
		ICamera camera = mc.activeCamera;
		double camX = camera.getX(partialTick);
		double camY = camera.getY(partialTick);
		double camZ = camera.getZ(partialTick);
		float yaw = (float) camera.getYRot(partialTick);
		float pitch = (float) camera.getXRot(partialTick);
		int dimension = MapManager.INSTANCE.dimension();
		int minY = mc.currentWorld.getWorldType().getMinY(mc.currentWorld);
		int maxY = mc.currentWorld.getWorldType().getMaxY(mc.currentWorld);

		GLRenderer.pushFrame();
		GLRenderer.globalSetLightEnabled(false);
		GLRenderer.enableState(State.BLEND);
		GLRenderer.setBlendFunc(BlendFactor.SRC_ALPHA, BlendFactor.ONE_MINUS_SRC_ALPHA);
		GLRenderer.disableState(State.CULL_FACE);
		GLRenderer.setDepthMask(false);
		TessellatorShader t = GLRenderer.getTessellator();
		for (Waypoint waypoint : store.all()) {
			if (!waypoint.visible || waypoint.dimension != dimension) {
				continue;
			}
			double dx = waypoint.x + 0.5 - camX;
			double dz = waypoint.z + 0.5 - camZ;
			double distance = Math.sqrt(dx * dx + dz * dz);
			if (distance > MAX_DISTANCE) {
				continue;
			}
			drawBeam(t, waypoint, dx, minY - camY, maxY + 1 - camY, dz);
			double labelY = Math.max(waypoint.y + 1.5, camY + Math.min(distance, 64.0) * 0.1) - camY;
			drawLabel(mc.font, t, waypoint, distance, dx, labelY, dz, yaw, pitch);
		}
		GLRenderer.setDepthMask(true);
		GLRenderer.enableState(State.CULL_FACE);
		GLRenderer.globalSetLightEnabled(true);
		GLRenderer.popFrame();
	}

	/** Two crossed quads read as a column from every angle without needing a real cylinder. */
	private static void drawBeam(TessellatorShader t, Waypoint waypoint, double x, double y0, double y1, double z) {
		GLRenderer.pushFrame();
		GLRenderer.setShader(Shaders.COLOR);
		t.startDrawingQuads();
		t.setColor2i(waypoint.color, 0x60);
		float w = BEAM_HALF_WIDTH;
		t.addVertex(x - w, y0, z);
		t.addVertex(x - w, y1, z);
		t.addVertex(x + w, y1, z);
		t.addVertex(x + w, y0, z);
		t.addVertex(x, y0, z - w);
		t.addVertex(x, y1, z - w);
		t.addVertex(x, y1, z + w);
		t.addVertex(x, y0, z + w);
		t.draw();
		GLRenderer.popFrame();
	}

	private static void drawLabel(FontRenderer font, TessellatorShader t, Waypoint waypoint, double distance, double x, double y, double z, float yaw, float pitch) {
		String text = (waypoint.name.isEmpty() ? "" : waypoint.name + " ") + "[" + (int) distance + "m]";
		int halfWidth = font.stringWidth(text) / 2;
		float scale = LABEL_SCALE * (float) Math.max(1.0, distance / 8.0);

		GLRenderer.pushFrame();
		GLRenderer.modelM4f().translate((float) x, (float) y, (float) z);
		GLRenderer.modelM4f().rotateY((float) Math.toRadians(-yaw));
		GLRenderer.modelM4f().rotateX((float) Math.toRadians(pitch));
		GLRenderer.modelM4f().scale(-scale, -scale, scale);
		GLRenderer.disableState(State.DEPTH_TEST);

		GLRenderer.pushFrame();
		GLRenderer.setShader(Shaders.COLOR);
		t.startDrawingQuads();
		t.setColor4f(0.0F, 0.0F, 0.0F, 0.4F);
		t.addVertex(-halfWidth - 2, -2.0, 0.0);
		t.addVertex(-halfWidth - 2, 9.0, 0.0);
		t.addVertex(halfWidth + 2, 9.0, 0.0);
		t.addVertex(halfWidth + 2, -2.0, 0.0);
		t.draw();
		t.startDrawingQuads();
		t.setColor1i(waypoint.color);
		t.addVertex(-halfWidth - 2, -2.0, 0.0);
		t.addVertex(-halfWidth - 2, 9.0, 0.0);
		t.addVertex(-halfWidth - 1, 9.0, 0.0);
		t.addVertex(-halfWidth - 1, -2.0, 0.0);
		t.draw();
		GLRenderer.popFrame();

		font.render(text, -halfWidth, 0).setColor(0xFFFFFFFF).call();
		GLRenderer.enableState(State.DEPTH_TEST);
		GLRenderer.popFrame();
	}
}
