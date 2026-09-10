package dev.dubhe.anvilcraft.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.support.MunClientSky;
import dev.dubhe.anvilcraft.worldgen.MunSkyMath;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import javax.annotation.Nullable;

public final class MunSkyRenderer {
    private static final ResourceLocation EARTH_TEXTURE = AnvilCraft.of("textures/block/celestial_body/planet_overworld.png");
    private static final ResourceLocation SUN_TEXTURE = ResourceLocation.withDefaultNamespace("textures/environment/sun.png");
    private static @Nullable ShaderInstance shader;

    private MunSkyRenderer() {
    }

    public static void registerShaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(
            new ShaderInstance(event.getResourceProvider(), AnvilCraft.of("mun_sky"), DefaultVertexFormat.POSITION),
            instance -> shader = instance
        );
    }

    public static void render(ClientLevel level, float partialTick, Matrix4f view, Camera camera, Matrix4f projection) {
        drawSky(level, partialTick, view, camera, projection, false);
    }

    public static void renderBackground(ClientLevel level, float partialTick, Matrix4f view, Camera camera, Matrix4f projection) {
        drawSky(level, partialTick, view, camera, projection, true);
    }

    private static void drawSky(
        ClientLevel level, float partialTick, Matrix4f view, Camera camera, Matrix4f projection, boolean backgroundOnly
    ) {
        ShaderInstance skyShader = shader;
        if (skyShader == null || camera.getFluidInCamera() != FogType.NONE) return;
        if (camera.getEntity() instanceof LivingEntity living
            && (living.hasEffect(MobEffects.BLINDNESS) || living.hasEffect(MobEffects.DARKNESS))) return;
        Vec3 position = camera.getPosition();
        long dayTime = level.getDayTime();
        double partialTime = MunClientSky.partialDayTime(level, partialTick);
        MunSkyMath.Rotation rotation = MunSkyMath.skyRotation(position.x, position.z, dayTime, partialTime);
        MunSkyMath.Vector sun = MunSkyMath.referenceSun(dayTime, partialTime);
        skyShader.safeGetUniform("InverseProjection").set(new Matrix4f(projection).invert());
        skyShader.safeGetUniform("InverseView").set(new Matrix4f(view).invert());
        skyShader.safeGetUniform("SkyRotation").set(rotationMatrix(rotation));
        skyShader.safeGetUniform("EarthRotation").set(
            rotationMatrix(MunSkyMath.EARTH_ROTATION).mul(rotationMatrix(MunSkyMath.earthSpin(dayTime, partialTime)))
        );
        skyShader.safeGetUniform("SunDirection").set((float) sun.x(), (float) sun.y(), (float) sun.z());
        skyShader.safeGetUniform("EarthHalfSize").set((float) MunSkyMath.EARTH_HALF_SIZE);
        skyShader.safeGetUniform("AtmosphereThickness").set((float) MunSkyMath.EARTH_ATMOSPHERE_THICKNESS);
        skyShader.safeGetUniform("SunHalfSize").set((float) MunSkyMath.SUN_HALF_SIZE);
        skyShader.safeGetUniform("Daylight").set(MunClientSky.sunlight(level));
        RenderSystem.setShaderTexture(0, EARTH_TEXTURE);
        RenderSystem.setShaderTexture(1, SUN_TEXTURE);
        RenderSystem.setShader(() -> skyShader);
        RenderSystem.disableBlend();
        RenderSystem.depthMask(false);
        if (backgroundOnly) {
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
        } else {
            RenderSystem.disableDepthTest();
        }
        try {
            drawScreenQuad();
        } finally {
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
        }
    }

    public static Matrix4f rotationMatrix(MunSkyMath.Rotation rotation) {
        MunSkyMath.Vector axis = rotation.axis();
        return new Matrix4f().rotation((float) rotation.angle(), (float) axis.x(), (float) axis.y(), (float) axis.z());
    }

    public static void drawScreenQuad() {
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        buffer.addVertex(-1, -1, 0);
        buffer.addVertex(1, -1, 0);
        buffer.addVertex(1, 1, 0);
        buffer.addVertex(-1, 1, 0);
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }
}
