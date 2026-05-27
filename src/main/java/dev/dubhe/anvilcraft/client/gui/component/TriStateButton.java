package dev.dubhe.anvilcraft.client.gui.component;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.function.Consumer;

/**
 * 三层状态按钮:默认、悬停、选中
 * 贴图垂直排列:默认(上)、悬停(中)、选中(下)
 */
public class TriStateButton extends Button {
    @Setter
    private ResourceLocation texture;
    private final int texWidth;
    private final int texHeight;
    @Getter
    @Setter
    private boolean selected = false;
    private final Consumer<TriStateButton> onPress;
    @Getter
    @Setter
    private List<Component> tooltips;

    public TriStateButton(
        int x, int y, int width, int height,
        ResourceLocation texture,
        int texWidth, int texHeight,
        Consumer<TriStateButton> onPress,
        List<Component> tooltips
    ) {
        super(x, y, width, height, Component.empty(), btn -> {}, DEFAULT_NARRATION);
        this.texture = texture;
        this.texWidth = texWidth;
        this.texHeight = texHeight;
        this.onPress = onPress;
        this.tooltips = tooltips;
    }

    @SuppressWarnings("checkstyle:LocalVariableName")
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!this.visible) return;
        
        this.isHovered = this.isMouseOver(mouseX, mouseY);
        
        // 计算Y轴偏移:默认=0, 悬停=texHeight, 选中=2*texHeight
        int yOffset = 0;
        if (this.selected) {
            yOffset = 2 * this.texHeight;
        } else if (this.isHovered) {
            yOffset = this.texHeight;
        }
        
        guiGraphics.blit(texture, this.getX(), this.getY(), 0, yOffset, this.width, this.height, this.texWidth, this.texHeight * 3);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        if (onPress != null) {
            onPress.accept(this);
        }
    }
}
