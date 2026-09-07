package dev.mja00.btamap.mixin;

import dev.mja00.btamap.BtaMap;
import dev.mja00.btamap.BtaMapInput;
import dev.mja00.btamap.map.MapManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.InputDevice;
import net.minecraft.client.world.WorldClient;
import net.minecraft.core.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class MinecraftMixin {
	@Inject(method = "startGame", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/GameSettings;init(Ljava/io/File;)V"))
	private void btamap$registerSettings(CallbackInfo ci) {
		BtaMap.registerSettings();
	}

	@Inject(method = "runTick", at = @At("TAIL"))
	private void btamap$tick(CallbackInfo ci) {
		MapManager.INSTANCE.tick((Minecraft) (Object) this);
	}

	@Inject(method = "changeWorld(Lnet/minecraft/client/world/WorldClient;Ljava/lang/String;Lnet/minecraft/core/entity/player/Player;)V", at = @At("HEAD"))
	private void btamap$flushOnWorldChange(WorldClient world, String loadingTitle, Player player, CallbackInfo ci) {
		MapManager.INSTANCE.unload();
	}

	@Inject(method = "checkBoundInputs", at = @At("HEAD"), cancellable = true)
	private void btamap$handleKeys(InputDevice device, CallbackInfoReturnable<Boolean> cir) {
		if (BtaMapInput.handle((Minecraft) (Object) this, device)) {
			cir.setReturnValue(true);
		}
	}
}
