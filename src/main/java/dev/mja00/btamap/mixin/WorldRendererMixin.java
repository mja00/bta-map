package dev.mja00.btamap.mixin;

import dev.mja00.btamap.waypoint.WaypointRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {
	@Shadow
	public Minecraft mc;

	// After the selection outline the view matrices are still the world camera, but no terrain state is live.
	@Inject(method = "renderWorld", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WorldRenderer;renderPrecipitation(F)V"))
	private void btamap$renderWaypoints(float partialTicks, long updateRenderersUntil, CallbackInfo ci) {
		WaypointRenderer.render(this.mc, partialTicks);
	}
}
