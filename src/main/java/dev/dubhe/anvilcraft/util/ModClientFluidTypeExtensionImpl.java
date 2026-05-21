package dev.dubhe.anvilcraft.util;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.environment.FogEnvironment;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;

public class ModClientFluidTypeExtensionImpl implements IClientFluidTypeExtensions {
    public final boolean noFog;
    public final int fogColor;
    public final float fogDistance;

    public ModClientFluidTypeExtensionImpl(
        int fogColor,
        float fogDistance
    ) {
        this.noFog = false;
        this.fogColor = fogColor;
        this.fogDistance = fogDistance;
    }

    public ModClientFluidTypeExtensionImpl() {
        this.noFog = true;
        this.fogColor = 0xFF0000;
        this.fogDistance = 96.0F;
    }

    @Override
    public void modifyFogColor(
        Camera camera,
        float partialTick,
        ClientLevel level,
        int renderDistance,
        float darkenWorldAmount,
        Vector4f fluidFogColor
    ) {
        if (this.noFog) return;
        float fogRed = ((this.fogColor >> 16) & 255) / 255.0F;
        float fogGreen = ((this.fogColor >> 8) & 255) / 255.0F;
        float fogBlue = (this.fogColor & 255) / 255.0F;
        fluidFogColor.set(fogRed, fogGreen, fogBlue);
    }

    @Override
    public void modifyFogRender(
        Camera camera,
        @Nullable FogEnvironment environment,
        float renderDistance,
        float partialTick,
        FogData fogData
    ) {
        if (camera.entity().isSpectator() || this.noFog) return;
        fogData.renderDistanceStart = 0;
        fogData.renderDistanceEnd = this.fogDistance;
    }
}
