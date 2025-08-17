package dev.dubhe.anvilcraft.api.entity.fakeplayer;

import dev.dubhe.anvilcraft.api.entity.player.IAnvilCraftBlockPlacer;

import java.util.HashSet;
import java.util.Set;

public class AnvilCraftFakePlayers {
    public static Set<String> BLOCK_PLACER_BLACKLIST = new HashSet<>();
    public static IAnvilCraftBlockPlacer anvilCraftBlockPlacer = null;
    public static AnvilCraftKillerFakePlayer anvilCraftKiller = null;
}
