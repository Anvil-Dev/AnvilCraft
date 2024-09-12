package dev.dubhe.anvilcraft.util;

import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.util.FakePlayer;

public abstract class PlayerUtil {
    private PlayerUtil() {}

    public static boolean isFakePlayer(Player player) {
        return player instanceof FakePlayer;
    }
}
