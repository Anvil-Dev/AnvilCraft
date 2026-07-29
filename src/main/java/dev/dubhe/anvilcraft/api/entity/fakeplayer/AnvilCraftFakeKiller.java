package dev.dubhe.anvilcraft.api.entity.fakeplayer;

import com.mojang.authlib.GameProfile;
import dev.dubhe.anvilcraft.init.enchantment.ModEnchantments;
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
import javax.annotation.Nullable;

public class AnvilCraftFakeKiller {
    static final IntFunction<GameProfile> FAKE_PROFILE_FACTORY = num -> new GameProfile(
        UUID.randomUUID(),
        "[AnvilCraft Fake Killer No." + num + "]"
    );
    private final Queue<Killer> disabledKillers = new ConcurrentLinkedQueue<>();
    private final List<Killer> enabledKillers = Collections.synchronizedList(new ArrayList<>());

    private @Nullable ItemStack dummyLooting5Weapon;
    private @Nullable ItemStack dummyDisintegrationWeapon;

    public AnvilCraftFakeKiller() {
    }

    public ServerPlayer offerPlayer(ServerLevel level) {
        Killer killer;
        do {
            killer = this.disabledKillers.poll();
        } while (killer != null && killer.player().level() != level);
        if (killer == null) {
            killer = new Killer(level, this.enabledKillers.size());
        }
        this.enabledKillers.add(killer);
        return killer.player();
    }

    public void enableLooting5(ServerLevel level, ServerPlayer player) {
        if (this.dummyLooting5Weapon == null) {
            ItemStack weapon = Items.POTATO.getDefaultInstance();
            weapon.set(DataComponents.CUSTOM_NAME, Component.literal("Looting 5 Potato!!!"));
            level.holderLookup(Registries.ENCHANTMENT)
                .get(Enchantments.LOOTING)
                .ifPresent(e -> weapon.enchant(e, 5));
            this.dummyLooting5Weapon = weapon;
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, this.dummyLooting5Weapon.copy());
    }

    public void enableDisintegration(ServerLevel level, ServerPlayer player) {
        if (this.dummyDisintegrationWeapon == null) {
            ItemStack weapon = Items.QUARTZ.getDefaultInstance();
            weapon.set(DataComponents.CUSTOM_NAME, Component.literal("Disintegration Quartz!!!"));
            level.holderLookup(Registries.ENCHANTMENT)
                .get(ModEnchantments.DISINTEGRATION_KEY)
                .ifPresent(e -> weapon.enchant(e, 1));
            this.dummyDisintegrationWeapon = weapon;
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, this.dummyDisintegrationWeapon.copy());
    }

    public void disable(ServerPlayer player) {
        this.enabledKillers.stream()
            .filter(killer -> killer.getUUID().equals(player.getUUID()))
            .findFirst()
            .ifPresent(killer -> {
                killer.player().setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                this.disabledKillers.offer(killer);
                this.enabledKillers.remove(killer);
            });
    }

    public void clear(ServerLevel level) {
        this.disabledKillers.removeIf(killer -> clearIfInLevel(killer.player(), level));
        synchronized (this.enabledKillers) {
            this.enabledKillers.removeIf(killer -> clearIfInLevel(killer.player(), level));
        }
        this.dummyLooting5Weapon = null;
        this.dummyDisintegrationWeapon = null;
    }

    private static boolean clearIfInLevel(ServerPlayer player, ServerLevel level) {
        if (player.level() != level) {
            return false;
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        return true;
    }

    public record Killer(ServerPlayer player, GameProfile profile) {
        public Killer(ServerLevel player, int profile) {
            this(FakePlayerFactory.get(player, Killer.create(profile)), Killer.create(profile));
        }

        private static GameProfile create(int profile) {
            return AnvilCraftFakeKiller.FAKE_PROFILE_FACTORY.apply(profile + 1);
        }

        public UUID getUUID() {
            return this.player.getUUID();
        }
    }
}
