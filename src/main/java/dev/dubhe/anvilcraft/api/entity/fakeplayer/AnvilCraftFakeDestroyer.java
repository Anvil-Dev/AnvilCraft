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
    private static final Queue<Destroyer> DISABLED_DESTROYERS = new ConcurrentLinkedQueue<>();
    private static final List<Destroyer> ENABLED_DESTROYERS = Collections.synchronizedList(new ArrayList<>());
    private static @Nullable ItemStack DUMMY_BREAK_TOOL = null;

    public AnvilCraftFakeDestroyer() {
    }

    public ServerPlayer offerPlayer(ServerLevel level) {
        Destroyer destroyer = DISABLED_DESTROYERS.poll();
        if (destroyer == null) {
            destroyer = new Destroyer(level, ENABLED_DESTROYERS.size());
        }
        ENABLED_DESTROYERS.add(destroyer);
        return destroyer.player();
    }

    public void enabledDestroy(ServerPlayer player, ItemStack itemStack) {
        if (DUMMY_BREAK_TOOL == null) {
            DUMMY_BREAK_TOOL = itemStack;
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, DUMMY_BREAK_TOOL.copy());
    }

    public void disable(ServerPlayer player) {
        for (Destroyer destroyer : AnvilCraftFakeDestroyer.ENABLED_DESTROYERS) {
            if (!destroyer.getUUID().equals(player.getUUID())) continue;
            destroyer.player().setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            DISABLED_DESTROYERS.offer(destroyer);
            ENABLED_DESTROYERS.remove(destroyer);
            break;
        }
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
