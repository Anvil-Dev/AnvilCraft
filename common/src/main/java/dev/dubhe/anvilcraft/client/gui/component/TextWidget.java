package dev.dubhe.anvilcraft.client.gui.component;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.network.chat.Component;

public class TextWidget extends StringWidget {

    private final TextProvider provider;

    public TextWidget(int x, int y, int width, int height, Font font, TextProvider provider) {
        super(x, y, width, height, Component.empty(), font);
        this.provider = provider;
    }

    @Override
    public Component getMessage() {
        return provider.get();
    }

    @FunctionalInterface
    public interface TextProvider {
        Component get();
    }
}
