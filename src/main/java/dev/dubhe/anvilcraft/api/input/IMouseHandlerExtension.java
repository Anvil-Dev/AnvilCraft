package dev.dubhe.anvilcraft.api.input;

import net.minecraft.client.MouseHandler;

public interface IMouseHandlerExtension {
    static IMouseHandlerExtension of(MouseHandler mouseHandler) {
        return (IMouseHandlerExtension) mouseHandler;
    }

    void anvilCraft$grabMouseWithScreen();
}
