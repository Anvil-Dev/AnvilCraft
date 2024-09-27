package dev.dubhe.anvilcraft.client.renderer;

import dev.dubhe.anvilcraft.api.power.SimplePowerGrid;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class PowerGridRenderer {
    private static final Map<Integer, SimplePowerGrid> GRID_MAP = Collections.synchronizedMap(new HashMap<>());

    public static Map<Integer, SimplePowerGrid> getGridMap() {
        return PowerGridRenderer.GRID_MAP;
    }

    /**
     * 渲染
     */
    public static void render(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, Vec3 camera) {
        if (Minecraft.getInstance().level == null) return;
        RandomSource random = Minecraft.getInstance().level.random;
        String level = Minecraft.getInstance().level.dimension().location().toString();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lines());
        for (SimplePowerGrid grid : PowerGridRenderer.GRID_MAP.values()) {
            if (!grid.shouldRender(camera)) return;
            if (!grid.getLevel().equals(level)) continue;
            random.setSeed(grid.getHash());
            PowerGridRenderer.renderOutline(
                    poseStack,
                    consumer,
                    camera,
                    grid.getPos(),
                    grid.getCachedOutlineShape(),
                    random.nextFloat(),
                    random.nextFloat(),
                    random.nextFloat(),
                    0.4f
            );
        }
    }

    public static void renderTransmitterLine(
            PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, Vec3 camera) {
        if (Minecraft.getInstance().level == null) return;
        String level = Minecraft.getInstance().level.dimension().location().toString();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lines());
        for (SimplePowerGrid grid : PowerGridRenderer.GRID_MAP.values()) {
            if (!grid.shouldRender(camera)) return;
            if (!grid.getLevel().equals(level)) continue;
            grid.getPowerTransmitterLines().forEach(it -> it.render(poseStack, consumer, camera, 0x9966ccff));
        }
    }

    public static void clearAllGrid() {
        GRID_MAP.clear();
    }

    @SuppressWarnings("SameParameterValue")
    private static void renderOutline(
            PoseStack poseStack,
            VertexConsumer consumer,
            Vec3 camera,
            @NotNull BlockPos pos,
            @NotNull VoxelShape shape,
            float red,
            float green,
            float blue,
            float alpha) {
        PowerGridRenderer.renderShape(
                poseStack,
                consumer,
                shape,
                (double) pos.getX() - camera.x,
                (double) pos.getY() - camera.y,
                (double) pos.getZ() - camera.z,
                red,
                green,
                blue,
                alpha
        );
    }

    private static void renderShape(
            @NotNull PoseStack poseStack,
            VertexConsumer consumer,
            @NotNull VoxelShape shape,
            double x,
            double y,
            double z,
            float red,
            float green,
            float blue,
            float alpha) {
        PoseStack.Pose pose = poseStack.last();
        shape.forAllEdges((minX, minY, minZ, maxX, maxY, maxZ) -> {
            float dx = (float) (maxX - minX);
            float dy = (float) (maxY - minY);
            float dz = (float) (maxZ - minZ);
            float distance = Mth.sqrt(dx * dx + dy * dy + dz * dz);
            consumer.addVertex(pose.pose(), (float) (minX + x), (float) (minY + y), (float) (minZ + z))
                    .setColor(red, green, blue, alpha)
                    .setNormal(pose.copy(), dx /= distance, dy /= distance, dz /= distance);
            consumer.addVertex(pose.pose(), (float) (maxX + x), (float) (maxY + y), (float) (maxZ + z))
                    .setColor(red, green, blue, alpha)
                    .setNormal(pose.copy(), dx, dy, dz);
        });
    }
}
