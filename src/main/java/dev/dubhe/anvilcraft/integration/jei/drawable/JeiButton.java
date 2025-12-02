package dev.dubhe.anvilcraft.integration.jei.drawable;

import mezz.jei.api.gui.inputs.IJeiGuiEventListener;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.navigation.ScreenRectangle;

import java.util.function.Consumer;

public class JeiButton<T> implements IJeiGuiEventListener {
    private final Consumer<T> onClickCallback;
    private final int buttonX;
    private final int buttonY;
    private final int size;
    private final T metadataKey;

    public JeiButton(int buttonX, int buttonY, int size, Consumer<T> onClickCallback, T metadataKey) {
        this.onClickCallback = onClickCallback;
        this.buttonX = buttonX;
        this.buttonY = buttonY;
        this.size = size;
        this.metadataKey = metadataKey;
    }

    @Override
    public ScreenRectangle getArea() {
        return new ScreenRectangle(new ScreenPosition(buttonX, buttonY), size, size);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            onClickCallback.accept(metadataKey);
            return true;
        }
        return false;
    }
}
