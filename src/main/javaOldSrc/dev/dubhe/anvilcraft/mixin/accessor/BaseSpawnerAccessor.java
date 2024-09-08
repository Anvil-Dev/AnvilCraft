package dev.dubhe.anvilcraft.mixin.accessor;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SpawnData;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * {@link BaseSpawner}访问器
 */
@Mixin(BaseSpawner.class)
public interface BaseSpawnerAccessor {
    @Invoker("getOrCreateNextSpawnData")
    SpawnData invoker$getOrCreateNextSpawnData(@Nullable Level level, RandomSource random, BlockPos pos);

    @Accessor
    int getSpawnCount();

    @Accessor
    int getMaxNearbyEntities();

    @Accessor
    int getSpawnRange();
}
