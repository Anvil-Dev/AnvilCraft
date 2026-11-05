package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.HeliostatsBlockEntity;
import dev.dubhe.anvilcraft.client.AnvilCraftClient;
import dev.dubhe.anvilcraft.client.init.ModRenderTypes;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.BiConsumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class HeliostatsBeam {
    static final double MIRROR_PIVOT_Y = 1.3;
    private static final double LENGTH_FRACTION = 0.6;
    private static final float HALF_WIDTH = 0.3F;
    private static final int START_COLOR = 0x19FFF4C2;
    private static final int FADE_SEGMENTS = 8;

    private static final Set<HeliostatsBlockEntity> ACTIVE = Collections.newSetFromMap(new WeakHashMap<>());
    private static final ContextKey<List<Beam>> BEAMS = new ContextKey<>(AnvilCraft.of("heliostats_beams"));

    private HeliostatsBeam() {
    }

    public static void track(HeliostatsBlockEntity mirror) {
        ACTIVE.add(mirror);
    }

    private record Beam(Vec3 start, Vec3 end) {
    }

    @SubscribeEvent
    public static void extract(ExtractLevelRenderStateEvent event) {
        if (!AnvilCraftClient.CONFIG.graphics.heliostatsBeam || ACTIVE.isEmpty()) {
            return;
        }
        List<Beam> beams = new ArrayList<>();
        forEachVisible(event.getLevel(), event.getRenderState().cameraRenderState.pos, event.getFrustum(),
            (start, end) -> beams.add(new Beam(start, end)));
        event.getRenderState().setRenderData(BEAMS, beams);
    }

    @SubscribeEvent
    public static void submit(SubmitCustomGeometryEvent event) {
        if (!AnvilCraftClient.CONFIG.graphics.heliostatsBeam) {
            return;
        }
        var beams = event.getLevelRenderState().getRenderData(BEAMS);
        if (beams == null) {
            return;
        }
        var pose = event.getPoseStack();
        for (Beam beam : beams) {
            pose.pushPose();
            applyPose(pose, event.getLevelRenderState().cameraRenderState.pos, beam.start(), beam.end());
            event.getSubmitNodeCollector().submitCustomGeometry(pose, ModRenderTypes.HELIOSTATS_BEAM, HeliostatsBeam::draw);
            pose.popPose();
        }
    }

    private static void forEachVisible(ClientLevel level, Vec3 camera, Frustum frustum, BiConsumer<Vec3, Vec3> render) {
        double distance = AnvilCraftClient.CONFIG.graphics.heliostatsRenderDistance;
        var mirrors = ACTIVE.iterator();
        while (mirrors.hasNext()) {
            HeliostatsBlockEntity mirror = mirrors.next();
            BlockPos target = mirror.getIrritatePos();
            if (mirror.isRemoved() || mirror.getLevel() != level || !mirror.getWorkResult().isWorking() || target == null) {
                mirrors.remove();
                continue;
            }
            Vec3 start = target.getCenter();
            if (start.distanceToSqr(camera) > distance * distance) {
                continue;
            }
            Vec3 pivot = Vec3.atBottomCenterOf(mirror.getBlockPos()).add(0, MIRROR_PIVOT_Y, 0);
            Vec3 end = start.lerp(pivot, LENGTH_FRACTION);
            if (frustum.isVisible(new AABB(start, end).inflate(HALF_WIDTH * Math.sqrt(2)))) {
                render.accept(start, end);
            }
        }
    }

    private static void applyPose(PoseStack pose, Vec3 camera, Vec3 start, Vec3 end) {
        Vec3 axis = end.subtract(start);
        pose.translate(start.x - camera.x, start.y - camera.y, start.z - camera.z);
        float yaw = (float) Math.atan2(axis.x, axis.z);
        float pitch = (float) -Math.atan2(axis.y, Math.hypot(axis.x, axis.z));
        pose.mulPose(new Quaternionf().rotationYXZ(yaw, pitch, 0));
        pose.scale(1, 1, (float) axis.length());
    }

    private static void draw(PoseStack.Pose pose, VertexConsumer vertices) {
        Matrix4f matrix = pose.pose();
        float r = HALF_WIDTH;
        side(matrix, vertices, -r, -r, r, -r);
        side(matrix, vertices, r, -r, r, r);
        side(matrix, vertices, r, r, -r, r);
        side(matrix, vertices, -r, r, -r, -r);
        vertices.addVertex(matrix, -r, -r, 0).setColor(START_COLOR);
        vertices.addVertex(matrix, -r, r, 0).setColor(START_COLOR);
        vertices.addVertex(matrix, r, r, 0).setColor(START_COLOR);
        vertices.addVertex(matrix, r, -r, 0).setColor(START_COLOR);
    }

    private static void side(Matrix4f matrix, VertexConsumer vertices, float x1, float y1, float x2, float y2) {
        float start = 0;
        int startColor = START_COLOR;
        for (int i = 1; i <= FADE_SEGMENTS; i++) {
            float end = (float) i / FADE_SEGMENTS;
            float remaining = 1 - end;
            int alpha = Math.round((START_COLOR >>> 24) * remaining * remaining);
            int endColor = (alpha << 24) | (START_COLOR & 0xFFFFFF);
            vertices.addVertex(matrix, x1, y1, start).setColor(startColor);
            vertices.addVertex(matrix, x2, y2, start).setColor(startColor);
            vertices.addVertex(matrix, x2, y2, end).setColor(endColor);
            vertices.addVertex(matrix, x1, y1, end).setColor(endColor);
            start = end;
            startColor = endColor;
        }
    }
}
