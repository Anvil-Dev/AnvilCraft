package dev.dubhe.anvilcraft.api.entity.fakeplayer;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
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

    public static void clear(ServerLevel level) {
        if (AnvilCraftFakePlayers.blockPlacer != null) {
            AnvilCraftFakePlayers.blockPlacer.clear(level);
        }
        if (AnvilCraftFakePlayers.killer != null) {
            AnvilCraftFakePlayers.killer.clear(level);
        }
        if (AnvilCraftFakePlayers.destroyer != null) {
            AnvilCraftFakePlayers.destroyer.clear(level);
        }
        FakePlayerFactory.unloadLevel(level);
    }

    static boolean clearIfInLevel(ServerPlayer player, ServerLevel level) {
        if (player.level() != level) {
            return false;
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        return true;
    }
}
