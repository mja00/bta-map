package dev.mja00.btamap.gui;

import dev.mja00.btamap.BtaMapOptions;
import dev.mja00.btamap.map.MapManager;
import dev.mja00.btamap.map.MapRenderer;
import dev.mja00.btamap.map.MapWorld;
import dev.mja00.btamap.waypoint.Waypoint;
import dev.mja00.btamap.waypoint.WaypointStore;
import net.minecraft.client.gui.ButtonElement;
import net.minecraft.client.gui.Screen;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.lang.I18n;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class ScreenWorldMap extends Screen {
	private static final double MIN_SCALE = 0.125;
	private static final double MAX_SCALE = 8.0;
	private static final int DRAG_THRESHOLD = 3;

	/** View state survives reopening so the player returns to where they were looking. */
	private static double centerX;
	private static double centerZ;
	private static double scale = 1.0;
	private static boolean followPlayer = true;

	private ButtonElement followButton;
	private boolean dragging;
	private int dragStartX;
	private int dragStartY;
	private int lastMouseX;
	private int lastMouseY;
	private boolean dragMoved;

	public ScreenWorldMap(Screen parent) {
		super(parent);
	}

	@Override
	public void init() {
		I18n i18n = I18n.getInstance();
		int bottom = this.height - 24;
		this.add(new ButtonElement(0, this.width / 2 - 154, bottom, 100, 20, i18n.translateKey("gui.btamap.worldmap.addWaypoint")))
			.setListener(button -> addWaypointAt(centerX, centerZ));
		followButton = this.add(new ButtonElement(1, this.width / 2 - 50, bottom, 100, 20, followLabel()));
		followButton.setListener(button -> {
			followPlayer = !followPlayer;
			button.displayString = followLabel();
		});
		this.add(new ButtonElement(2, this.width / 2 + 54, bottom, 100, 20, i18n.translateKey("gui.btamap.done"))).setListener(button -> this.mc.displayScreen(this.parentScreen));
		Player player = this.mc.thePlayer;
		if (followPlayer && player != null) {
			centerX = player.x;
			centerZ = player.z;
		}
	}

	private static String followLabel() {
		return I18n.getInstance().translateKey(followPlayer ? "gui.btamap.worldmap.following" : "gui.btamap.worldmap.follow");
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void render(int mx, int my, float partialTick) {
		Player player = this.mc.thePlayer;
		MapWorld map = MapManager.INSTANCE.map();
		handleScroll(mx, my);
		handleDrag(mx, my);
		if (followPlayer && player != null && !dragging) {
			centerX = player.x;
			centerZ = player.z;
		}

		MapRenderer.fillBackground(this, 0, 0, this.width, this.height);
		double screenCx = this.width / 2.0;
		double screenCy = this.height / 2.0;
		if (map != null) {
			MapRenderer.drawRegions(map, centerX, centerZ, scale, screenCx, screenCy, 0, 0, this.width, this.height);
		}

		WaypointStore store = MapManager.INSTANCE.waypoints();
		Waypoint hovered = null;
		if (store != null) {
			int dimension = MapManager.INSTANCE.dimension();
			for (Waypoint waypoint : store.all()) {
				if (waypoint.dimension != dimension) {
					continue;
				}
				int sx = toScreenX(waypoint.x + 0.5);
				int sy = toScreenY(waypoint.z + 0.5);
				if (sx < -8 || sy < -8 || sx > this.width + 8 || sy > this.height + 8) {
					continue;
				}
				MapRenderer.drawWaypointMarker(this, this.fontRenderer, waypoint, sx, sy, true);
				if (Math.abs(mx - sx) <= 4 && Math.abs(my - sy) <= 4) {
					hovered = waypoint;
				}
			}
		}

		if (player != null) {
			MapRenderer.drawArrow(toScreenX(player.x), toScreenY(player.z), Math.toRadians(player.yRot) + Math.PI, 5.0, 0xFFFFFFFF);
		}

		// Overlays sit above the map but below the buttons.
		this.drawRect(0, 0, this.width, 14, 0x90000000);
		this.drawRect(0, this.height - 30, this.width, this.height, 0x90000000);
		String title = I18n.getInstance().translateKey("gui.btamap.worldmap.title");
		this.drawStringCenteredShadow(this.fontRenderer, title, this.width / 2, 3, 0xFFFFFFFF);
		String zoom = "x" + trim(scale);
		this.drawStringShadow(this.fontRenderer, zoom, this.width - this.fontRenderer.stringWidth(zoom) - 4, 3, 0xFFAAAAAA);
		int bx = (int) Math.floor(toWorldX(mx));
		int bz = (int) Math.floor(toWorldZ(my));
		this.drawStringShadow(this.fontRenderer, bx + ", " + bz, 4, 3, 0xFFAAAAAA);
		if (hovered != null) {
			String label = hovered.name.isEmpty() ? "(" + hovered.x + ", " + hovered.y + ", " + hovered.z + ")" : hovered.name + "  " + hovered.x + ", " + hovered.y + ", " + hovered.z;
			int w = this.fontRenderer.stringWidth(label);
			int tx = Math.min(mx + 8, this.width - w - 4);
			this.drawRect(tx - 2, my - 14, tx + w + 2, my - 3, 0xC0000000);
			this.drawStringShadow(this.fontRenderer, label, tx, my - 12, 0xFFFFFFFF);
		}
		super.render(mx, my, partialTick);
	}

	private static String trim(double value) {
		return value == Math.floor(value) ? Integer.toString((int) value) : Double.toString(value);
	}

	private void handleScroll(int mx, int my) {
		int wheel = Mouse.getDWheel();
		if (wheel == 0) {
			return;
		}
		double before = scale;
		scale = wheel > 0 ? Math.min(MAX_SCALE, scale * 2.0) : Math.max(MIN_SCALE, scale / 2.0);
		if (scale == before || followPlayer) {
			return;
		}
		// Zoom around the cursor so the block under it stays put.
		double wx = toWorldX(mx, before);
		double wz = toWorldZ(my, before);
		centerX = wx - (mx - this.width / 2.0) / scale;
		centerZ = wz - (my - this.height / 2.0) / scale;
	}

	private void handleDrag(int mx, int my) {
		if (!dragging) {
			return;
		}
		if (!Mouse.isButtonDown(0)) {
			dragging = false;
			return;
		}
		int dx = mx - lastMouseX;
		int dy = my - lastMouseY;
		if (dx != 0 || dy != 0) {
			if (Math.abs(mx - dragStartX) > DRAG_THRESHOLD || Math.abs(my - dragStartY) > DRAG_THRESHOLD) {
				dragMoved = true;
				followPlayer = false;
				followButton.displayString = followLabel();
			}
			if (dragMoved) {
				centerX -= dx / scale;
				centerZ -= dy / scale;
			}
			lastMouseX = mx;
			lastMouseY = my;
		}
	}

	@Override
	public void mouseClicked(int mx, int my, int button) {
		if (my >= this.height - 30) {
			super.mouseClicked(mx, my, button);
			return;
		}
		if (button == 0) {
			dragging = true;
			dragMoved = false;
			dragStartX = mx;
			dragStartY = my;
			lastMouseX = mx;
			lastMouseY = my;
		} else if (button == 1) {
			Waypoint hovered = waypointAt(mx, my);
			if (hovered != null) {
				this.mc.displayScreen(new ScreenWaypointEdit(this, MapManager.INSTANCE.waypoints(), hovered, false));
			} else {
				addWaypointAt(toWorldX(mx), toWorldZ(my));
			}
		}
	}

	@Override
	public void mouseReleased(int mx, int my, int button) {
		super.mouseReleased(mx, my, button);
		if (button == 0 && dragging) {
			dragging = false;
			if (!dragMoved && my < this.height - 30) {
				Waypoint hovered = waypointAt(mx, my);
				if (hovered != null) {
					this.mc.displayScreen(new ScreenWaypointEdit(this, MapManager.INSTANCE.waypoints(), hovered, false));
				}
			}
		}
	}

	@Override
	public void keyPressed(char c, int key, int mx, int my) {
		if (key == Keyboard.KEY_ESCAPE || BtaMapOptions.KEY_OPEN_MAP.isKeyboardKey(key)) {
			this.mc.displayScreen(this.parentScreen);
			return;
		}
		if (BtaMapOptions.KEY_NEW_WAYPOINT.isKeyboardKey(key)) {
			addWaypointAt(toWorldX(mx), toWorldZ(my));
			return;
		}
		super.keyPressed(c, key, mx, my);
	}

	private Waypoint waypointAt(int mx, int my) {
		WaypointStore store = MapManager.INSTANCE.waypoints();
		if (store == null) {
			return null;
		}
		int dimension = MapManager.INSTANCE.dimension();
		for (Waypoint waypoint : store.all()) {
			if (waypoint.dimension == dimension && Math.abs(mx - toScreenX(waypoint.x + 0.5)) <= 4 && Math.abs(my - toScreenY(waypoint.z + 0.5)) <= 4) {
				return waypoint;
			}
		}
		return null;
	}

	private void addWaypointAt(double wx, double wz) {
		WaypointStore store = MapManager.INSTANCE.waypoints();
		if (store == null) {
			return;
		}
		int y = this.mc.thePlayer != null ? (int) Math.floor(this.mc.thePlayer.y) : 64;
		Waypoint waypoint = new Waypoint("", (int) Math.floor(wx), y, (int) Math.floor(wz), MapManager.INSTANCE.dimension(), store.nextColor());
		this.mc.displayScreen(new ScreenWaypointEdit(this, store, waypoint, true));
	}

	private int toScreenX(double wx) {
		return (int) Math.round(this.width / 2.0 + (wx - centerX) * scale);
	}

	private int toScreenY(double wz) {
		return (int) Math.round(this.height / 2.0 + (wz - centerZ) * scale);
	}

	private double toWorldX(int sx) {
		return toWorldX(sx, scale);
	}

	private double toWorldZ(int sy) {
		return toWorldZ(sy, scale);
	}

	private double toWorldX(int sx, double atScale) {
		return centerX + (sx - this.width / 2.0) / atScale;
	}

	private double toWorldZ(int sy, double atScale) {
		return centerZ + (sy - this.height / 2.0) / atScale;
	}
}
