package dev.dubhe.anvilcraft.client.gui.component;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class SwitchableImageButton extends ImageButton {
    private final Supplier<Boolean> status;

    public SwitchableImageButton(
        int x,
        int y,
        int width,
        int height,
        WidgetSprites sprites,
        Supplier<Boolean> status,
        Consumer<Boolean> changeStatus,
        OnPress onPress,
        Component message
    ) {
        super(x, y, width, height, sprites, button -> {
            changeStatus.accept(!status.get());
            onPress.onPress(button);
        }, message);
        this.status = status;
    }

    public SwitchableImageButton(
        int x,
        int y,
        int width,
        int height,
        WidgetSprites sprites,
        Supplier<Boolean> status,
        Consumer<Boolean> changeStatus,
        OnPress onPress
    ) {
        this(x, y, width, height, sprites, status, changeStatus, onPress, CommonComponents.EMPTY);
    }

    public SwitchableImageButton(
        int x,
        int y,
        WidgetSprites sprites,
        Supplier<Boolean> status,
        Consumer<Boolean> changeStatus,
        OnPress onPress
    ) {
        this(x, y, 18, 18, sprites, status, changeStatus, onPress);
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        ResourceLocation resourcelocation = this.sprites.get(this.status.get(), this.isHovered());
        guiGraphics.blitSprite(resourcelocation, this.getX(), this.getY(), this.width, this.height);
    }
}
