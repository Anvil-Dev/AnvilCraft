package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.client.support.PowerGridSupport;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
abstract class ClientLevelMixin {
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
}
