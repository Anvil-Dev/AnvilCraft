package dev.dubhe.anvilcraft.client.gui.component;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.dubhe.anvilcraft.AnvilCraft;
import lombok.Getter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class SilencerButton extends Button {

    private final ResourceLocation texture;
    @Getter
    private final int index;

    /**
     * 主动静音器 screen 的按钮
     */
    public SilencerButton(int x, int y, int index, String variant, OnPress onPress) {
        super(x,
                y,
                10,
                10,
                Component.literal(""),
                onPress,
                (var) -> Component.literal(variant)
        );
        this.index = index;
        texture = AnvilCraft.of("textures/gui/container/machine/active_silencer_%s.png".formatted(variant));
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderTexture(
                guiGraphics,
                texture,
                this.getX(),
                this.getY(),
                0,
                0,
                15,
                this.width,
                this.height,
                112,
                30
        );
    }

    @Override
    public void renderTexture(@NotNull GuiGraphics guiGraphics, @NotNull ResourceLocation texture,
                              int x, int y, int puOffset, int pvOffset, int textureDifference,
                              int width, int height, int textureWidth, int textureHeight) {
        int i = pvOffset;
        if (this.isHovered()) {
            i += textureDifference;
        }
        RenderSystem.enableDepthTest();
        guiGraphics.blit(texture, x, y, puOffset, i, width, height, textureWidth, textureHeight);
    }
}
