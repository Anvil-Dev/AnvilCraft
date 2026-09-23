package dev.dubhe.anvilcraft.client.gui.component;

import dev.dubhe.anvilcraft.constant.SharedTextures;

public class SimpleIconButton extends TexturedButton {
    public SimpleIconButton(int x, int y, String variant, OnPress onPress) {
        super(x, y, 10, 10, SharedTextures.textureGui("machine/button_%s".formatted(variant)), 10, 10, 20, onPress);
    }
}
