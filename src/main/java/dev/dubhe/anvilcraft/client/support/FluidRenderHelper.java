/*
 * Original Code Copyright (C) 2022 The Create Team
 * Source: https://github.com/Creators-of-Create/Ponder
 *
 * This file is part of "Ponder" project, which is licensed under
 * the MIT License.
 *
 * --- MODIFICATIONS ---
 * This file has been modified for use in AnvilCraft.
 * Modifications made by: QiuShui1012
 * Modification date: 2026/4/24
 * These modifications continue to be licensed under LGPLv3.
 * -------------------------------------------------------------
 */

package dev.dubhe.anvilcraft.client.support;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

public final class FluidRenderHelper {
    public static final FluidRenderHelper INSTANCE = new FluidRenderHelper();

    public void renderFluidBox(
        FluidStack fluid,
        float minX,
        float minY,
        float minZ,
        float maxX,
        float maxY,
        float maxZ,
        MultiBufferSource buffer,
        PoseStack ms,
        int light,
        boolean renderBottom,
        boolean invertGasses
    ) {
        this.renderFluidBox(fluid, minX, minY, minZ, maxX, maxY, maxZ, getFluidBuilder(buffer), ms, light, renderBottom, invertGasses);
    }

    public void renderFluidBox(
        FluidStack fluid,
        float minX,
        float minY,
        float minZ,
        float maxX,
        float maxY,
        float maxZ,
        VertexConsumer builder,
        PoseStack ms,
        int light,
        boolean renderBottom,
        boolean invertGasses
    ) {
        var renderProps = IClientFluidTypeExtensions.of(fluid.getFluid());
        final TextureAtlasSprite fluidTexture = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
            .apply(renderProps.getStillTexture(fluid));
        final int color = renderProps.getTintColor(fluid);

        int blockLightIn = (light >> 4) & 0xF;
        int luminosity = Math.max(blockLightIn, fluid.getFluidType().getLightLevel());
        light = (light & 0xF00000) | luminosity << 4;

        Vec3 center = new Vec3(minX + (maxX - minX) / 2, minY + (maxY - minY) / 2, minZ + (maxZ - minZ) / 2);
        ms.pushPose();
        if (invertGasses && fluid.getFluidType().isLighterThanAir()) {
            ms.translate(center.x, center.y, center.z);
            ms.mulPose(Axis.XP.rotationDegrees(180));
            ms.translate(-center.x, -center.y, -center.z);
        }

        for (Direction side : Direction.values()) {
            if (side == Direction.DOWN && !renderBottom) continue;

            boolean positive = side.getAxisDirection() == Direction.AxisDirection.POSITIVE;
            if (side.getAxis()
                .isHorizontal()) {
                if (side.getAxis() == Direction.Axis.X) {
                    renderStillTiledFace(
                        side, minZ, minY, maxZ, maxY, positive ? maxX : minX,
                        builder, ms, light, color, fluidTexture
                    );
                } else {
                    renderStillTiledFace(
                        side, minX, minY, maxX, maxY, positive ? maxZ : minZ,
                        builder, ms, light, color, fluidTexture
                    );
                }
            } else {
                renderStillTiledFace(
                    side, minX, minZ, maxX, maxZ, positive ? maxY : minY,
                    builder, ms, light, color, fluidTexture
                );
            }
        }

        ms.popPose();
    }

    public static VertexConsumer getFluidBuilder(MultiBufferSource buffer) {
        return buffer.getBuffer(RenderType.TRANSLUCENT);
    }

    public static void renderStillTiledFace(
        Direction dir, float left, float down, float right, float up,
        float depth, VertexConsumer builder, PoseStack ms, int light, int color, TextureAtlasSprite texture
    ) {
        renderTiledFace(dir, left, down, right, up, depth, builder, ms, light, color, texture, 1);
    }

    public static void renderTiledFace(
        Direction dir, float left, float down, float right, float up,
        float depth, VertexConsumer builder, PoseStack ms, int light, int color, TextureAtlasSprite texture,
        float textureScale
    ) {
        boolean positive = dir.getAxisDirection() == Direction.AxisDirection.POSITIVE;
        boolean horizontal = dir.getAxis().isHorizontal();
        boolean x = dir.getAxis() == Direction.Axis.X;

        float shrink = texture.uvShrinkRatio() * 0.25f * textureScale;
        float centerU = texture.getU0() + (texture.getU1() - texture.getU0()) * 0.5f * textureScale;
        float centerV = texture.getV0() + (texture.getV1() - texture.getV0()) * 0.5f * textureScale;

        float f;
        float x2;
        float y2;
        float u1;
        float u2;
        float v1;
        float v2;
        for (float x1 = left; x1 < right; x1 = x2) {
            f = Mth.floor(x1);
            x2 = Math.min(f + 1, right);
            if (dir == Direction.NORTH || dir == Direction.EAST) {
                f = Mth.ceil(x2);
                u1 = texture.getU((f - x2) * textureScale);
                u2 = texture.getU((f - x1) * textureScale);
            } else {
                u1 = texture.getU((x1 - f) * textureScale);
                u2 = texture.getU((x2 - f) * textureScale);
            }
            u1 = Mth.lerp(shrink, u1, centerU);
            u2 = Mth.lerp(shrink, u2, centerU);
            for (float y1 = down; y1 < up; y1 = y2) {
                f = Mth.floor(y1);
                y2 = Math.min(f + 1, up);
                if (dir == Direction.UP) {
                    v1 = texture.getV((y1 - f) * textureScale);
                    v2 = texture.getV((y2 - f) * textureScale);
                } else {
                    f = Mth.ceil(y2);
                    v1 = texture.getV((f - y2) * textureScale);
                    v2 = texture.getV((f - y1) * textureScale);
                }
                v1 = Mth.lerp(shrink, v1, centerV);
                v2 = Mth.lerp(shrink, v2, centerV);

                if (horizontal) {
                    if (x) {
                        putVertex(builder, ms, depth, y2, positive ? x2 : x1, color, u1, v1, dir, light);
                        putVertex(builder, ms, depth, y1, positive ? x2 : x1, color, u1, v2, dir, light);
                        putVertex(builder, ms, depth, y1, positive ? x1 : x2, color, u2, v2, dir, light);
                        putVertex(builder, ms, depth, y2, positive ? x1 : x2, color, u2, v1, dir, light);
                    } else {
                        putVertex(builder, ms, positive ? x1 : x2, y2, depth, color, u1, v1, dir, light);
                        putVertex(builder, ms, positive ? x1 : x2, y1, depth, color, u1, v2, dir, light);
                        putVertex(builder, ms, positive ? x2 : x1, y1, depth, color, u2, v2, dir, light);
                        putVertex(builder, ms, positive ? x2 : x1, y2, depth, color, u2, v1, dir, light);
                    }
                } else {
                    putVertex(builder, ms, x1, depth, positive ? y1 : y2, color, u1, v1, dir, light);
                    putVertex(builder, ms, x1, depth, positive ? y2 : y1, color, u1, v2, dir, light);
                    putVertex(builder, ms, x2, depth, positive ? y2 : y1, color, u2, v2, dir, light);
                    putVertex(builder, ms, x2, depth, positive ? y1 : y2, color, u2, v1, dir, light);
                }
            }
        }
    }

    private static void putVertex(
        VertexConsumer builder, PoseStack ms, float x, float y, float z, int color, float u,
        float v, Direction face, int light
    ) {

        Vec3i normal = face.getNormal();
        PoseStack.Pose peek = ms.last();
        int a = color >> 24 & 0xff;
        int r = color >> 16 & 0xff;
        int g = color >> 8 & 0xff;
        int b = color & 0xff;

        builder.addVertex(peek.pose(), x, y, z)
            .setColor(r, g, b, a)
            .setUv(u, v)
            .setLight(light)
            .setNormal(peek.copy(), normal.getX(), normal.getY(), normal.getZ())
        ;
    }
}

