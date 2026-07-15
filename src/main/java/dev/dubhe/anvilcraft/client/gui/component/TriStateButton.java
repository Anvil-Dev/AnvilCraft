package dev.dubhe.anvilcraft.client.gui.component;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;

public class TriStateButton extends Button {
    private final OnPress onPress;
    @Setter
    private Identifier texture;
    private final int textureWidth;
    private final int textureHeight;
    @Getter
    @Setter
    private List<Component> tooltips;
    @Getter
    @Setter
    protected boolean selected;
    private final int[] stateOffset;

    // StateOffset: 0:未选中 1:按下 2: 悬停
    public TriStateButton(
        int x,
        int y,
        int width,
        int height,
        Identifier texture,
        int textureWidth,
        int textureHeight,
        int[] stateOffset,
        OnPress onPress,
        List<Component> tooltips
    ) {
        super(x, y, width, height, Component.empty(), _ -> {}, Button.DEFAULT_NARRATION);
        this.onPress = onPress;
        this.stateOffset = stateOffset;
        this.texture = texture;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.tooltips = tooltips;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        if (!this.visible) return;
        this.isHovered = this.isMouseOver(mouseX, mouseY);
        int offsetV = this.stateOffset[0];
        if (this.selected) {
            offsetV = this.stateOffset[1];
        } else if (this.isHovered) {
            offsetV = this.stateOffset[2];
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

    @Override
    public void onPress(InputWithModifiers input) {
        this.selected = !this.selected;
        this.onPress.onPress(this);
    }

    public interface OnPress {
        void onPress(TriStateButton btn);
    }
}
