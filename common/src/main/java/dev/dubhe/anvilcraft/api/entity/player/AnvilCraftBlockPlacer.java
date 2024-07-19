package dev.dubhe.anvilcraft.api.entity.player;

import java.util.HashSet;
import java.util.Set;

public class AnvilCraftBlockPlacer {
    public static Set<String> BLOCK_PLACER_BLACKLIST = new HashSet<>();
    public static IAnvilCraftBlockPlacer anvilCraftBlockPlacer = null;
}
