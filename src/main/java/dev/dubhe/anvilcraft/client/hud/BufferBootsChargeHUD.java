package dev.dubhe.anvilcraft.client.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.dubhe.anvilcraft.client.init.ModShaders;
import dev.dubhe.anvilcraft.util.ColorUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

public final class BufferBootsChargeHUD {
    private static final ResourceLocation BACKGROUND = ResourceLocation.withDefaultNamespace("hud/experience_bar_background");
    private static final ResourceLocation PROGRESS = ResourceLocation.withDefaultNamespace("hud/experience_bar_progress");

    private BufferBootsChargeHUD() {
    }

    public static int color(float progress) {
        return ColorUtil.lerpColor(Math.clamp(progress, 0, 1), 0xFF328CFF, 0xFFFF9500);
    }

    public static void render(GuiGraphics graphics, float progress) {
        float fraction = Math.clamp(progress, 0, 1);
        int left = graphics.guiWidth() / 2 - 91;
        int top = graphics.guiHeight() - 29;
        RenderSystem.enableBlend();
        graphics.blitSprite(BACKGROUND, left, top, 182, 5);
        int width = Math.min(182, (int) (fraction * 183));
        if (width <= 0) {
            RenderSystem.disableBlend();
            return;
        }
        graphics.flush();
        TextureAtlasSprite sprite = Minecraft.getInstance().getGuiSprites().getSprite(PROGRESS);
        Matrix4f pose = graphics.pose().last().pose();
        int tint = color(fraction);
        RenderSystem.setShader(ModShaders::getEquipmentChargeShader);
        RenderSystem.setShaderTexture(0, sprite.atlasLocation());
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        float rightU = sprite.getU(width / 182.0f);
        buffer.addVertex(pose, left, top, 0).setUv(sprite.getU0(), sprite.getV0()).setColor(tint);
        buffer.addVertex(pose, left, top + 5, 0).setUv(sprite.getU0(), sprite.getV1()).setColor(tint);
        buffer.addVertex(pose, left + width, top + 5, 0).setUv(rightU, sprite.getV1()).setColor(tint);
        buffer.addVertex(pose, left + width, top, 0).setUv(rightU, sprite.getV0()).setColor(tint);
        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.disableBlend();
    }
}
