package dev.dubhe.anvilcraft.client.gui.component;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.gui.screen.inventory.ActiveSilencerScreen;
import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SilencerButton extends Button {

    private final ResourceLocation texture;
    @Getter
    private final int index;
    private final ActiveSilencerScreen parent;
    private final String variant;

    /**
     * 主动静音器 screen 的按钮
     */
    public SilencerButton(int x, int y, int index, String variant, OnPress onPress, ActiveSilencerScreen parent) {
        super(x,
                y,
                10,
                10,
                Component.literal(""),
                onPress,
                (var) -> Component.literal(variant)
        );
        this.height = 15;
        this.width = 112;
        this.index = index;
        texture = AnvilCraft.of("textures/gui/container/machine/active_silencer_button_%s.png".formatted(variant));
        this.parent = parent;
        this.variant = variant;
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        ResourceLocation soundId = parent.getSoundIdAt(index, variant);
        if (soundId == null) return;
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
        this.setMessage(parent.getSoundTextAt(index, variant));
        this.renderString(guiGraphics, Minecraft.getInstance().font, 16777215 | Mth.ceil(this.alpha * 255.0F) << 24);
        if (this.isHovered()) {
            guiGraphics.renderTooltip(
                    Minecraft.getInstance().font,
                    List.of(
                            this.getMessage()
                                    .copy()
                                    .setStyle(Style.EMPTY.applyFormat(ChatFormatting.WHITE))
                                    .getVisualOrderText(),
                            Component.literal(soundId.toString())
                                    .copy()
                                    .setStyle(Style.EMPTY.applyFormat(ChatFormatting.GRAY))
                                    .getVisualOrderText()
                    ),
                    mouseX,
                    mouseY
            );
        }
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
