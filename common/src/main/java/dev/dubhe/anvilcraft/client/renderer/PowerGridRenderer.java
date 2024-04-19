package dev.dubhe.anvilcraft.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import lombok.Getter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Environment(EnvType.CLIENT)
public class PowerGridRenderer {
    @Getter
    private static Map<Integer, PowerGrid.SimplePowerGrid> grids = Collections.synchronizedMap(new HashMap<>());

    /**
     * 渲染
     */
    public static void render(PoseStack poseStack, VertexConsumer consumer, double camX, double camY, double camZ) {
        if (Minecraft.getInstance().level == null) return;
        RandomSource random = Minecraft.getInstance().level.random;
        String level = Minecraft.getInstance().level.dimension().location().toString();
        for (PowerGrid.SimplePowerGrid grid : PowerGridRenderer.grids.values()) {
            if (!grid.getLevel().equals(level)) continue;
            random.setSeed(grid.getHash());
            PowerGridRenderer.renderOutline(
                poseStack, consumer, camX, camY, camZ,
                grid.getPos(), grid.getShape(),
                random.nextFloat(), random.nextFloat(), random.nextFloat(), 0.4f
            );
        }
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
