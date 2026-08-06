package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.api.world.load.ChunkFeatureManager;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BaseSpawner.class)
public class BaseSpawnerMixin {

    @Inject(
        method = "isNearPlayer",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onIsNearPlayer(Level level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        ChunkPos chunkPos = new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4);

        if (!ChunkFeatureManager.isChunkManaged(level.dimension(), chunkPos)) return;

        if (ChunkFeatureManager.shouldAllowSpawnerSpawn(level.dimension(), chunkPos)) {
            cir.setReturnValue(true);
        }
    }
}