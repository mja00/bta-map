package dev.mja00.btamap.hud;

import dev.mja00.btamap.BtaMapOptions;
import dev.mja00.btamap.map.MapManager;
import dev.mja00.btamap.map.MapRegion;
import dev.mja00.btamap.map.MapRenderer;
import dev.mja00.btamap.map.MapWorld;
import dev.mja00.btamap.waypoint.Waypoint;
import dev.mja00.btamap.waypoint.WaypointStore;
import java.nio.IntBuffer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.hud.HudIngame;
import net.minecraft.client.gui.hud.component.HudComponentMovable;
import net.minecraft.client.gui.hud.component.layout.Layout;
import net.minecraft.client.gui.options.components.BooleanOptionComponent;
import net.minecraft.client.gui.options.components.KeyBindingComponent;
import net.minecraft.client.gui.options.components.ToggleableOptionComponent;
import net.minecraft.client.option.GameSettings;
import net.minecraft.client.render.renderer.BlendFactor;
import net.minecraft.client.render.renderer.DrawMode;
import net.minecraft.client.render.renderer.GLRenderer;
import net.minecraft.client.render.renderer.Shaders;
import net.minecraft.client.render.renderer.State;
import net.minecraft.client.render.tessellator.TessellatorShader;
import net.minecraft.core.entity.player.Player;
import org.lwjgl.opengl.GL41;

public class HudComponentMinimap extends HudComponentMovable {
	/** Texels of map around the player kept on the GPU; large enough to cover the widest zoom when rotated. */
	private static final int TEXTURE_SIZE = 512;
	private static final int CIRCLE_SEGMENTS = 64;
	private static final int REFRESH_TICKS = 10;

	private final int[] pixels = new int[TEXTURE_SIZE * TEXTURE_SIZE];
	private int textureId = -1;
	private int textureCenterX = Integer.MIN_VALUE;
	private int textureCenterZ = Integer.MIN_VALUE;
	private MapWorld textureMap;
	private long lastRefreshTick = Long.MIN_VALUE;
	private long tickCounter;

	public HudComponentMinimap(String key, Layout layout) {
		super(key, BtaMapOptions.MINIMAP_SIZE.value, BtaMapOptions.MINIMAP_SIZE.value, layout);
		this.<HudComponentMinimap>addAttachedOption(BtaMapOptions.MINIMAP_ENABLED, () -> new BooleanOptionComponent(BtaMapOptions.MINIMAP_ENABLED))
			.<HudComponentMinimap>addAttachedOption(BtaMapOptions.MINIMAP_SHAPE, () -> new ToggleableOptionComponent<>(BtaMapOptions.MINIMAP_SHAPE))
			.<HudComponentMinimap>addAttachedOption(BtaMapOptions.MINIMAP_SIZE, () -> new ToggleableOptionComponent<>(BtaMapOptions.MINIMAP_SIZE))
			.<HudComponentMinimap>addAttachedOption(BtaMapOptions.MINIMAP_ZOOM, () -> new ToggleableOptionComponent<>(BtaMapOptions.MINIMAP_ZOOM))
			.<HudComponentMinimap>addAttachedOption(BtaMapOptions.MINIMAP_ROTATE, () -> new BooleanOptionComponent(BtaMapOptions.MINIMAP_ROTATE))
			.<HudComponentMinimap>addAttachedOption(BtaMapOptions.SHOW_COORDS, () -> new BooleanOptionComponent(BtaMapOptions.SHOW_COORDS))
			.<HudComponentMinimap>addAttachedOption(BtaMapOptions.SHOW_WAYPOINTS, () -> new BooleanOptionComponent(BtaMapOptions.SHOW_WAYPOINTS))
			.<HudComponentMinimap>addAttachedOption(BtaMapOptions.WAYPOINT_BEAMS, () -> new BooleanOptionComponent(BtaMapOptions.WAYPOINT_BEAMS))
			.<HudComponentMinimap>addAttachedKeyBinding(BtaMapOptions.KEY_OPEN_MAP, () -> new KeyBindingComponent(BtaMapOptions.KEY_OPEN_MAP))
			.<HudComponentMinimap>addAttachedKeyBinding(BtaMapOptions.KEY_NEW_WAYPOINT, () -> new KeyBindingComponent(BtaMapOptions.KEY_NEW_WAYPOINT))
			.<HudComponentMinimap>addAttachedKeyBinding(BtaMapOptions.KEY_TOGGLE_MINIMAP, () -> new KeyBindingComponent(BtaMapOptions.KEY_TOGGLE_MINIMAP))
			.<HudComponentMinimap>addAttachedKeyBinding(BtaMapOptions.KEY_ZOOM_IN, () -> new KeyBindingComponent(BtaMapOptions.KEY_ZOOM_IN))
			.<HudComponentMinimap>addAttachedKeyBinding(BtaMapOptions.KEY_ZOOM_OUT, () -> new KeyBindingComponent(BtaMapOptions.KEY_ZOOM_OUT));
	}

	@Override
	public int getBaseXSize() {
		return BtaMapOptions.MINIMAP_SIZE.value;
	}

	@Override
	public int getBaseYSize() {
		// Leave a text line under the map for coordinates.
		return BtaMapOptions.MINIMAP_SIZE.value + (BtaMapOptions.SHOW_COORDS.value ? 10 : 0);
	}

	@Override
	public boolean isVisible() {
		return BtaMapOptions.MINIMAP_ENABLED.value && GameSettings.IMMERSIVE_MODE.drawOverlays() && !GameSettings.SHOW_DEBUG_SCREEN.value;
	}

	@Override
	public boolean isEnabled() {
		return BtaMapOptions.MINIMAP_ENABLED.value;
	}

	@Override
	public void render(HudIngame hud, int xSizeScreen, int ySizeScreen, float partialTick) {
		Player player = mc.thePlayer;
		MapWorld map = MapManager.INSTANCE.map();
		if (player == null || map == null) {
			return;
		}
		int x = this.getLayout().getComponentX(this, xSizeScreen);
		int y = this.getLayout().getComponentY(this, ySizeScreen);
		int size = BtaMapOptions.MINIMAP_SIZE.value;
		double px = player.xo + (player.x - player.xo) * partialTick;
		double pz = player.zo + (player.z - player.zo) * partialTick;
		float yaw = player.yRotO + (player.yRot - player.yRotO) * partialTick;

		refreshTexture(map, (int) Math.floor(px), (int) Math.floor(pz));
		boolean round = BtaMapOptions.MINIMAP_SHAPE.value == BtaMapOptions.Shape.ROUND;
		boolean rotate = BtaMapOptions.MINIMAP_ROTATE.value;
		double angle = rotate ? Math.PI - Math.toRadians(yaw) : 0.0;
		double cx = x + size / 2.0;
		double cy = y + size / 2.0;
		drawMap(round, cx, cy, size / 2.0, angle, px, pz);
		drawFrame(hud, round, x, y, size);

		if (BtaMapOptions.SHOW_WAYPOINTS.value) {
			drawWaypoints(hud, cx, cy, size / 2.0, angle, px, pz, round);
		}
		// Arrow points where the player looks: up when the map rotates with them, otherwise by heading.
		MapRenderer.drawArrow(cx, cy, rotate ? 0.0 : Math.toRadians(yaw) + Math.PI, 4.0, 0xFFFFFFFF);
		if (rotate) {
			double n = -angle;
			int nx = (int) Math.round(cx + Math.sin(n) * (size / 2.0 - 6));
			int ny = (int) Math.round(cy - Math.cos(n) * (size / 2.0 - 6));
			hud.drawStringCenteredShadow(mc.font, "N", nx, ny - 4, 0xFFFF5555);
		}
		if (BtaMapOptions.SHOW_COORDS.value) {
			String coords = (int) Math.floor(player.x) + ", " + (int) Math.floor(player.y) + ", " + (int) Math.floor(player.z);
			hud.drawStringCenteredShadow(mc.font, coords, (int) cx, y + size + 1, 0xFFFFFFFF);
		}
	}

	@Override
	public void renderPreview(Gui gui, Layout layout, int xSizeScreen, int ySizeScreen) {
		int x = layout.getComponentX(this, xSizeScreen);
		int y = layout.getComponentY(this, ySizeScreen);
		int size = BtaMapOptions.MINIMAP_SIZE.value;
		boolean round = BtaMapOptions.MINIMAP_SHAPE.value == BtaMapOptions.Shape.ROUND;
		if (round) {
			drawDisc(x + size / 2.0, y + size / 2.0, size / 2.0, 0xFF3A6B35);
		} else {
			gui.drawRect(x, y, x + size, y + size, 0xFF3A6B35);
		}
		drawFrame(gui, round, x, y, size);
		MapRenderer.drawArrow(x + size / 2.0, y + size / 2.0, 0.0, 4.0, 0xFFFFFFFF);
		if (BtaMapOptions.SHOW_COORDS.value) {
			gui.drawStringCenteredShadow(mc.font, "0, 64, 0", x + size / 2, y + size + 1, 0xFFFFFFFF);
		}
	}

	private void refreshTexture(MapWorld map, int centerX, int centerZ) {
		tickCounter++;
		boolean moved = centerX != textureCenterX || centerZ != textureCenterZ || map != textureMap;
		if (!moved && tickCounter - lastRefreshTick < REFRESH_TICKS) {
			return;
		}
		lastRefreshTick = tickCounter;
		textureCenterX = centerX;
		textureCenterZ = centerZ;
		textureMap = map;
		int half = TEXTURE_SIZE / 2;
		for (int j = 0; j < TEXTURE_SIZE; j++) {
			int bz = centerZ - half + j;
			int row = j * TEXTURE_SIZE;
			for (int i = 0; i < TEXTURE_SIZE; i++) {
				int color = map.color(centerX - half + i, bz);
				pixels[row + i] = color == 0 ? MapRenderer.UNEXPLORED : color;
			}
		}
		if (textureId < 0) {
			textureId = GL41.glGenTextures();
			GL41.glBindTexture(GL41.GL_TEXTURE_2D, textureId);
			GL41.glTexParameteri(GL41.GL_TEXTURE_2D, GL41.GL_TEXTURE_MIN_FILTER, GL41.GL_NEAREST);
			GL41.glTexParameteri(GL41.GL_TEXTURE_2D, GL41.GL_TEXTURE_MAG_FILTER, GL41.GL_NEAREST);
			GL41.glTexParameteri(GL41.GL_TEXTURE_2D, GL41.GL_TEXTURE_WRAP_S, GL41.GL_CLAMP_TO_EDGE);
			GL41.glTexParameteri(GL41.GL_TEXTURE_2D, GL41.GL_TEXTURE_WRAP_T, GL41.GL_CLAMP_TO_EDGE);
			GL41.glTexImage2D(GL41.GL_TEXTURE_2D, 0, GL41.GL_RGBA8, TEXTURE_SIZE, TEXTURE_SIZE, 0, GL41.GL_BGRA, GL41.GL_UNSIGNED_INT_8_8_8_8_REV, (IntBuffer) null);
		}
		MapRegion.upload(textureId, pixels, TEXTURE_SIZE, TEXTURE_SIZE);
	}

	/** Draws the map texture with UVs rotated around the player so no clipping is needed for the round shape. */
	private void drawMap(boolean round, double cx, double cy, double radius, double angle, double px, double pz) {
		double scale = BtaMapOptions.minimapPixelsPerBlock();
		double half = TEXTURE_SIZE / 2.0;
		double uCenter = (px - textureCenterX + half) / TEXTURE_SIZE;
		double vCenter = (pz - textureCenterZ + half) / TEXTURE_SIZE;
		double sin = Math.sin(angle);
		double cos = Math.cos(angle);

		GLRenderer.pushFrame();
		GLRenderer.setShader(Shaders.INTERFACE);
		GLRenderer.setColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		GLRenderer.disableState(State.BLEND);
		GL41.glBindTexture(GL41.GL_TEXTURE_2D, textureId);
		TessellatorShader t = GLRenderer.getTessellator();
		if (round) {
			t.startDrawing(DrawMode.TRIANGLE_FAN);
			t.addVertexWithUV(cx, cy, 0.0, uCenter, vCenter);
			for (int i = 0; i <= CIRCLE_SEGMENTS; i++) {
				double a = i * (Math.PI * 2.0 / CIRCLE_SEGMENTS);
				addMapVertex(t, cx, cy, Math.cos(a) * radius, Math.sin(a) * radius, uCenter, vCenter, scale, sin, cos);
			}
		} else {
			t.startDrawingQuads();
			addMapVertex(t, cx, cy, -radius, radius, uCenter, vCenter, scale, sin, cos);
			addMapVertex(t, cx, cy, radius, radius, uCenter, vCenter, scale, sin, cos);
			addMapVertex(t, cx, cy, radius, -radius, uCenter, vCenter, scale, sin, cos);
			addMapVertex(t, cx, cy, -radius, -radius, uCenter, vCenter, scale, sin, cos);
		}
		t.draw();
		GLRenderer.popFrame();
	}

	/** Screen offset (sx, sy) from the center is un-rotated back into world blocks and then into texture UVs. */
	private static void addMapVertex(TessellatorShader t, double cx, double cy, double sx, double sy, double uCenter, double vCenter, double scale, double sin, double cos) {
		double wx = (sx * cos + sy * sin) / scale;
		double wz = (-sx * sin + sy * cos) / scale;
		t.addVertexWithUV(cx + sx, cy + sy, 0.0, uCenter + wx / TEXTURE_SIZE, vCenter + wz / TEXTURE_SIZE);
	}

	private void drawWaypoints(HudIngame hud, double cx, double cy, double radius, double angle, double px, double pz, boolean round) {
		WaypointStore store = MapManager.INSTANCE.waypoints();
		if (store == null) {
			return;
		}
		double scale = BtaMapOptions.minimapPixelsPerBlock();
		double sin = Math.sin(angle);
		double cos = Math.cos(angle);
		int dimension = MapManager.INSTANCE.dimension();
		double limit = radius - 4;
		for (Waypoint waypoint : store.all()) {
			if (!waypoint.visible || waypoint.dimension != dimension) {
				continue;
			}
			double wx = (waypoint.x + 0.5 - px) * scale;
			double wz = (waypoint.z + 0.5 - pz) * scale;
			double sx = wx * cos - wz * sin;
			double sy = wx * sin + wz * cos;
			// Off-map waypoints are pinned to the edge so the player still knows which way to go.
			if (round) {
				double dist = Math.sqrt(sx * sx + sy * sy);
				if (dist > limit) {
					sx = sx / dist * limit;
					sy = sy / dist * limit;
				}
			} else {
				sx = Math.max(-limit, Math.min(limit, sx));
				sy = Math.max(-limit, Math.min(limit, sy));
			}
			MapRenderer.drawWaypointMarker(hud, mc.font, waypoint, (int) Math.round(cx + sx), (int) Math.round(cy + sy), false);
		}
	}

	private void drawFrame(Gui gui, boolean round, int x, int y, int size) {
		if (round) {
			drawRing(x + size / 2.0, y + size / 2.0, size / 2.0, 1.5, 0xFF000000);
		} else {
			gui.drawBox(x - 1, y - 1, x + size + 1, y + size + 1, 0xFF000000, 1);
		}
	}

	private static void drawDisc(double cx, double cy, double radius, int argb) {
		GLRenderer.pushFrame();
		GLRenderer.setShader(Shaders.COLOR);
		GLRenderer.enableState(State.BLEND);
		GLRenderer.setBlendFunc(BlendFactor.SRC_ALPHA, BlendFactor.ONE_MINUS_SRC_ALPHA);
		TessellatorShader t = GLRenderer.getTessellator();
		t.startDrawing(DrawMode.TRIANGLE_FAN);
		t.setColor1i(argb);
		t.addVertex(cx, cy, 0.0);
		for (int i = 0; i <= CIRCLE_SEGMENTS; i++) {
			double a = i * (Math.PI * 2.0 / CIRCLE_SEGMENTS);
			t.addVertex(cx + Math.cos(a) * radius, cy + Math.sin(a) * radius, 0.0);
		}
		t.draw();
		GLRenderer.popFrame();
	}

	private static void drawRing(double cx, double cy, double radius, double thickness, int argb) {
		GLRenderer.pushFrame();
		GLRenderer.setShader(Shaders.COLOR);
		GLRenderer.enableState(State.BLEND);
		GLRenderer.setBlendFunc(BlendFactor.SRC_ALPHA, BlendFactor.ONE_MINUS_SRC_ALPHA);
		TessellatorShader t = GLRenderer.getTessellator();
		t.startDrawing(DrawMode.TRIANGLE_STRIP);
		t.setColor1i(argb);
		for (int i = 0; i <= CIRCLE_SEGMENTS; i++) {
			double a = i * (Math.PI * 2.0 / CIRCLE_SEGMENTS);
			double c = Math.cos(a);
			double s = Math.sin(a);
			t.addVertex(cx + c * (radius - 0.5), cy + s * (radius - 0.5), 0.0);
			t.addVertex(cx + c * (radius + thickness), cy + s * (radius + thickness), 0.0);
		}
		t.draw();
		GLRenderer.popFrame();
	}
}
