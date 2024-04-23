package dev.dubhe.anvilcraft.util.fabric;

import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.world.entity.player.Player;

public class PlayerUtilImpl {
    public static boolean isFakePlayer(Player player) {
        return player instanceof FakePlayer;
    }
}
