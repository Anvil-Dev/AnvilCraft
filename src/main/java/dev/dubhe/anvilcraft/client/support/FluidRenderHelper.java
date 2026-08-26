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
import dev.dubhe.anvilcraft.util.LiquidEnchantmentClientFluidTypeExtension;
import dev.dubhe.anvilcraft.util.ModClientFluidTypeExtensionImpl;
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
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.Set;

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
        this.renderFluidBox(fluid, minX, minY, minZ, maxX, maxY, maxZ, buffer, ms, light,
            getSkippedSides(renderBottom), invertGasses);
    }

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
        Set<Direction> skippedSides,
        boolean invertGasses
    ) {
        var renderProps = IClientFluidTypeExtensions.of(fluid.getFluid());
        boolean opaque = (renderProps instanceof ModClientFluidTypeExtensionImpl ext && ext.isOpaque())
            || fluid.is(NeoForgeMod.MILK.value());
        RenderType renderType = opaque ? RenderType.cutout() : RenderType.translucent();
        VertexConsumer builder = buffer.getBuffer(renderType);
        this.renderFluidBox(fluid, minX, minY, minZ, maxX, maxY, maxZ, builder, ms, light,
            skippedSides, invertGasses);
    }

    /** {@link MultiBufferSource} 版、带自定义侧面贴图的重载（用于排水口向下流动水柱）。 */
    public void renderFluidBox(
        FluidStack fluid,
        float minX, float minY, float minZ,
        float maxX, float maxY, float maxZ,
        MultiBufferSource buffer, PoseStack ms, int light,
        boolean renderBottom, boolean invertGasses, TextureAtlasSprite sideTexture
    ) {
        var renderProps = IClientFluidTypeExtensions.of(fluid.getFluid());
        boolean opaque = (renderProps instanceof ModClientFluidTypeExtensionImpl ext && ext.isOpaque())
            || fluid.is(NeoForgeMod.MILK.value());
        RenderType renderType = opaque ? RenderType.cutout() : RenderType.translucent();
        VertexConsumer builder = buffer.getBuffer(renderType);
        this.renderFluidBox(fluid, minX, minY, minZ, maxX, maxY, maxZ, builder, ms, light,
            renderBottom, invertGasses, sideTexture);
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
        this.renderFluidBox(fluid, minX, minY, minZ, maxX, maxY, maxZ, builder, ms, light,
            getSkippedSides(renderBottom), invertGasses);
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
        Set<Direction> skippedSides,
        boolean invertGasses
    ) {
        var renderProps = IClientFluidTypeExtensions.of(fluid.getFluid());
        final TextureAtlasSprite stillTexture = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
            .apply(renderProps.getStillTexture(fluid));
        renderFluidBox(fluid, minX, minY, minZ, maxX, maxY, maxZ, builder, ms, light,
            skippedSides, invertGasses, stillTexture);
    }

    /**
     * 带自定义<b>侧面贴图</b>的流体盒渲染：水平四面用 {@code sideTexture}（如流动贴图，营造向下流动感），
     * 顶/底面用静止贴图。用于排水口向下水柱等需要"流动侧面"的渲染。
     */
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
        boolean invertGasses,
        TextureAtlasSprite sideTexture
    ) {
        renderFluidBox(fluid, minX, minY, minZ, maxX, maxY, maxZ, builder, ms, light,
            getSkippedSides(renderBottom), invertGasses, sideTexture);
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
        Set<Direction> skippedSides,
        boolean invertGasses,
        TextureAtlasSprite sideTexture
    ) {
        var renderProps = IClientFluidTypeExtensions.of(fluid.getFluid());
        final TextureAtlasSprite stillTexture = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
            .apply(renderProps.getStillTexture(fluid));
        final int[] colors = renderProps instanceof LiquidEnchantmentClientFluidTypeExtension extension
            ? extension.getLayerColors(fluid)
            : new int[]{renderProps.getTintColor(fluid)};

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

        for (int color : colors) {
            for (Direction side : Direction.values()) {
                if (skippedSides.contains(side)) continue;

                // 水平面用侧面贴图（流动），上下面用静止贴图
                TextureAtlasSprite tex = side.getAxis().isHorizontal() ? sideTexture : stillTexture;
                boolean positive = side.getAxisDirection() == Direction.AxisDirection.POSITIVE;
                if (side.getAxis().isHorizontal()) {
                    if (side.getAxis() == Direction.Axis.X) {
                        renderStillTiledFace(side, minZ, minY, maxZ, maxY, positive ? maxX : minX,
                            builder, ms, light, color, tex);
                    } else {
                        renderStillTiledFace(side, minX, minY, maxX, maxY, positive ? maxZ : minZ,
                            builder, ms, light, color, tex);
                    }
                } else {
                    renderStillTiledFace(side, minX, minZ, maxX, maxZ, positive ? maxY : minY,
                        builder, ms, light, color, tex);
                }
            }
        }

        ms.popPose();
    }

    /**
     * 带 alpha 缩放的气体渲染（供玻璃管道等使用）：始终以给定完整范围渲染盒体，
     * {@code alphaFill}（0..1）缩放流体 tint 的 alpha，使气体量由透明度传达。
     */
    public void renderFluidBox(
        FluidStack fluid,
        float minX, float minY, float minZ,
        float maxX, float maxY, float maxZ,
        MultiBufferSource buffer, PoseStack ms, int light,
        Set<Direction> skippedSides, float alphaFill
    ) {
        var renderProps = IClientFluidTypeExtensions.of(fluid.getFluid());
        boolean opaque = (renderProps instanceof ModClientFluidTypeExtensionImpl ext && ext.isOpaque())
            || fluid.is(NeoForgeMod.MILK.value());
        RenderType renderType = opaque ? RenderType.cutout() : RenderType.translucent();
        VertexConsumer builder = buffer.getBuffer(renderType);
        var stillTexture = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
            .apply(renderProps.getStillTexture(fluid));
        this.renderFluidBoxWithAlpha(fluid, minX, minY, minZ, maxX, maxY, maxZ, builder, ms, light,
            skippedSides, false, stillTexture, alphaFill);
    }

    /**
     * Gas-fluid variant of {@link #renderFluidBox}: the box is always rendered at the given
     * full extents, and {@code alphaFill} (0..1) scales the fluid tint's alpha so the amount
     * of gas is conveyed by opacity while still filling the whole tank.
     */
    public void renderFluidBox(
        FluidStack fluid,
        float minX, float minY, float minZ,
        float maxX, float maxY, float maxZ,
        MultiBufferSource buffer, PoseStack ms, int light,
        boolean renderBottom, float alphaFill
    ) {
        var renderProps = IClientFluidTypeExtensions.of(fluid.getFluid());
        boolean opaque = (renderProps instanceof ModClientFluidTypeExtensionImpl ext && ext.isOpaque())
            || fluid.is(NeoForgeMod.MILK.value());
        RenderType renderType = opaque ? RenderType.cutout() : RenderType.translucent();
        VertexConsumer builder = buffer.getBuffer(renderType);
        var stillTexture = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
            .apply(renderProps.getStillTexture(fluid));
        this.renderFluidBoxWithAlpha(fluid, minX, minY, minZ, maxX, maxY, maxZ, builder, ms, light,
            getSkippedSides(renderBottom), false, stillTexture, alphaFill);
    }

    private void renderFluidBoxWithAlpha(
        FluidStack fluid,
        float minX, float minY, float minZ,
        float maxX, float maxY, float maxZ,
        VertexConsumer builder, PoseStack ms, int light,
        Set<Direction> skippedSides, boolean invertGasses,
        TextureAtlasSprite sideTexture, float alphaFill
    ) {
        var renderProps = IClientFluidTypeExtensions.of(fluid.getFluid());
        final TextureAtlasSprite stillTexture = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
            .apply(renderProps.getStillTexture(fluid));
        final int[] baseColors = renderProps instanceof LiquidEnchantmentClientFluidTypeExtension extension
            ? extension.getLayerColors(fluid)
            : new int[]{renderProps.getTintColor(fluid)};

        int blockLightIn = (light >> 4) & 0xF;
        int luminosity = Math.max(blockLightIn, fluid.getFluidType().getLightLevel());
        light = (light & 0xF00000) | luminosity << 4;

        int clampedAlpha = (int) (Mth.clamp(alphaFill, 0, 1) * 255);
        int[] colors = new int[baseColors.length];
        for (int i = 0; i < baseColors.length; i++) {
            int base = baseColors[i];
            int a = ((base >> 24) & 0xFF) * clampedAlpha / 255;
            colors[i] = (base & 0x00FFFFFF) | (a << 24);
        }

        Vec3 center = new Vec3(minX + (maxX - minX) / 2, minY + (maxY - minY) / 2, minZ + (maxZ - minZ) / 2);
        ms.pushPose();
        if (invertGasses && fluid.getFluidType().isLighterThanAir()) {
            ms.translate(center.x, center.y, center.z);
            ms.mulPose(Axis.XP.rotationDegrees(180));
            ms.translate(-center.x, -center.y, -center.z);
        }

        for (int color : colors) {
            for (Direction side : Direction.values()) {
                if (skippedSides.contains(side)) continue;
                TextureAtlasSprite tex = side.getAxis().isHorizontal() ? sideTexture : stillTexture;
                boolean positive = side.getAxisDirection() == Direction.AxisDirection.POSITIVE;
                if (side.getAxis().isHorizontal()) {
                    if (side.getAxis() == Direction.Axis.X) {
                        renderStillTiledFace(side, minZ, minY, maxZ, maxY, positive ? maxX : minX,
                            builder, ms, light, color, tex);
                    } else {
                        renderStillTiledFace(side, minX, minY, maxX, maxY, positive ? maxZ : minZ,
                            builder, ms, light, color, tex);
                    }
                } else {
                    renderStillTiledFace(side, minX, minZ, maxX, maxZ, positive ? maxY : minY,
                        builder, ms, light, color, tex);
                }
            }
        }

        ms.popPose();
    }

    private static Set<Direction> getSkippedSides(boolean renderBottom) {
        return renderBottom ? Set.of() : Set.of(Direction.DOWN);
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

