package dev.dubhe.anvilcraft.client.gui.component;

import com.google.common.collect.Collections2;
import dev.dubhe.anvilcraft.util.MathUtil;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
public class SwitchableButton extends Button {
    public static final Button.OnPress DO_NOTHING = OnPress::doNothing;

    private final List<Button> switchables = new ArrayList<>();
    private final List<Component> message;
    @Getter
    @Setter
    private int current = 0;

    public SwitchableButton(
        int pX, int pY, int pWidth, int pHeight,
        List<ResourceLocation> textures, int yDiffTex, int textureWidth, int textureHeight,
        OnPress pOnPress
    ) {
        this(
            pX, pY, pWidth, pHeight,
            Collections2.transform(
                textures,
                texture -> new TexturedButton(
                    pX, pY, pWidth, pHeight, texture, yDiffTex, textureWidth, textureHeight, DO_NOTHING)),
            pOnPress,
            List.of()
        );
    }

    public SwitchableButton(
        int pX, int pY, int pWidth, int pHeight,
        List<ResourceLocation> textures, int yDiffTex, int textureWidth, int textureHeight,
        OnPress pOnPress, List<Component> message
    ) {
        this(
            pX, pY, pWidth, pHeight,
            Collections2.transform(
                textures,
                texture -> new TexturedButton(
                    pX, pY, pWidth, pHeight, texture, yDiffTex, textureWidth, textureHeight, DO_NOTHING)),
            pOnPress,
            message
        );
    }

    public SwitchableButton(
        int pX, int pY, int pWidth, int pHeight, Collection<Button> buttons, OnPress pOnPress, List<Component> message
    ) {
        super(pX, pY, pWidth, pHeight, Component.empty(), pOnPress, DEFAULT_NARRATION);
        this.message = message;
        this.switchables.addAll(buttons);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.switchables.get(this.current).render(guiGraphics, mouseX, mouseY, partialTick);
        if (MathUtil.isInRange(mouseX, this.getX(), this.getX() + this.width)
            && MathUtil.isInRange(mouseY, this.getY(), this.getY() + this.height)
            && !this.message.isEmpty()
            && this.switchables.size() == this.message.size()) {
            guiGraphics.renderTooltip(
                Minecraft.getInstance().font, List.of(getMessage()), Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    public Component getMessage() {
        if (this.message.isEmpty()) {
            return Component.empty();
        }
        return this.message.get(this.getCurrent());
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        this.onPress(0);
    }

    @Override
    public void onClick(double mouseX, double mouseY, int button) {
        this.onPress(button);
    }

    protected void onPress(int button) {
        if (button == 0) this.current += 1;
        else if (button == 1) this.current -= 1;
        if (this.current < 0) this.current = this.switchables.size() - 1;
        else if (this.current >= this.switchables.size()) this.current = 0;

        ((OnPress) this.onPress).onPress(this, this.current);
    }

    @Override
    public void onPress() {
        this.onPress(0);
    }

    @Override
    protected boolean isValidClickButton(int button) {
        return button == 0 || button == 1;
    }

    public interface OnPress extends Button.OnPress, Consumer<Button>, BiConsumer<Button, @NotNull Integer> {
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

        private static <T> void doNothing(T t) {
        }
    }
}
