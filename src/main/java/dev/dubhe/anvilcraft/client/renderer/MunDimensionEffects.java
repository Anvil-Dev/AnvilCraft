package dev.dubhe.anvilcraft.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.client.support.MunClientSky;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import javax.annotation.Nullable;

public final class MunDimensionEffects extends DimensionSpecialEffects {
    public MunDimensionEffects() {
        super(Float.NaN, false, SkyType.NONE, false, false);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
        return Vec3.ZERO;
    }

    @Override
    public boolean isFoggyAt(int x, int y) {
        return false;
    }

    @Override
    public @Nullable float[] getSunriseColor(float timeOfDay, float partialTicks) {
        return null;
    }

    @Override
    public boolean renderSky(
        ClientLevel level, int ticks, float partialTick, Matrix4f modelViewMatrix,
        Camera camera, Matrix4f projectionMatrix, boolean isFoggy, Runnable setupFog
    ) {
        MunSkyRenderer.render(level, partialTick, modelViewMatrix, camera, projectionMatrix);
        return true;
    }

    @Override
    public boolean renderClouds(
        ClientLevel level, int ticks, float partialTick, PoseStack poseStack,
        double camX, double camY, double camZ, Matrix4f modelViewMatrix, Matrix4f projectionMatrix
    ) {
        return true;
    }

    @Override
    public boolean renderSnowAndRain(
        ClientLevel level, int ticks, float partialTick, LightTexture lightTexture, double camX, double camY, double camZ
    ) {
        return true;
    }

    @Override
    public boolean tickRain(ClientLevel level, int ticks, Camera camera) {
        return true;
    }

    @Override
    public void adjustLightmapColors(
        ClientLevel level, float partialTicks, float skyDarken, float blockLightRedFlicker,
        float skyLight, int pixelX, int pixelY, Vector3f colors
    ) {
        if (!MunSurfaceRenderer.isLightingEnabled()) return;
        float block = LightTexture.getBrightness(level.dimensionType(), pixelX) * blockLightRedFlicker;
        colors.set(block, block * ((block * 0.6F + 0.4F) * 0.6F + 0.4F), block * (block * block * 0.6F + 0.4F));
        colors.add(0.26F, 0.28F, 0.30F);
        if (pixelY == 15) colors.add(MunClientSky.sunlight(level), MunClientSky.sunlight(level), MunClientSky.sunlight(level));
    }
}
