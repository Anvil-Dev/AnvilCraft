package dev.dubhe.anvilcraft.util;

import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.util.FakePlayer;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PlayerUtil {

    public static boolean isFakePlayer(Player player) {
        return player instanceof FakePlayer;
    }
}
