/*
 * Original Code Copyright (C) 2013 - 2020 AlgorithmX2 et al
 * Source: https://github.com/AppliedEnergistics/Applied-Energistics-2
 *
 * This file is part of "Applied Energistics 2" project, which is licensed under
 * the GNU Lesser General Public License Version 3 (LGPLv3).
 *
 * --- MODIFICATIONS ---
 * This file has been modified for use in AnvilCraft.
 * Modifications made by: TB_pig
 * Modification date: 2026/2/12
 * These modifications continue to be licensed under LGPLv3.
 * -------------------------------------------------------------
 */
package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.dubhe.anvilcraft.block.entity.FluidTankBlockEntity;
import dev.dubhe.anvilcraft.block.entity.LargeFluidTankBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import org.joml.Matrix4f;

public class LargeFluidTankBlockEntityRenderer implements BlockEntityRenderer<LargeFluidTankBlockEntity> {
    public LargeFluidTankBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }


    @Override
    public void render(
        LargeFluidTankBlockEntity tank, float tickDelta, PoseStack ms, MultiBufferSource vertexConsumers, int light, int overlay) {
        if (!tank.getTank().getFluid().isEmpty()) {

            /*
             *
             * // Uncomment to allow the liquid to rotate with the tank ms.pushPose(); ms.translate(0.5, 0.5, 0.5);
             * FacingToRotation.get(tank.getForward(), tank.getUp()).push(ms); ms.translate(-0.5, -0.5, -0.5);
             */

            drawFluidInTank(
                tank, ms, vertexConsumers, light, tank.getTank().getFluid(),
                (float) tank.getTank().getFluid().getAmount() / tank.getTank().getCapacity()
            );

            // ms.popPose();
        }
    }

    private static final float TANK_W = 1 / 16f + 0.001f; // avoiding Z-fighting

    public static void drawFluidInTank(
        BlockEntity be, PoseStack ms, MultiBufferSource vcp, int light, FluidStack fluid,
        float fill
    ) {
        drawFluidInTank(be.getLevel(), be.getBlockPos(), ms, vcp, light, fluid, fill);
    }

    public static void drawFluidInTank(
        Level level, BlockPos pos, PoseStack ps, MultiBufferSource mbs, int light,
        FluidStack fluid, float fill
    ) {
        // From Modern Industrialization
        var renderProps = IClientFluidTypeExtensions.of(fluid.getFluid());
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
            .apply(renderProps.getStillTexture(fluid));

        int color = renderProps.getTintColor(fluid);
        float fillY = Mth.lerp(Mth.clamp(fill, 0, 1), TANK_W, 1 - TANK_W);

        // Top and bottom positions of the fluid inside the tank
        float topHeight = fillY;
        float bottomHeight = TANK_W;

        // Render gas from top to bottom
        FluidType attributes = fluid.getFluid().getFluidType();
        if (attributes.isLighterThanAir()) {
            topHeight = 1 - TANK_W;
            bottomHeight = 1 - fillY;
        }

        float minX = TANK_W * 3 - 1;
        float minZ = TANK_W * 3 - 1;
        float maxX = (1 - TANK_W) * 3 - 1;
        float maxZ = (1 - TANK_W) * 3 - 1;
        float minY = bottomHeight * 3 - 1;
        float maxY = topHeight * 3 - 1;

        // 在 render 方法中
        ps.pushPose();

        // 获取顶点构建器
        VertexConsumer consumer = mbs.getBuffer(RenderType.translucent());

        // 纹理映射：简单将整个 sprite 映射到每个面，不进行平铺
        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();

        Matrix4f pose = ps.last().pose();

        consumer.addVertex(pose, maxX, maxY, maxZ).setColor(color).setUv(u0, v0).setLight(light).setNormal(0, 1, 0);
        consumer.addVertex(pose, maxX, maxY, minZ).setColor(color).setUv(u1, v0).setLight(light).setNormal(0, 1, 0);
        consumer.addVertex(pose, minX, maxY, minZ).setColor(color).setUv(u1, v1).setLight(light).setNormal(0, 1, 0);
        consumer.addVertex(pose, minX, maxY, maxZ).setColor(color).setUv(u0, v1).setLight(light).setNormal(0, 1, 0);

        // 北面 (Z-)
        consumer.addVertex(pose, maxX, maxY, minZ).setColor(color).setUv(u0, v0).setLight(light).setNormal(0, 0, -1);
        consumer.addVertex(pose, maxX, minY, minZ).setColor(color).setUv(u1, v0).setLight(light).setNormal(0, 0, -1);
        consumer.addVertex(pose, minX, minY, minZ).setColor(color).setUv(u1, v1).setLight(light).setNormal(0, 0, -1);
        consumer.addVertex(pose, minX, maxY, minZ).setColor(color).setUv(u0, v1).setLight(light).setNormal(0, 0, -1);

        // 南面 (Z+)
        consumer.addVertex(pose, maxX, minY, maxZ).setColor(color).setUv(u0, v0).setLight(light).setNormal(0, 0, 1);
        consumer.addVertex(pose, maxX, maxY, maxZ).setColor(color).setUv(u1, v0).setLight(light).setNormal(0, 0, 1);
        consumer.addVertex(pose, minX, maxY, maxZ).setColor(color).setUv(u1, v1).setLight(light).setNormal(0, 0, 1);
        consumer.addVertex(pose, minX, minY, maxZ).setColor(color).setUv(u0, v1).setLight(light).setNormal(0, 0, 1);

        // 西面 (X-)
        consumer.addVertex(pose, minX, maxY, maxZ).setColor(color).setUv(u0, v0).setLight(light).setNormal(-1, 0, 0);
        consumer.addVertex(pose, minX, maxY, minZ).setColor(color).setUv(u1, v0).setLight(light).setNormal(-1, 0, 0);
        consumer.addVertex(pose, minX, minY, minZ).setColor(color).setUv(u1, v1).setLight(light).setNormal(-1, 0, 0);
        consumer.addVertex(pose, minX, minY, maxZ).setColor(color).setUv(u0, v1).setLight(light).setNormal(-1, 0, 0);

        // 东面 (X+)
        consumer.addVertex(pose, maxX, minY, maxZ).setColor(color).setUv(u0, v0).setLight(light).setNormal(1, 0, 0);
        consumer.addVertex(pose, maxX, minY, minZ).setColor(color).setUv(u1, v0).setLight(light).setNormal(1, 0, 0);
        consumer.addVertex(pose, maxX, maxY, minZ).setColor(color).setUv(u1, v1).setLight(light).setNormal(1, 0, 0);
        consumer.addVertex(pose, maxX, maxY, maxZ).setColor(color).setUv(u0, v1).setLight(light).setNormal(1, 0, 0);

        // 底面 (Y-)
        consumer.addVertex(pose, maxX, minY, maxZ).setColor(color).setUv(u0, v0).setLight(light).setNormal(0, -1, 0);
        consumer.addVertex(pose, minX, minY, maxZ).setColor(color).setUv(u1, v0).setLight(light).setNormal(0, -1, 0);
        consumer.addVertex(pose, minX, minY, minZ).setColor(color).setUv(u1, v1).setLight(light).setNormal(0, -1, 0);
        consumer.addVertex(pose, maxX, minY, minZ).setColor(color).setUv(u0, v1).setLight(light).setNormal(0, -1, 0);
        ps.popPose();
    }

}
