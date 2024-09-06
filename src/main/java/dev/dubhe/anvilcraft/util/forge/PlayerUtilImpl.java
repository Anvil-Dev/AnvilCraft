package dev.dubhe.anvilcraft.util.forge;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.util.FakePlayer;

public class PlayerUtilImpl {
    public static boolean isFakePlayer(Player player) {
        return player instanceof FakePlayer;
    }
}
