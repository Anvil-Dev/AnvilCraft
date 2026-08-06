package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.api.world.load.ChunkFeatureManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLevel.class)
public class ServerLevelFireSpreadMixin {

    @Inject(
        method = "canSpreadFireAround",
        at = @At("RETURN"),
        cancellable = true
    )
    private void onCanSpreadFireAround(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) {
            return;
        }

        ChunkPos chunkPos = new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4);
        // noinspection ConstantConditions
        ServerLevel self = (ServerLevel) (Object) this;
        if (ChunkFeatureManager.isChunkManaged(self.dimension(), chunkPos) &&
            ChunkFeatureManager.shouldAllowFireSpread(self.dimension(), chunkPos)) {
            cir.setReturnValue(true);
        }
    }
}