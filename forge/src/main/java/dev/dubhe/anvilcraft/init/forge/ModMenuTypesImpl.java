package dev.dubhe.anvilcraft.init.forge;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;

public class ModMenuTypesImpl {
    public static void open(ServerPlayer player, MenuProvider provider) {
        player.openMenu(provider);
    }
}
