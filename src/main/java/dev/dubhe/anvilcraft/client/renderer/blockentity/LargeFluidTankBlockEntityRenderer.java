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
import dev.dubhe.anvilcraft.block.entity.LargeFluidTankBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.FluidTankRenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import org.joml.Matrix4f;

public class LargeFluidTankBlockEntityRenderer implements BlockEntityRenderer<LargeFluidTankBlockEntity> {
    public LargeFluidTankBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public AABB getRenderBoundingBox(LargeFluidTankBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(1, 1, 1);
    }

    @Override
    public void render(
        LargeFluidTankBlockEntity tank, float tickDelta, PoseStack ms, MultiBufferSource vertexConsumers, int light, int overlay) {
        if (tank.getTank().getFluid().isEmpty()) return;
        if (!tank.isMainPart()) return;

        /*
         *
         * // Uncomment to allow the liquid to rotate with the tank ms.pushPose(); ms.translate(0.5, 0.5, 0.5);
         * FacingToRotation.get(tank.getForward(), tank.getUp()).push(ms); ms.translate(-0.5, -0.5, -0.5);
         */
        float fill = (float) tank.getTank().getFluid().getAmount() / tank.getTank().getCapacity();
        if (fill <= 0.025) fill = 0.025f;
        fill = Mth.clamp(fill, 0, 1);

        drawFluidInTank(ms, vertexConsumers, light, tank.getTank().getFluid(), fill);

        // ms.popPose();
    }

    private static final float TANK_W = 4 / 16f + 0.001f; // avoiding Z-fighting

    public static void drawFluidInTank(PoseStack ps, MultiBufferSource mbs, int light, FluidStack fluid, float fill) {
        // From Modern Industrialization
        var renderProps = IClientFluidTypeExtensions.of(fluid.getFluid());
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
            .apply(renderProps.getStillTexture(fluid));

        int color = renderProps.getTintColor(fluid);

        // Top and bottom positions of the fluid inside the tank
        float height = 3 - 2 * TANK_W;

        float minX = TANK_W - 1;
        float minY = TANK_W - 1;
        float minZ = TANK_W - 1;
        float maxX = 2 - TANK_W;
        float maxY = 2 - TANK_W;
        float maxZ = 2 - TANK_W;

        FluidType attributes = fluid.getFluid().getFluidType();
        if (attributes.isLighterThanAir()) {
            minY = maxY - fill * height;
        } else {
            maxY = minY + fill * height;
        }

        // 使用平移复制裁切方式渲染液体材质
        FluidTankRenderUtil.renderFluidCube(ps, mbs, light, sprite, color, minX, minY, minZ, maxX, maxY, maxZ);
    }

    public static class RenderManager {
        protected VertexConsumer consumer;
        protected int light;
        protected Matrix4f pose;
        protected int color;
        protected float minX;
        protected float minY;
        protected float minZ;
        protected float maxX;
        protected float maxY;
        protected float maxZ;
        protected float u0;
        protected float u1;
        protected float v0;
        protected float v1;
        protected float ul;
        protected float vl;

        public RenderManager(
            VertexConsumer consumer,
            int light,
            TextureAtlasSprite sprite,
            Matrix4f pose,
            int color,
            float minX,
            float minY,
            float minZ,
            float maxX,
            float maxY,
            float maxZ
        ) {
            this.consumer = consumer;
            this.light = light;
            this.pose = pose;
            this.u0 = sprite.getU0();
            this.u1 = sprite.getU1();
            this.v0 = sprite.getV0();
            this.v1 = sprite.getV1();
            this.ul = this.u1 - this.u0;
            this.vl = this.v1 - this.v0;
            this.color = color;
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
        }

        public void render() {
            renderSide(Direction.NORTH);
            renderSide(Direction.SOUTH);
            renderSide(Direction.WEST);
            renderSide(Direction.EAST);
            renderSide(Direction.UP);
            renderSide(Direction.DOWN);
        }

        public void renderSide(Direction direction) {
            float[] normal = {
                0,
                0,
                0
            };
            float minSideI;
            float minSideJ;
            float maxSideI;
            float maxSideJ;
            switch (direction) {
                case NORTH -> {
                    normal[2] = -1;
                    minSideI = minX;
                    maxSideI = maxX;
                    minSideJ = minY;
                    maxSideJ = maxY;
                }
                case SOUTH -> {
                    normal[2] = 1;
                    minSideI = minX;
                    maxSideI = maxX;
                    minSideJ = minY;
                    maxSideJ = maxY;
                }
                case WEST -> {
                    normal[0] = -1;
                    minSideI = minY;
                    maxSideI = maxY;
                    minSideJ = minZ;
                    maxSideJ = maxZ;
                }
                case EAST -> {
                    normal[0] = 1;
                    minSideI = minY;
                    maxSideI = maxY;
                    minSideJ = minZ;
                    maxSideJ = maxZ;
                }
                case UP -> {
                    normal[1] = 1;
                    minSideI = minX;
                    maxSideI = maxX;
                    minSideJ = minZ;
                    maxSideJ = maxZ;
                }
                default -> {
                    normal[1] = -1;
                    minSideI = minX;
                    maxSideI = maxX;
                    minSideJ = minZ;
                    maxSideJ = maxZ;
                }
            }
            for (float minI = minSideI; minI < maxSideI; minI += 1) {
                float maxI = Math.min(minI + 1, maxSideI);
                float v0 = this.v0;
                float v1 = this.v0 + (maxI - minI) * this.vl;
                for (float minJ = minSideJ; minJ < maxSideJ; minJ += 1) {
                    float maxJ = Math.min(minJ + 1, maxSideJ);
                    float u0 = this.u0;
                    float u1 = this.u0 + (maxJ - minJ) * this.ul;
                    switch (direction) {
                        case NORTH -> {
                            addVertex(maxI, maxJ, minZ, u0, v0, normal);
                            addVertex(maxI, minJ, minZ, u1, v0, normal);
                            addVertex(minI, minJ, minZ, u1, v1, normal);
                            addVertex(minI, maxJ, minZ, u0, v1, normal);
                        }
                        case SOUTH -> {
                            addVertex(maxI, minJ, maxZ, u0, v0, normal);
                            addVertex(maxI, maxJ, maxZ, u1, v0, normal);
                            addVertex(minI, maxJ, maxZ, u1, v1, normal);
                            addVertex(minI, minJ, maxZ, u0, v1, normal);
                        }
                        case WEST -> {
                            addVertex(minX, maxI, maxJ, u0, v0, normal);
                            addVertex(minX, maxI, minJ, u1, v0, normal);
                            addVertex(minX, minI, minJ, u1, v1, normal);
                            addVertex(minX, minI, maxJ, u0, v1, normal);
                        }
                        case EAST -> {
                            addVertex(maxX, minI, maxJ, u0, v0, normal);
                            addVertex(maxX, minI, minJ, u1, v0, normal);
                            addVertex(maxX, maxI, minJ, u1, v1, normal);
                            addVertex(maxX, maxI, maxJ, u0, v1, normal);
                        }
                        case UP -> {
                            addVertex(maxI, maxY, maxJ, u0, v0, normal);
                            addVertex(maxI, maxY, minJ, u1, v0, normal);
                            addVertex(minI, maxY, minJ, u1, v1, normal);
                            addVertex(minI, maxY, maxJ, u0, v1, normal);
                        }
                        default -> {
                            addVertex(maxI, minY, maxJ, u0, v0, normal);
                            addVertex(maxI, minY, minJ, u1, v0, normal);
                            addVertex(minI, minY, minJ, u1, v1, normal);
                            addVertex(minI, minY, maxJ, u0, v1, normal);
                        }
                    }
                }
            }
        }

        public void addVertex(float x, float y, float z, float u, float v, float[] normal) {
            consumer.addVertex(pose, x, y, z).setColor(color).setUv(u, v).setLight(light).setNormal(normal[0], normal[1], normal[2]);
        }
    }
}
