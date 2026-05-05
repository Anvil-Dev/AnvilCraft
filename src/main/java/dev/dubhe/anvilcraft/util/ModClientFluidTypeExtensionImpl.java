package dev.dubhe.anvilcraft.util;

import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Getter;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import org.joml.Vector3f;

public class ModClientFluidTypeExtensionImpl implements IClientFluidTypeExtensions {
    @Getter
    public final Identifier stillTexture;
    @Getter
    public final Identifier flowingTexture;
    public final boolean noFog;
    public final int fogColor;
    public final float fogDistance;

    public ModClientFluidTypeExtensionImpl(
        Identifier stillTexture,
        Identifier flowingTexture,
        int fogColor,
        float fogDistance
    ) {
        this.stillTexture = stillTexture;
        this.flowingTexture = flowingTexture;
        this.noFog = false;
        this.fogColor = fogColor;
        this.fogDistance = fogDistance;
    }

    public ModClientFluidTypeExtensionImpl(
        Identifier stillTexture,
        Identifier flowingTexture
    ) {
        this.stillTexture = stillTexture;
        this.flowingTexture = flowingTexture;
        this.noFog = true;
        this.fogColor = 0xFF0000;
        this.fogDistance = 96.0F;
    }

    public ModClientFluidTypeExtensionImpl(Identifier texture) {
        this(texture, texture);
    }

    @Override
    public Vector3f modifyFogColor(
        Camera camera,
        float partialTick,
        ClientLevel level,
        int renderDistance,
        float darkenWorldAmount,
        Vector3f fluidFogColor
    ) {
        if (this.noFog) return fluidFogColor;
        float fogRed = ((this.fogColor >> 16) & 255) / 255.0F;
        float fogGreen = ((this.fogColor >> 8) & 255) / 255.0F;
        float fogBlue = (this.fogColor & 255) / 255.0F;
        return new Vector3f(fogRed, fogGreen, fogBlue);
    }

    @Override
    public void modifyFogRender(
        Camera camera,
        FogRenderer.FogMode mode,
        float renderDistance,
        float partialTick,
        float nearDistance,
        float farDistance,
        FogShape shape
    ) {
        if (camera.getEntity().isSpectator() || this.noFog) return;
        RenderSystem.setShaderFogStart(0.0F);
        RenderSystem.setShaderFogEnd(this.fogDistance);
    }
}