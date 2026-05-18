package dev.dubhe.anvilcraft.client.gui.component;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.function.Consumer;

/**
 * 两状态切换按钮:默认、悬停
 * 贴图垂直排列:默认(上)、悬停(下)
 * 点击后切换到另一张贴图
 */
public class ToggleButton extends Button {
    @Setter
    private ResourceLocation texture;
    private final int texWidth;
    private final int texHeight;
    @Getter
    @Setter
    private boolean selected = false;
    private final Consumer<ToggleButton> onPress;
    @Getter
    @Setter
    private List<Component> tooltips;
    
    public ToggleButton(
        int x, int y, int width, int height,
        ResourceLocation texture,
        int texWidth, int texHeight,
        Consumer<ToggleButton> onPress,
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
        
        // 计算Y轴偏移:默认=0, 悬停=texHeight
        int yOffset = this.isHovered ? this.texHeight : 0;
        
        guiGraphics.blit(texture, this.getX(), this.getY(), 0, yOffset, this.width, this.height, this.texWidth, this.texHeight * 2);
        
        // 渲染tooltip，确保在所有元素上方（包括预览窗口提示文本）
        if (this.isHovered && tooltips != null && !tooltips.isEmpty()) {
            guiGraphics.pose().pushPose();
            // 将Z轴向前移动，确保tooltip在所有按钮和预览窗口提示文本上方
            guiGraphics.pose().translate(0, 0, 1500);
            guiGraphics.renderTooltip(
                Minecraft.getInstance().font, 
                tooltips, 
                java.util.Optional.empty(), 
                mouseX, 
                mouseY
            );
            guiGraphics.pose().popPose();
        }
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        if (onPress != null) {
            onPress.accept(this);
        }
    }
}
