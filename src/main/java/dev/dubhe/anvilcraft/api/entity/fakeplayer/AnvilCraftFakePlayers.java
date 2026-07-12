package dev.dubhe.anvilcraft.api.entity.fakeplayer;

import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class AnvilCraftFakePlayers {
    public static Set<String> BLOCK_PLACER_BLACKLIST = new HashSet<>();
    public static @Nullable AnvilCraftFakeBlockPlacer blockPlacer = null;
    public static @Nullable AnvilCraftFakeKiller killer = null;
    public static @Nullable AnvilCraftFakeDestroyer destroyer = null;

    public static AnvilCraftFakeBlockPlacer getBlockPlacer() {
        return Objects.requireNonNull(AnvilCraftFakePlayers.blockPlacer);
    }

    public static AnvilCraftFakeKiller getKiller() {
        return Objects.requireNonNull(AnvilCraftFakePlayers.killer);
    }

    public static AnvilCraftFakeDestroyer getDestroyer() {
        return Objects.requireNonNull(AnvilCraftFakePlayers.destroyer);
    }
}
