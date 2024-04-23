package dev.dubhe.anvilcraft.util;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.world.entity.player.Player;

public abstract class PlayerUtil {
    private PlayerUtil() {
    }

    @ExpectPlatform
    public static boolean isFakePlayer(Player player) {
        throw new AssertionError();
    }
}
