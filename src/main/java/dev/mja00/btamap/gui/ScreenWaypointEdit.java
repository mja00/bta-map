package dev.mja00.btamap.gui;

import dev.mja00.btamap.waypoint.Waypoint;
import dev.mja00.btamap.waypoint.WaypointStore;
import net.minecraft.client.gui.ButtonElement;
import net.minecraft.client.gui.Screen;
import net.minecraft.client.gui.TextFieldElement;
import net.minecraft.core.lang.I18n;
import org.lwjgl.input.Keyboard;

public class ScreenWaypointEdit extends Screen {
	private final WaypointStore store;
	private final Waypoint waypoint;
	private final boolean isNew;

	private TextFieldElement nameField;
	private TextFieldElement xField;
	private TextFieldElement yField;
	private TextFieldElement zField;
	private ButtonElement visibleButton;
	private int colorIndex;

	public ScreenWaypointEdit(Screen parent, WaypointStore store, Waypoint waypoint, boolean isNew) {
		super(parent);
		this.store = store;
		this.waypoint = waypoint;
		this.isNew = isNew;
		for (int i = 0; i < Waypoint.PALETTE.length; i++) {
			if (Waypoint.PALETTE[i] == waypoint.color) {
				colorIndex = i;
			}
		}
	}

	@Override
	public void init() {
		I18n i18n = I18n.getInstance();
		Keyboard.enableRepeatEvents(true);
		int left = this.width / 2 - 100;
		int top = this.height / 2 - 70;

		nameField = new TextFieldElement(this, this.fontRenderer, left, top + 12, 200, 20, waypoint.name, i18n.translateKey("gui.btamap.waypoint.namePlaceholder"));
		nameField.setMaxStringLength(32);
		nameField.setFocused(true);
		xField = new TextFieldElement(this, this.fontRenderer, left, top + 48, 62, 20, Integer.toString(waypoint.x), "X");
		yField = new TextFieldElement(this, this.fontRenderer, left + 69, top + 48, 62, 20, Integer.toString(waypoint.y), "Y");
		zField = new TextFieldElement(this, this.fontRenderer, left + 138, top + 48, 62, 20, Integer.toString(waypoint.z), "Z");
		xField.setMaxStringLength(9);
		yField.setMaxStringLength(9);
		zField.setMaxStringLength(9);

		this.add(new ButtonElement(0, left, top + 74, 98, 20, i18n.translateKey("gui.btamap.waypoint.color"))).setListener(button -> {
			colorIndex = (colorIndex + 1) % Waypoint.PALETTE.length;
		});
		visibleButton = this.add(new ButtonElement(1, left + 102, top + 74, 98, 20, visibleLabel(waypoint.visible)));
		visibleButton.setListener(button -> {
			waypoint.visible = !waypoint.visible;
			button.displayString = visibleLabel(waypoint.visible);
		});

		this.add(new ButtonElement(2, left, top + 104, 98, 20, i18n.translateKey("gui.btamap.waypoint.save"))).setListener(button -> save());
		ButtonElement deleteButton = this.add(new ButtonElement(3, left + 102, top + 104, 98, 20,
			i18n.translateKey(isNew ? "gui.btamap.waypoint.cancel" : "gui.btamap.waypoint.delete")));
		deleteButton.setListener(button -> {
			if (!isNew) {
				store.remove(waypoint);
				store.save();
			}
			close();
		});
	}

	private static String visibleLabel(boolean visible) {
		return I18n.getInstance().translateKey(visible ? "gui.btamap.waypoint.visible" : "gui.btamap.waypoint.hidden");
	}

	@Override
	public void removed() {
		Keyboard.enableRepeatEvents(false);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void tick() {
		nameField.updateCursorCounter();
		xField.updateCursorCounter();
		yField.updateCursorCounter();
		zField.updateCursorCounter();
	}

	@Override
	public void render(int mx, int my, float partialTick) {
		this.renderBackground();
		I18n i18n = I18n.getInstance();
		int left = this.width / 2 - 100;
		int top = this.height / 2 - 70;
		this.drawStringCenteredShadow(this.fontRenderer, i18n.translateKey(isNew ? "gui.btamap.waypoint.titleNew" : "gui.btamap.waypoint.titleEdit"), this.width / 2, top - 14, 0xFFFFFFFF);
		this.drawStringShadow(this.fontRenderer, i18n.translateKey("gui.btamap.waypoint.name"), left, top + 2, 0xFFA0A0A0);
		this.drawStringShadow(this.fontRenderer, i18n.translateKey("gui.btamap.waypoint.position"), left, top + 38, 0xFFA0A0A0);
		nameField.drawTextBox();
		xField.drawTextBox();
		yField.drawTextBox();
		zField.drawTextBox();
		super.render(mx, my, partialTick);
		// Drawn after the buttons so the swatch sits on top of the color button.
		int swatch = Waypoint.PALETTE[colorIndex];
		this.drawRect(left + 80, top + 78, left + 94, top + 90, 0xFF000000);
		this.drawRect(left + 81, top + 79, left + 93, top + 89, swatch);
	}

	@Override
	public void keyPressed(char c, int key, int mx, int my) {
		if (key == Keyboard.KEY_RETURN) {
			save();
			return;
		}
		if (key == Keyboard.KEY_ESCAPE) {
			close();
			return;
		}
		nameField.textboxKeyTyped(c, key);
		xField.textboxKeyTyped(c, key);
		yField.textboxKeyTyped(c, key);
		zField.textboxKeyTyped(c, key);
	}

	@Override
	public void mouseClicked(int mx, int my, int button) {
		super.mouseClicked(mx, my, button);
		nameField.mouseClicked(mx, my, button);
		xField.mouseClicked(mx, my, button);
		yField.mouseClicked(mx, my, button);
		zField.mouseClicked(mx, my, button);
	}

	private void save() {
		waypoint.name = nameField.getText().trim();
		waypoint.x = parseOr(xField.getText(), waypoint.x);
		waypoint.y = parseOr(yField.getText(), waypoint.y);
		waypoint.z = parseOr(zField.getText(), waypoint.z);
		waypoint.color = Waypoint.PALETTE[colorIndex];
		if (isNew) {
			store.add(waypoint);
		} else {
			store.markDirty();
		}
		store.save();
		close();
	}

	private static int parseOr(String text, int fallback) {
		try {
			return Integer.parseInt(text.trim());
		} catch (NumberFormatException e) {
			return fallback;
		}
	}

	private void close() {
		this.mc.displayScreen(this.parentScreen);
	}
}
