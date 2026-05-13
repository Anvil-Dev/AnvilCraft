package dev.dubhe.anvilcraft.api.injection.input;

import net.minecraft.client.MouseHandler;

public interface IMouseHandlerExtension {
    void anvilcraft$grabMouseWithScreen();

    static IMouseHandlerExtension of(MouseHandler mouseHandler) {
        return (IMouseHandlerExtension) mouseHandler;
    }
}
