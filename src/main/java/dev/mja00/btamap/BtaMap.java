package dev.mja00.btamap;

import dev.mja00.btamap.hud.HudComponentMinimap;
import dev.mja00.btamap.map.BlockColors;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.hud.component.ComponentAnchor;
import net.minecraft.client.gui.hud.component.HudComponents;
import net.minecraft.client.gui.hud.component.layout.LayoutAbsolute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import turniplabs.halplibe.HalpLibe;
import turniplabs.halplibe.event.defs.ClientEvents;
import turniplabs.halplibe.util.dependency.Key;

public class BtaMap implements ClientModInitializer {
	public static final String MOD_ID = HalpLibe.registerMod("btamap", true);
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitializeClient() {
		ClientEvents.BLOCK_MODEL_RELOAD.listen(Key.of(MOD_ID), dispatcher -> BlockColors.invalidate());
		ClientEvents.BLOCK_COLOR_RELOAD.listen(Key.of(MOD_ID), dispatcher -> BlockColors.invalidate());
		LOGGER.info("BTA Map initialized");
	}

	/**
	 * Called from startGame right before GameSettings.init: items already exist (HudComponents' static init builds
	 * ItemStacks), and options registered now still load from options.txt and join the default HUD layout.
	 */
	public static void registerSettings() {
		BtaMapOptions.register();
		HudComponents.register(new HudComponentMinimap("btamap_minimap", new LayoutAbsolute(1.0F, 0.0F, ComponentAnchor.TOP_RIGHT, -4, 4)));
	}
}
