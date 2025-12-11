package dev.dubhe.anvilcraft.api.entity.fakeplayer;

import com.mojang.authlib.GameProfile;
import lombok.Data;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.IntFunction;

public class AnvilCraftDestroyerFakePlayer {
    private static final IntFunction<GameProfile> FAKE_PROFILE_FACTORY = num -> new GameProfile(
        UUID.randomUUID(),
        "[Destroyer of AnvilCraft No." + num + "]"
    );
    private static final Queue<Destroyer> DISABLED_DESTROYERS = new ConcurrentLinkedQueue<>();
    private static final List<Destroyer> ENABLED_DESTROYERS = Collections.synchronizedList(new ArrayList<>());
    private static WeakReference<ItemStack> DUMMY_BREAK_TOOL_REF = new WeakReference<>(null);

    public AnvilCraftDestroyerFakePlayer() {
    }

    public ServerPlayer offerPlayer(ServerLevel level) {
        Destroyer destroyer = DISABLED_DESTROYERS.poll();
        if (destroyer == null) {
            destroyer = new Destroyer(level, ENABLED_DESTROYERS.size());
        }
        ENABLED_DESTROYERS.add(destroyer);
        return destroyer.getPlayer();
    }

    public void enabledDestroy(ServerPlayer player, ItemStack itemStack) {
        ItemStack dummyTool = DUMMY_BREAK_TOOL_REF.get();
        if (dummyTool == null) {
            dummyTool = itemStack;
            DUMMY_BREAK_TOOL_REF = new WeakReference<>(dummyTool);
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, dummyTool.copy());
    }

    public void recyclePlayer(ServerPlayer player) {
        Destroyer destroyer = null;
        synchronized (ENABLED_DESTROYERS) {
            Iterator<Destroyer> it = ENABLED_DESTROYERS.iterator();
            while (it.hasNext()) {
                Destroyer d = it.next();
                if (d.getPlayer() == player) {
                    destroyer = d;
                    it.remove();
                    break;
                }
            }
        }
        if (destroyer != null) {
            DISABLED_DESTROYERS.offer(destroyer);
        }
    }

    @Data
    public static final class Destroyer {
        private final GameProfile profile;
        private final ServerPlayer player;

        public Destroyer(ServerLevel level, int index) {
            this.profile = FAKE_PROFILE_FACTORY.apply(index + 1);
            this.player = FakePlayerFactory.get(level, this.profile);
        }

        public UUID getUUID() {
            return this.player.getUUID();
        }
    }
}
