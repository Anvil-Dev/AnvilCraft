package dev.dubhe.anvilcraft.client.gui.component;

import dev.anvilcraft.lib.v2.util.MathUtil;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class SwitchableButton extends Button {
    private final List<Identifier> textures = new ArrayList<>();
    private final List<Component> message;
    private final int texYDiff;
    private final int textureWidth;
    private final int textureHeight;
    @Getter
    @Setter
    private int current = 0;

    public SwitchableButton(
        int x,
        int y,
        int width,
        int height,
        List<Identifier> textures,
        int texYDiff,
        int textureWidth,
        int textureHeight,
        OnPress onPress
    ) {
        this(x, y, width, height, textures, texYDiff, textureWidth, textureHeight, onPress, List.of());
    }

    public SwitchableButton(
        int x,
        int y,
        int width,
        int height,
        List<Identifier> textures,
        int texYDiff,
        int textureWidth,
        int textureHeight,
        OnPress onPress,
        List<Component> message
    ) {
        super(x, y, width, height, Component.empty(), onPress, DEFAULT_NARRATION);
        this.textures.addAll(textures);
        this.message = message;
        this.texYDiff = texYDiff;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        if (!this.visible) return;
        this.isHovered = this.isMouseOver(mouseX, mouseY);
        int offsetV = 0;
        if (this.isHovered) {
            offsetV = this.texYDiff;
        }
        if (this.current < this.textures.size()) {
            graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                this.textures.get(this.current),
                this.getX(),
                this.getY(),
                0,
                offsetV,
                this.width,
                this.height,
                this.textureWidth,
                this.textureHeight
            );
        }
        if (
            MathUtil.isInRange(mouseX, this.getX(), this.getX() + this.width)
            && MathUtil.isInRange(mouseY, this.getY(), this.getY() + this.height)
            && !this.message.isEmpty()
            && this.textures.size() == this.message.size()
        ) {
            graphics.setTooltipForNextFrame(Collections.singletonList(this.getMessage().getVisualOrderText()), mouseX, mouseY);
        }
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            this.switchToNext();
        } else if (event.button() == 1) {
            this.switchToPrev();
        }
    }

    @Override
    protected boolean isValidClickButton(MouseButtonInfo buttonInfo) {
        return buttonInfo.button() == 0 || buttonInfo.button() == 1;
    }

    @Override
    public Component getMessage() {
        if (this.message.isEmpty()) {
            return Component.empty();
        }
        return this.message.get(this.getCurrent());
    }

    public void switchToNext() {
        this.current += 1;
        if (this.current >= this.textures.size()) {
            this.current = 0;
        }
        ((OnPress) this.onPress).onPress(this, this.current);
    }

    public void switchToPrev() {
        this.current -= 1;
        if (this.current < 0) {
            this.current = this.textures.size() - 1;
        }
        ((OnPress) this.onPress).onPress(this, this.current);
    }

    public interface OnPress extends Button.OnPress, Consumer<Button>, BiConsumer<Button, Integer> {
        void onPress(Button button, int index);

        @Override
        default void onPress(Button button) {
            if (button instanceof SwitchableButton stButton) {
                this.onPress(stButton, stButton.current);
            }
        }

        @Override
        default void accept(Button button) {
            this.onPress(button);
        }

        @Override
        default void accept(Button button, Integer index) {
            this.onPress(button, index);
        }
    }
}
