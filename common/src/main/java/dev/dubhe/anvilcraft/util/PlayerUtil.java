package dev.dubhe.anvilcraft.util;

import net.minecraft.world.entity.player.Player;

import dev.architectury.injectables.annotations.ExpectPlatform;

public abstract class PlayerUtil {
    private PlayerUtil() {}

    @ExpectPlatform
    public static boolean isFakePlayer(Player player) {
        throw new AssertionError();
    }
}
