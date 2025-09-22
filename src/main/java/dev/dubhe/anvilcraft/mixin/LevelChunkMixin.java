package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.api.rendering.pipeline.cached.CacheableBERenderingPipeline;
import dev.dubhe.anvilcraft.block.entity.BaseLaserBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(LevelChunk.class)
public abstract class LevelChunkMixin {
    @Shadow
    @Nullable
    public abstract BlockEntity getBlockEntity(BlockPos pos);

    @Inject(method = "removeBlockEntity", at = @At("HEAD"))
    void onBlockEntityRemoved(BlockPos pos, CallbackInfo ci) {
        BlockEntity be = getBlockEntity(pos);
        if (be instanceof BaseLaserBlockEntity laserStateAccess) {
            CacheableBERenderingPipeline.getInstance().blockRemoved(laserStateAccess);
        }
    }
}
