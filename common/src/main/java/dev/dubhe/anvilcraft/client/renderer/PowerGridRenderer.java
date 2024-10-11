package dev.dubhe.anvilcraft.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.power.SimplePowerGrid;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Environment(EnvType.CLIENT)
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
            if (!grid.shouldRender(camera)) continue;
            if (!grid.getLevel().equals(level)) continue;
            random.setSeed(grid.getHash());
            PowerGridRenderer.renderOutline(
                poseStack,
                consumer,
                camera.x,
                camera.y,
                camera.z,
                grid.getPos(),
                grid.getCachedOutlineShape(),
                random.nextFloat(),
                random.nextFloat(),
                random.nextFloat(),
                0.4f
            );
        }
    }

    /**
     * 渲染电线杆连接线
     */
    public static void renderTransmitterLine(
        PoseStack poseStack,
        MultiBufferSource.BufferSource bufferSource,
        Vec3 camera
    ) {
        if (!AnvilCraft.config.renderPowerTransmitterLines) return;
        if (Minecraft.getInstance().level == null) return;
        String level = Minecraft.getInstance().level.dimension().location().toString();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lines());
        for (SimplePowerGrid grid : PowerGridRenderer.GRID_MAP.values()) {
            if (!grid.shouldRender(camera)) continue;
            if (!grid.getLevel().equals(level)) continue;
            grid.getPowerTransmitterLines().forEach(it -> it.render(poseStack, consumer, camera, 0x9966ccff));
        }
        bufferSource.endLastBatch();
    }

    public static void clearAllGrid() {
        GRID_MAP.clear();
    }

    @SuppressWarnings("SameParameterValue")
    private static void renderOutline(
        PoseStack poseStack, VertexConsumer consumer,
        double camX, double camY, double camZ, @NotNull BlockPos pos, @NotNull VoxelShape shape,
        float red, float green, float blue, float alpha
    ) {
        PowerGridRenderer.renderShape(
            poseStack, consumer, shape,
            (double) pos.getX() - camX, (double) pos.getY() - camY, (double) pos.getZ() - camZ,
            red, green, blue, alpha
        );
    }

    private static void renderShape(
        @NotNull PoseStack poseStack, VertexConsumer consumer, @NotNull VoxelShape shape,
        double x, double y, double z, float red, float green, float blue, float alpha
    ) {
        PoseStack.Pose pose = poseStack.last();
        shape.forAllEdges((minX, minY, minZ, maxX, maxY, maxZ) -> {
            float k = (float) (maxX - minX);
            float l = (float) (maxY - minY);
            float m = (float) (maxZ - minZ);
            float n = Mth.sqrt(k * k + l * l + m * m);
            consumer.vertex(pose.pose(), (float) (minX + x), (float) (minY + y), (float) (minZ + z))
                .color(red, green, blue, alpha)
                .normal(pose.normal(), k /= n, l /= n, m /= n)
                .endVertex();
            consumer.vertex(pose.pose(), (float) (maxX + x), (float) (maxY + y), (float) (maxZ + z))
                .color(red, green, blue, alpha)
                .normal(pose.normal(), k, l, m)
                .endVertex();
        });
    }
}
