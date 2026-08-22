package dev.dubhe.anvilcraft.mixin.accessor;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.Services;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MinecraftServer.class)
public interface MinecraftServerAccessor {
    @Accessor
    Services getServices();

    @Accessor("storageSource")
    LevelStorageSource.LevelStorageAccess getStorageSource();
}
