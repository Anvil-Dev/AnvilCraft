package dev.dubhe.anvilcraft.integration.iris;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.client.renderer.MunRenderPipeline;
import dev.dubhe.anvilcraft.client.renderer.MunSkyRenderer;
import dev.dubhe.anvilcraft.client.support.MunClientSky;
import dev.dubhe.anvilcraft.worldgen.MunSkyMath;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.Objects;

/** 向光影包提供月球的天体方向与阴影视角，并在最终合成后绘制真空天空。 */
public final class MunIrisCompat {
    private MunIrisCompat() {
    }

    public static boolean isEnabled() {
        return MunRenderPipeline.enabled() && MunClientSky.isMun();
    }

    public static Vector4f celestialPosition(boolean sun, boolean cameraSpace) {
        ClientLevel level = Objects.requireNonNull(Minecraft.getInstance().level);
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        double partialTime = MunClientSky.partialDayTime(level, CapturedRenderingState.INSTANCE.getTickDelta());
        MunSkyMath.Rotation rotation = MunSkyMath.skyRotation(camera.x, camera.z, level.getDayTime(), partialTime);
        MunSkyMath.Vector direction = rotation.apply(sun ? MunSkyMath.referenceSun(level.getDayTime(), partialTime) : MunSkyMath.UP);
        Vector4f position = new Vector4f((float) direction.x() * 100, (float) direction.y() * 100, (float) direction.z() * 100, 0);
        if (cameraSpace) CapturedRenderingState.INSTANCE.getGbufferModelView().transform(position);
        return position;
    }

    public static float sunAngle() {
        ClientLevel level = Objects.requireNonNull(Minecraft.getInstance().level);
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        double partialTime = MunClientSky.partialDayTime(level, CapturedRenderingState.INSTANCE.getTickDelta());
        MunSkyMath.Rotation rotation = MunSkyMath.skyRotation(camera.x, camera.z, level.getDayTime(), partialTime);
        MunSkyMath.Vector sun = MunSkyMath.referenceSun(level.getDayTime(), partialTime);
        double height = rotation.apply(sun).y();
        double derivative = rotation.apply(new MunSkyMath.Vector(sun.y(), -sun.x(), 0)).y();
        double turns = Math.atan2(-derivative, height) / (Math.PI * 2) + 0.25;
        return (float) (turns - Math.floor(turns));
    }

    public static void shadowView(PoseStack stack) {
        Vector4f sun = celestialPosition(true, false).normalize3();
        Matrix4f view = new Matrix4f().lookAt(
            0, 0, 0, -sun.x, -sun.y, -sun.z, 0, Math.abs(sun.y) > 0.99 ? 0 : 1, Math.abs(sun.y) > 0.99 ? 1 : 0
        );
        stack.last().pose().set(view);
        stack.last().normal().set(view);
    }

    public static void renderSky() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || !MunClientSky.isMun()) return;
        minecraft.getMainRenderTarget().bindWrite(true);
        MunSkyRenderer.renderBackground(
            level, CapturedRenderingState.INSTANCE.getTickDelta(), new Matrix4f(CapturedRenderingState.INSTANCE.getGbufferModelView()),
            minecraft.gameRenderer.getMainCamera(), new Matrix4f(CapturedRenderingState.INSTANCE.getGbufferProjection())
        );
    }
}
