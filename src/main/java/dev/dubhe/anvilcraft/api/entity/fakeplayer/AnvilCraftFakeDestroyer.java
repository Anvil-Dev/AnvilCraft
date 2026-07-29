package dev.dubhe.anvilcraft.api.entity.fakeplayer;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.IntFunction;

public class AnvilCraftFakeDestroyer {
    static final IntFunction<GameProfile> FAKE_PROFILE_FACTORY = num -> new GameProfile(
        UUID.randomUUID(),
        "[AnvilCraft Fake Destroyer No." + num + "]"
    );
    private final Queue<Destroyer> disabledDestroyers = new ConcurrentLinkedQueue<>();
    private final List<Destroyer> enabledDestroyers = Collections.synchronizedList(new ArrayList<>());
    private @Nullable ItemStack dummyBreakTool;

    public AnvilCraftFakeDestroyer() {
    }

    public ServerPlayer offerPlayer(ServerLevel level) {
        Destroyer destroyer = this.disabledDestroyers.poll();
        if (destroyer == null) {
            destroyer = new Destroyer(level, this.enabledDestroyers.size());
        }
        this.enabledDestroyers.add(destroyer);
        return destroyer.player();
    }

    public void enabledDestroy(ServerPlayer player, ItemStack itemStack) {
        if (this.dummyBreakTool == null) {
            this.dummyBreakTool = itemStack;
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, this.dummyBreakTool.copy());
    }

    public void disable(ServerPlayer player) {
        for (Destroyer destroyer : this.enabledDestroyers) {
            if (!destroyer.getUUID().equals(player.getUUID())) continue;
            destroyer.player().setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            this.disabledDestroyers.offer(destroyer);
            this.enabledDestroyers.remove(destroyer);
            break;
        }
    }

    public void clear(ServerLevel level) {
        this.disabledDestroyers.removeIf(destroyer -> clearIfInLevel(destroyer.player(), level));
        synchronized (this.enabledDestroyers) {
            this.enabledDestroyers.removeIf(destroyer -> clearIfInLevel(destroyer.player(), level));
        }
        this.dummyBreakTool = null;
    }

    private static boolean clearIfInLevel(ServerPlayer player, ServerLevel level) {
        if (player.level() != level) {
            return false;
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        return true;
    }

    public record Destroyer(ServerPlayer player, GameProfile profile) {
        public Destroyer(ServerLevel player, int profile) {
            this(FakePlayerFactory.get(player, Destroyer.create(profile)), Destroyer.create(profile));
        }

        private static GameProfile create(int profile) {
            return AnvilCraftFakeDestroyer.FAKE_PROFILE_FACTORY.apply(profile + 1);
        }

        public UUID getUUID() {
            return this.player.getUUID();
        }
    }
}
