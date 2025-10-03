package dev.dubhe.anvilcraft.client.gui.component;

import dev.dubhe.anvilcraft.util.MathUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class SwitchableImageButton extends ImageButton {
    private static final Component EMPTY = Component.empty();
    private final Supplier<Boolean> status;
    private final List<Component> messages;

    public SwitchableImageButton(
        int x,
        int y,
        int width,
        int height,
        WidgetSprites sprites,
        Supplier<Boolean> status,
        Consumer<Boolean> changeStatus,
        OnPress onPress,
        List<Component> messages
    ) {
        super(x, y, width, height, sprites, button -> {
            changeStatus.accept(!status.get());
            onPress.onPress(button);
        });
        this.status = status;
        this.messages = messages;
    }

    public SwitchableImageButton(
        int x,
        int y,
        WidgetSprites sprites,
        Supplier<Boolean> status,
        Consumer<Boolean> changeStatus,
        OnPress onPress,
        List<Component> messages
    ) {
        this(x, y, 16, 16, sprites, status, changeStatus, onPress, messages);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        Component message = this.getMessage();
        if (
            MathUtil.isInRange(mouseX, this.getX(), this.getX() + this.width)
            && MathUtil.isInRange(mouseY, this.getY(), this.getY() + this.height)
            && !message.equals(EMPTY)
        ) {
            guiGraphics.renderTooltip(
                Minecraft.getInstance().font, List.of(getMessage()), Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    public Component getMessage() {
        if (this.messages.isEmpty() || this.messages.size() < 2) {
            return EMPTY;
        }
        return this.status.get() ? this.messages.get(0) : this.messages.get(1);
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        ResourceLocation resourcelocation = this.sprites.get(this.status.get(), this.isHovered());
        guiGraphics.blitSprite(resourcelocation, this.getX(), this.getY(), this.width, this.height);
    }
}
