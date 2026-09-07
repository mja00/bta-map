package dev.mja00.btamap.mixin;

import dev.mja00.btamap.map.MapManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.net.handler.PacketHandlerClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PacketHandlerClient.class)
public class PacketHandlerClientMixin {
	@Inject(method = "<init>(Lnet/minecraft/client/Minecraft;Ljava/lang/String;I)V", at = @At("RETURN"))
	private void btamap$rememberServer(Minecraft minecraft, String host, int port, CallbackInfo ci) {
		MapManager.INSTANCE.setServerAddress(host + "_" + port);
	}
}
