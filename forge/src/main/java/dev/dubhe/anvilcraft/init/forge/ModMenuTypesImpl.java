package dev.dubhe.anvilcraft.init.forge;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraftforge.network.NetworkHooks;

public class ModMenuTypesImpl {
    public static void open(ServerPlayer player, MenuProvider provider) {
        NetworkHooks.openScreen(player, provider);
    }

    public static void open(ServerPlayer player, MenuProvider provider, BlockPos pos) {
        NetworkHooks.openScreen(player, provider, pos);
    }
}
