package dev.dubhe.anvilcraft.client.gui.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

final class ScreenEditing {
    private ScreenEditing() {
    }

    static boolean isReadOnly() {
        Player player = Minecraft.getInstance().player;
        return player == null || player.isSpectator();
    }
}
