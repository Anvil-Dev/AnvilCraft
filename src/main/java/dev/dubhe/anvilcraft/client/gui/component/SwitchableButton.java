package dev.dubhe.anvilcraft.client.gui.component;

import dev.anvilcraft.lib.v2.util.MathUtil;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class SwitchableButton extends Button {
    public static final Button.OnPress DO_NOTHING = btn -> {
    };

    private final List<Identifier> textures = new ArrayList<>();
    private final List<Component> message;
    private final int texYDiff;
    private final int textureWidth;
    private final int textureHeight;
    @Getter
    @Setter
    private int current = 0;

    public SwitchableButton(
        int x, int y, int width, int height,
        List<Identifier> textures, int texYDiff, int textureWidth, int textureHeight,
        OnPress onPress
    ) {
        this(x, y, width, height, textures, texYDiff, textureWidth, textureHeight, onPress, List.of());
    }

    public SwitchableButton(
        int x, int y, int width, int height,
        List<Identifier> textures, int texYDiff, int textureWidth, int textureHeight,
        OnPress onPress, List<Component> message
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
            graphics.blit(this.textures.get(this.current), this.getX(), this.getY(), 0, offsetV, width, height, this.textureWidth, this.textureHeight);
        }
        if (MathUtil.isInRange(mouseX, this.getX(), this.getX() + this.width)
            && MathUtil.isInRange(mouseY, this.getY(), this.getY() + this.height)
            && !this.message.isEmpty()
            && this.textures.size() == this.message.size()) {
            graphics.tooltip(
                Minecraft.getInstance().font,
                List.of(ClientTooltipComponent.create(this.getMessage().getVisualOrderText())),
                mouseX,
                mouseY,
                DefaultTooltipPositioner.INSTANCE,
                null
            );
        }
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
