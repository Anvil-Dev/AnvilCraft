package dev.dubhe.anvilcraft.client.gui.component;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.gui.screen.TeslaTowerScreen;
import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class TeslaTowerButton extends Button {

    private final ResourceLocation texture;

    @Getter
    private final int index;

    private final TeslaTowerScreen parent;
    private final int variant;

    /**
     * 主动静音器 screen 的按钮
     */
    public TeslaTowerButton(
        int x,
        int y,
        int index,
        int variant,
        OnPress onPress,
        TeslaTowerScreen parent,
        String textureVariant
    ) {
        super(
            x,
            y,
            10,
            10,
            Component.literal(""),
            onPress,
            (var) -> parent.getFilterTitle(index, variant).copy()
        );
        this.height = 15;
        this.width = 112;
        this.index = index;
        texture = AnvilCraft.of("textures/gui/container/machine/active_silencer_button_%s.png".formatted(textureVariant));
        this.parent = parent;
        this.variant = variant;
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        String searchText = parent.getFilterText();
        String id = parent.getFilterToolTipAt(index, variant);
        if (id == null) return;
        this.renderTexture(guiGraphics, texture, this.getX(), this.getY(), 0, 0, 15, this.width, this.height, 112, 30);
        Component message;
        if (searchText.startsWith("#") || searchText.startsWith("~")) {
            message = parent.getFilterTitle(index, variant);
        } else {
            message = highlighted(
                parent.getFilterTitle(index, variant).getString(),
                searchText,
                ChatFormatting.WHITE
            );
        }
        this.setMessage(message);
        this.renderString(guiGraphics, Minecraft.getInstance().font, 16777215 | Mth.ceil(this.alpha * 255.0F) << 24);
        if (this.isHovered()) {
            Component filterText = highlighted(
                    id, searchText.replaceFirst("#", ""), ChatFormatting.GRAY);
            guiGraphics.renderTooltip(
                Minecraft.getInstance().font,
                filterText.getString().isEmpty() ?  List.of(message.getVisualOrderText()) : List.of(message.getVisualOrderText(), filterText.getVisualOrderText()),
                mouseX,
                mouseY);
        }
    }

    private static Component highlighted(
        String original,
        String hightlighted,
        ChatFormatting originalFormatting
    ) {
        String[] parts = original.split(hightlighted);
        List<Component> components = new ArrayList<>();
        for (String s : parts) {
            components.add(Component.literal(s).copy().setStyle(Style.EMPTY.applyFormat(originalFormatting)));
        }
        return ComponentUtils.formatList(
            components, Component.literal(hightlighted).withStyle(ChatFormatting.YELLOW));
    }

    public void renderTexture(
        @NotNull GuiGraphics guiGraphics,
        @NotNull ResourceLocation texture,
        int x,
        int y,
        int puOffset,
        int pvOffset,
        int textureDifference,
        int width,
        int height,
        int textureWidth,
        int textureHeight) {
        int i = pvOffset;
        if (this.isHovered()) {
            i += textureDifference;
        }
        RenderSystem.enableDepthTest();
        guiGraphics.blit(texture, x, y, puOffset, i, width, height, textureWidth, textureHeight);
    }
}
