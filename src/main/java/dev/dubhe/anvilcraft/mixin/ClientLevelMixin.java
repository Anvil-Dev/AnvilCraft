package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.client.support.PowerGridSupport;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
abstract class ClientLevelMixin implements LevelReader {

    @Inject(method = "<init>", at = @At("RETURN"))
    void onLevelLoad(
        ClientPacketListener connection,
        ClientLevel.ClientLevelData levelData,
        ResourceKey<Level> dimension,
        Holder<DimensionType> dimensionType,
        int serverChunkRadius,
        int serverSimulationDistance,
        LevelRenderer levelRenderer,
        boolean isDebug,
        long biomeZoomSeed,
        int seaLevel,
        CallbackInfo ci
    ) {
        PowerGridSupport.clearAllGrid();
    }

    @Inject(method = "addBreakingBlockEffect(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;"
        + "Lnet/minecraft/world/phys/HitResult;)V", at = @At("HEAD"), cancellable = true)
    private void cancelHitEffectForEmptyBlock(BlockPos pos, Direction direction, HitResult hitResult, CallbackInfo ci) {
        if (this.getBlockState(pos).getShape(this, pos).isEmpty()) {
            ci.cancel();
        }
    }
}
