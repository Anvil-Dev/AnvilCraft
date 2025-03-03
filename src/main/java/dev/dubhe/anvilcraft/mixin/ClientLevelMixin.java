package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.api.chargecollector.HeatedBlockRecorder;
import dev.dubhe.anvilcraft.client.PowerGridClient;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

@Mixin(ClientLevel.class)
abstract class ClientLevelMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    void onLevelLoad(
        ClientPacketListener connection,
        ClientLevel.ClientLevelData clientLevelData,
        ResourceKey<?> dimension,
        Holder<?> dimensionType,
        int viewDistance,
        int serverSimulationDistance,
        Supplier<?> profiler,
        LevelRenderer levelRenderer,
        boolean isDebug,
        long biomeZoomSeed,
        CallbackInfo ci) {
        PowerGridClient.clearAllGrid();
        HeatedBlockRecorder.clear();
    }
}
