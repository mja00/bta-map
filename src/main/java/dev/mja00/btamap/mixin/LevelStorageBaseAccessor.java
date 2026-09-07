package dev.mja00.btamap.mixin;

import net.minecraft.core.world.save.LevelStorageBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LevelStorageBase.class)
public interface LevelStorageBaseAccessor {
	@Accessor("worldDirName")
	String btamap$getWorldDirName();
}
