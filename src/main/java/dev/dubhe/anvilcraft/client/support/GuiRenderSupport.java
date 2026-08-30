package dev.dubhe.anvilcraft.client.support;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.GuiSpriteManager;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.gui.GuiSpriteScaling;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import org.joml.Matrix4f;

public class GuiRenderSupport {
    public static void centeredEllipsisText(GuiGraphics graphics, Font font, Component text, int x, int y, int max) {
        GuiRenderSupport.centeredEllipsisText(graphics, font, text, x, y, max, 0xFFFFFFFF);
    }

    public static void centeredEllipsisText(GuiGraphics graphics, Font font, Component text, int x, int y, int max, int color) {
        int width = font.width(text);
        if (width < max) { // 小于最大宽度，需要居中
            graphics.drawCenteredString(font, text, x + Math.floorDiv(max, 2), y, color);
        } else if (width > max) { // 大于最大宽度，需要截断
            FormattedText truncated = FormattedText.composite(
                font.substrByWidth(text, max - font.width(CommonComponents.ELLIPSIS)),
                CommonComponents.ELLIPSIS
            );
            graphics.drawString(font, Language.getInstance().getVisualOrder(truncated), x, y, color, true);
        } else { // 等于最大宽度，直接渲染
            graphics.drawString(font, text, x, y, color, true);
        }
    }

    public static void blitSprite(GuiGraphics graphics, ResourceLocation sprite, int x, int y, int width, int height, int color) {
        GuiRenderSupport.blitSprite(graphics, sprite, x, y, 0, width, height, color);
    }

    public static void blitSprite(
        GuiGraphics graphics,
        ResourceLocation spriteLocation,
        int x,
        int y,
        int blitOffset,
        int width,
        int height,
        int color
    ) {
        GuiSpriteManager sprites = Minecraft.getInstance().getGuiSprites();
        TextureAtlasSprite sprite = sprites.getSprite(spriteLocation);
        GuiSpriteScaling guispritescaling = sprites.getSpriteScaling(sprite);
        switch (guispritescaling) {
            case GuiSpriteScaling.Stretch ignored -> GuiRenderSupport.blitSprite(graphics, sprite, x, y, blitOffset, width, height, color);
            case GuiSpriteScaling.Tile(int width1, int height1) -> GuiRenderSupport.blitTiledSprite(
                graphics,
                sprite,
                x,
                y,
                blitOffset,
                width,
                height,
                0,
                0,
                width1,
                height1,
                width1,
                height1,
                color
            );
            case GuiSpriteScaling.NineSlice nineSlice -> GuiRenderSupport.blitNineSlicedSprite(
                graphics,
                sprite,
                nineSlice,
                x,
                y,
                blitOffset,
                width,
                height,
                color
            );
            default -> {
            }
        }
    }
    
    public static void blitSprite(
        GuiGraphics graphics,
        TextureAtlasSprite sprite,
        int textureWidth,
        int textureHeight,
        int posU,
        int posV,
        int x,
        int y,
        int blitOffset,
        int widthU,
        int heightV,
        int color
    ) {
        if (widthU != 0 && heightV != 0) {
            GuiRenderSupport.innerBlit(
                graphics,
                sprite.atlasLocation(),
                x,
                x + widthU,
                y,
                y + heightV,
                blitOffset,
                sprite.getU((float) posU / (float) textureWidth),
                sprite.getU((float) (posU + widthU) / (float) textureWidth),
                sprite.getV((float) posV / (float) textureHeight),
                sprite.getV((float) (posV + heightV) / (float) textureHeight),
                color
            );
        }
    }

    public static void blitSprite(
        GuiGraphics graphics,
        TextureAtlasSprite sprite,
        int x,
        int y,
        int blitOffset,
        int width,
        int height,
        int color
    ) {
        if (width != 0 && height != 0) {
            GuiRenderSupport.innerBlit(
                graphics,
                sprite.atlasLocation(),
                x,
                x + width,
                y,
                y + height,
                blitOffset,
                sprite.getU0(),
                sprite.getU1(),
                sprite.getV0(),
                sprite.getV1(),
                color
            );
        }
    }

    private static void blitTiledSprite(
        GuiGraphics graphics,
        TextureAtlasSprite sprite,
        int x,
        int y,
        int blitOffset,
        int width,
        int height,
        int posU,
        int posV,
        int spriteWidth,
        int spriteHeight,
        int nineSliceWidth,
        int nineSliceHeight,
        int color
    ) {
        if (width > 0 && height > 0) {
            if (spriteWidth > 0 && spriteHeight > 0) {
                for (int curX = 0; curX < width; curX += spriteWidth) {
                    int curWidth = Math.min(spriteWidth, width - curX);

                    for (int curY = 0; curY < height; curY += spriteHeight) {
                        int curHeight = Math.min(spriteHeight, height - curY);
                        GuiRenderSupport.blitSprite(
                            graphics,
                            sprite,
                            nineSliceWidth,
                            nineSliceHeight,
                            posU,
                            posV,
                            x + curX,
                            y + curY,
                            blitOffset,
                            curWidth,
                            curHeight,
                            color
                        );
                    }
                }
            } else {
                throw new IllegalArgumentException("Tiled sprite texture size must be positive, got " + spriteWidth + "x" + spriteHeight);
            }
        }
    }
    
    private static void blitNineSlicedSprite(
        GuiGraphics graphics,
        TextureAtlasSprite sprite,
        GuiSpriteScaling.NineSlice nineSlice,
        int x,
        int y,
        int blitOffset,
        int width,
        int height,
        int color
    ) {
        GuiSpriteScaling.NineSlice.Border border = nineSlice.border();
        int left = Math.min(border.left(), width / 2);
        int right = Math.min(border.right(), width / 2);
        int top = Math.min(border.top(), height / 2);
        int bottom = Math.min(border.bottom(), height / 2);
        if (width == nineSlice.width() && height == nineSlice.height()) {
            GuiRenderSupport.blitSprite(
                graphics,
                sprite,
                nineSlice.width(),
                nineSlice.height(),
                0,
                0,
                x,
                y,
                blitOffset,
                width,
                height,
                color
            );
        } else if (height == nineSlice.height()) {
            GuiRenderSupport.blitSprite(
                graphics,
                sprite,
                nineSlice.width(),
                nineSlice.height(),
                0,
                0,
                x,
                y,
                blitOffset,
                left,
                height,
                color
            );
            GuiRenderSupport.blitTiledSprite(
                graphics,
                sprite,
                x + left,
                y,
                blitOffset,
                width - right - left,
                height,
                left,
                0,
                nineSlice.width() - right - left,
                nineSlice.height(),
                nineSlice.width(),
                nineSlice.height(),
                color
            );
            GuiRenderSupport.blitSprite(
                graphics,
                sprite,
                nineSlice.width(),
                nineSlice.height(),
                nineSlice.width() - right,
                0,
                x + width - right,
                y,
                blitOffset,
                right,
                height,
                color
            );
        } else if (width == nineSlice.width()) {
            GuiRenderSupport.blitSprite(
                graphics,
                sprite,
                nineSlice.width(),
                nineSlice.height(),
                0,
                0,
                x,
                y,
                blitOffset,
                width,
                top,
                color
            );
            GuiRenderSupport.blitTiledSprite(
                graphics,
                sprite,
                x,
                y + top,
                blitOffset,
                width,
                height - bottom - top,
                0,
                top,
                nineSlice.width(),
                nineSlice.height() - bottom - top,
                nineSlice.width(),
                nineSlice.height(),
                color
            );
            GuiRenderSupport.blitSprite(
                graphics,
                sprite,
                nineSlice.width(),
                nineSlice.height(),
                0,
                nineSlice.height() - bottom,
                x,
                y + height - bottom,
                blitOffset,
                width,
                bottom,
                color
            );
        } else {
            GuiRenderSupport.blitSprite(
                graphics,
                sprite,
                nineSlice.width(),
                nineSlice.height(),
                0,
                0,
                x,
                y,
                blitOffset,
                left,
                top,
                color
            );
            GuiRenderSupport.blitTiledSprite(
                graphics,
                sprite,
                x + left,
                y,
                blitOffset,
                width - right - left,
                top,
                left,
                0,
                nineSlice.width() - right - left,
                top,
                nineSlice.width(),
                nineSlice.height(),
                color
            );
            GuiRenderSupport.blitSprite(
                graphics,
                sprite,
                nineSlice.width(),
                nineSlice.height(),
                nineSlice.width() - right,
                0,
                x + width - right,
                y,
                blitOffset,
                right,
                top,
                color
            );
            GuiRenderSupport.blitSprite(
                graphics,
                sprite,
                nineSlice.width(),
                nineSlice.height(),
                0,
                nineSlice.height() - bottom,
                x,
                y + height - bottom,
                blitOffset,
                left,
                bottom,
                color
            );
            GuiRenderSupport.blitTiledSprite(
                graphics,
                sprite,
                x + left,
                y + height - bottom,
                blitOffset,
                width - right - left,
                bottom,
                left,
                nineSlice.height() - bottom,
                nineSlice.width() - right - left,
                bottom,
                nineSlice.width(),
                nineSlice.height(),
                color
            );
            GuiRenderSupport.blitSprite(
                graphics,
                sprite,
                nineSlice.width(),
                nineSlice.height(),
                nineSlice.width() - right,
                nineSlice.height() - bottom,
                x + width - right,
                y + height - bottom,
                blitOffset,
                right,
                bottom,
                color
            );
            GuiRenderSupport.blitTiledSprite(
                graphics,
                sprite,
                x,
                y + top,
                blitOffset,
                left,
                height - bottom - top,
                0,
                top,
                left,
                nineSlice.height() - bottom - top,
                nineSlice.width(),
                nineSlice.height(),
                color
            );
            GuiRenderSupport.blitTiledSprite(
                graphics,
                sprite,
                x + left,
                y + top,
                blitOffset,
                width - right - left,
                height - bottom - top,
                left,
                top,
                nineSlice.width() - right - left,
                nineSlice.height() - bottom - top,
                nineSlice.width(),
                nineSlice.height(),
                color
            );
            GuiRenderSupport.blitTiledSprite(
                graphics,
                sprite,
                x + width - right,
                y + top,
                blitOffset,
                left,
                height - bottom - top,
                nineSlice.width() - right,
                top,
                right,
                nineSlice.height() - bottom - top,
                nineSlice.width(),
                nineSlice.height(),
                color
            );
        }
    }

    private static void innerBlit(
        GuiGraphics graphics,
        ResourceLocation atlasLocation,
        int x1,
        int x2,
        int y1,
        int y2,
        int blitOffset,
        float minU,
        float maxU,
        float minV,
        float maxV,
        int color
    ) {
        
        GuiRenderSupport.innerBlit(
            graphics,
            atlasLocation,
            x1,
            x2,
            y1,
            y2,
            blitOffset,
            minU,
            maxU,
            minV,
            maxV,
            FastColor.ARGB32.red(color) / 255F,
            FastColor.ARGB32.green(color) / 255F,
            FastColor.ARGB32.blue(color) / 255F,
            FastColor.ARGB32.alpha(color) / 255F
        );
    }
    
    private static void innerBlit(
        GuiGraphics graphics,
        ResourceLocation atlasLocation,
        int x1,
        int x2,
        int y1,
        int y2,
        int blitOffset,
        float minU,
        float maxU,
        float minV,
        float maxV,
        float red,
        float green,
        float blue,
        float alpha
    ) {
        RenderSystem.setShaderTexture(0, atlasLocation);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.enableBlend();
        Matrix4f matrix4f = graphics.pose().last().pose();
        BufferBuilder bufferbuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        bufferbuilder.addVertex(matrix4f, (float) x1, (float) y1, (float) blitOffset)
            .setUv(minU, minV)
            .setColor(red, green, blue, alpha);
        bufferbuilder.addVertex(matrix4f, (float) x1, (float) y2, (float) blitOffset)
            .setUv(minU, maxV)
            .setColor(red, green, blue, alpha);
        bufferbuilder.addVertex(matrix4f, (float) x2, (float) y2, (float) blitOffset)
            .setUv(maxU, maxV)
            .setColor(red, green, blue, alpha);
        bufferbuilder.addVertex(matrix4f, (float) x2, (float) y1, (float) blitOffset)
            .setUv(maxU, minV)
            .setColor(red, green, blue, alpha);
        BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());
        RenderSystem.disableBlend();
    }
}
