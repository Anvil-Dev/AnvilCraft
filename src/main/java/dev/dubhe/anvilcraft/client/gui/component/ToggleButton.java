package dev.dubhe.anvilcraft.client.gui.component;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.Collections;
import java.util.List;

public class ToggleButton extends Button {
    private Identifier texture;
    private final int textureWidth;
    private final int textureHeight;
    private final int texYDiff;
    private List<Component> tooltips;
    private boolean selected;

    public ToggleButton(
        int x,
        int y,
        int width,
        int height,
        Identifier texture,
        int textureWidth,
        int textureHeight,
        OnPress onPress,
        List<Component> tooltips
    ) {
        super(x, y, width, height, Component.empty(), onPress, DEFAULT_NARRATION);
        this.texture = texture;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.texYDiff = textureHeight;
        this.tooltips = tooltips;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isSelected() {
        return this.selected;
    }

    public void setTexture(Identifier texture) {
        this.texture = texture;
    }

    public void setTooltips(List<Component> tooltips) {
        this.tooltips = tooltips;
    }

    public List<Component> getTooltips() {
        return this.tooltips;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        if (!this.visible) return;
        this.isHovered = this.isMouseOver(mouseX, mouseY);
        int offsetV = 0;
        if (this.isHovered) {
            offsetV = this.texYDiff;
        }
        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            this.texture,
            this.getX(),
            this.getY(),
            0,
            offsetV,
            this.width,
            this.height,
            this.textureWidth,
            this.textureHeight
        );
        if (this.isHovered && !this.tooltips.isEmpty()) {
            graphics.setTooltipForNextFrame(
                this.tooltips.stream().map(Component::getVisualOrderText).toList(),
                mouseX,
                mouseY
            );
        }
    }
}
