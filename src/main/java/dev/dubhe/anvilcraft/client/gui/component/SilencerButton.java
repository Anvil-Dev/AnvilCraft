package dev.dubhe.anvilcraft.client.gui.component;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.dubhe.anvilcraft.client.gui.screen.ActiveSilencerScreen;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class SilencerButton extends Button {
    private final Identifier texture;

    @Getter
    private final int index;

    private final ActiveSilencerScreen parent;
    private final int variant;

    /**
     * 主动静音器 screen 的按钮
     */
    public SilencerButton(
        int x,
        int y,
        int index,
        int variant,
        OnPress onPress,
        ActiveSilencerScreen parent,
        String textureVariant
    ) {
        super(
            x,
            y,
            10,
            10,
            Component.literal(""),
            onPress,
            (var) -> parent.getSoundTextAt(index, variant).copy()
        );
        this.height = 15;
        this.width = 112;
        this.index = index;
        this.texture = SharedTextures.textureGui("machine/active_silencer/button_%s".formatted(textureVariant));
        this.parent = parent;
        this.variant = variant;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        String searchText = this.parent.getFilterText();
        Identifier soundId = this.parent.getSoundIdAt(this.index, this.variant);
        if (soundId == null) return;
        this.renderTexture(guiGraphics, this.texture, this.getX(), this.getY(), 0, 0, 15, this.width, this.height, 112, 30);
        Component message;
        if (searchText.startsWith("#") || searchText.startsWith("~")) {
            message = this.parent.getSoundTextAt(this.index, this.variant);
        } else {
            message = highlighted(
                this.parent.getSoundTextAt(this.index, this.variant).getString(),
                searchText,
                ChatFormatting.WHITE,
                ChatFormatting.YELLOW);
        }
        this.setMessage(message);
        this.renderString(guiGraphics, Minecraft.getInstance().font, 16777215 | Mth.ceil(this.alpha * 255.0F) << 24);
        if (this.isHovered()) {
            Component soundIdText = highlighted(
                soundId.toString(), searchText.replaceFirst("#", ""), ChatFormatting.GRAY, ChatFormatting.YELLOW);
            guiGraphics.renderTooltip(
                Minecraft.getInstance().font,
                List.of(message.getVisualOrderText(), soundIdText.getVisualOrderText()),
                mouseX,
                mouseY
            );
        }
    }

    private static Component highlighted(
        String original,
        String hightlighted,
        ChatFormatting originalFormatting,
        ChatFormatting highlightFormatting
    ) {
        try {
            String[] parts = original.split(Pattern.quote(hightlighted), -1);
            List<Component> components = new ArrayList<>();
            for (String s : parts) {
                components.add(Component.literal(s).copy().setStyle(Style.EMPTY.applyFormat(originalFormatting)));
            }
            return ComponentUtils.formatList(
                components,
                Component.literal(hightlighted).withStyle(highlightFormatting)
            );
        } catch (Throwable e) {
            return Component.literal(original);
        }
    }

    public void renderTexture(
        GuiGraphics guiGraphics,
        Identifier texture,
        int x,
        int y,
        int puOffset,
        int pvOffset,
        int textureDifference,
        int width,
        int height,
        int textureWidth,
        int textureHeight
    ) {
        int i = pvOffset;
        if (this.isHovered()) {
            i += textureDifference;
        }
        RenderSystem.enableDepthTest();
        guiGraphics.blit(texture, x, y, puOffset, i, width, height, textureWidth, textureHeight);
    }
}
