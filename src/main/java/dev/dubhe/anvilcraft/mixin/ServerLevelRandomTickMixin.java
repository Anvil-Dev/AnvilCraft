package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.api.world.load.ChunkFeatureManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public class ServerLevelRandomTickMixin {

    @Unique
    private boolean anvilcraft$isReentering = false;

    @Inject(
        method = "tickChunk(Lnet/minecraft/world/level/chunk/LevelChunk;I)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void anvilcraft$onTickChunk(LevelChunk chunk, int tickSpeed, CallbackInfo ci) {
        if (anvilcraft$isReentering || tickSpeed <= 0) {
            return;
        }

        ChunkPos pos = chunk.getPos();
        // noinspection ConstantConditions
        ServerLevel self = (ServerLevel) (Object) this;
        if (ChunkFeatureManager.isChunkManaged(self.dimension(), pos)
            && ChunkFeatureManager.shouldSkipRandomTick(self.dimension(), pos)) {

            ci.cancel();

            this.anvilcraft$isReentering = true;
            try {
                // noinspection ConstantConditions
                ((ServerLevel) (Object) this).tickChunk(chunk, 0);
            } finally {
                this.anvilcraft$isReentering = false;
            }
        }
    }
}