package dev.dubhe.anvilcraft.api.entity.fakeplayer;

import com.mojang.authlib.GameProfile;
import lombok.Data;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.IntFunction;

public class AnvilCraftKillerFakePlayer {
    static final IntFunction<GameProfile> FAKE_PROFILE_FACTORY = num -> new GameProfile(
        UUID.randomUUID(), "[Killer of AnvilCraft No." + num + "]");
    private static final Queue<Killer> DISABLED_KILLERS = new ConcurrentLinkedQueue<>();
    private static final List<Killer> ENABLED_KILLERS = Collections.synchronizedList(new ArrayList<>());

    private static ItemStack DUMMY_LOOTING_5_WEAPON = null;

    public AnvilCraftKillerFakePlayer() {
    }

    public ServerPlayer offerPlayer(ServerLevel level) {
        Killer killer = DISABLED_KILLERS.poll();
        if (killer == null) {
            killer = new Killer(level, ENABLED_KILLERS.size());
        }
        ENABLED_KILLERS.add(killer);
        return killer.getPlayer();
    }

    public void enableLooting5(ServerLevel level, ServerPlayer player) {
        if (DUMMY_LOOTING_5_WEAPON == null) {
            ItemStack weapon = Items.POTATO.getDefaultInstance();
            weapon.set(DataComponents.CUSTOM_NAME, Component.literal("Looting 5 Potato!!!"));
            level.holderLookup(Registries.ENCHANTMENT)
                .get(Enchantments.LOOTING)
                .ifPresent(e -> weapon.enchant(e, 5));
            DUMMY_LOOTING_5_WEAPON = weapon;
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, DUMMY_LOOTING_5_WEAPON.copy());
    }

    public void disable(ServerPlayer player) {
        ENABLED_KILLERS.stream()
            .filter(killer -> killer.getUUID().equals(player.getUUID()))
            .findFirst()
            .ifPresent(killer -> {
                killer.getPlayer().setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                DISABLED_KILLERS.offer(killer);
                ENABLED_KILLERS.remove(killer);
            });
    }

    @Data
    public static final class Killer {
        private final GameProfile profile;
        private final ServerPlayer player;

        private Killer(ServerLevel level, int index) {
            this.profile = FAKE_PROFILE_FACTORY.apply(index + 1);
            this.player = FakePlayerFactory.get(level, this.profile);
        }

        public UUID getUUID() {
            return this.player.getUUID();
        }
    }
}
