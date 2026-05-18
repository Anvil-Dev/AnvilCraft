package dev.dubhe.anvilcraft.client.gui.component;

import dev.dubhe.anvilcraft.client.gui.screen.TeslaTowerScreen;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class TeslaTowerButton extends Button {

    private final Identifier texture;

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
            var -> parent.getFilterTitle(index, variant).copy()
        );
        this.height = 15;
        this.width = 112;
        this.index = index;
        this.texture = SharedTextures.textureGui("machine/active_silencer/button_%s".formatted(textureVariant));
        this.parent = parent;
        this.variant = variant;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        String searchText = this.parent.getFilterText();
        String id = this.parent.getFilterToolTipAt(this.index, this.variant);
        if (id == null) return;
        this.renderTexture(graphics, this.texture, this.getX(), this.getY(), 0, 0, 15, this.width, this.height, 112, 30);
        Component message;
        if (searchText.startsWith("#") || searchText.startsWith("~")) {
            message = this.parent.getFilterTitle(this.index, this.variant);
        } else {
            message = highlighted(
                this.parent.getFilterTitle(this.index, this.variant).getString(),
                searchText,
                ChatFormatting.WHITE
            );
        }
        this.setMessage(message);
        int color = 16777215 | Mth.ceil(this.alpha * 255.0F) << 24;
        Font font = Minecraft.getInstance().font;
        graphics.text(font, message, this.getX() + 2, this.getY() + 3, color);
        if (this.isHovered()) {
            Component filterText = highlighted(
                id, searchText.replaceFirst("#", ""), ChatFormatting.GRAY);
            List<ClientTooltipComponent> tooltipComponents = filterText.getString().isEmpty()
                ? List.of(ClientTooltipComponent.create(message.getVisualOrderText()))
                : List.of(
                    ClientTooltipComponent.create(message.getVisualOrderText()),
                    ClientTooltipComponent.create(filterText.getVisualOrderText())
                );
            graphics.tooltip(font, tooltipComponents, mouseX, mouseY, DefaultTooltipPositioner.INSTANCE, null);
        }
    }

    private static Component highlighted(
        String original,
        String hightlighted,
        ChatFormatting originalFormatting
    ) {
        try {
            String[] parts = original.split(Pattern.quote(hightlighted), -1);
            List<Component> components = new ArrayList<>();
            for (String s : parts) {
                components.add(Component.literal(s).copy().setStyle(Style.EMPTY.applyFormat(originalFormatting)));
            }
            return ComponentUtils.formatList(
                components, Component.literal(hightlighted).withStyle(ChatFormatting.YELLOW));
        } catch (Throwable e) {
            return Component.literal(original);
        }
    }

    public void renderTexture(
        GuiGraphicsExtractor graphics,
        Identifier texture,
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
        graphics.blit(texture, x, y, puOffset, i, width, height, textureWidth, textureHeight);
    }
}
