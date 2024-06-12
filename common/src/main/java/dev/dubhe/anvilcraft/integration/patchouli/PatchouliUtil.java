package dev.dubhe.anvilcraft.integration.patchouli;

import dev.dubhe.anvilcraft.AnvilCraft;

import net.minecraft.server.level.ServerPlayer;

import vazkii.patchouli.api.PatchouliAPI;

public class PatchouliUtil {
    public static void openBook(ServerPlayer player) {
        PatchouliAPI.get().openBookGUI(player, AnvilCraft.of("guide"));
    }
}
