package dev.dubhe.anvilcraft.item.weapon;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.item.ICapacitorChargeable;
import dev.dubhe.anvilcraft.api.item.IFullCapacitor;
import dev.dubhe.anvilcraft.network.EnergyWeaponReloadPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class EnergyWeaponReload {
    public static final int CHARGE_TICK = 24;
    public static final int DURATION = 40;
    public static final String CLIENT_SLOT = "anvilcraft:weapon_reload_slot";
    private static final String ESCROW = "anvilcraft:weapon_reload_capacitor";
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    private EnergyWeaponReload() {
    }

    public static boolean isWeapon(ItemStack stack) {
        return stack.getItem() instanceof EnergyWeaponItem || stack.getItem() instanceof SpectralWeaponLauncherItem;
    }

    @Nullable
    public static InteractionHand holdingHand(Player player, ItemStack weapon) {
        if (weapon.isEmpty()) return null;
        if (player.getMainHandItem() == weapon) return InteractionHand.MAIN_HAND;
        if (player.getOffhandItem() == weapon) return InteractionHand.OFF_HAND;
        return null;
    }

    public static boolean isReloading(Player player, ItemStack stack) {
        if (player.level().isClientSide()) {
            CompoundTag data = player.getPersistentData();
            if (!data.contains(CLIENT_SLOT)) return false;
            int slot = data.getInt(CLIENT_SLOT);
            return slot >= 0 && slot < player.getInventory().getContainerSize() && player.getInventory().getItem(slot) == stack;
        }
        Session session = SESSIONS.get(player.getUUID());
        return session != null && session.weapon == stack;
    }

    public static boolean start(
        Player player, ItemStack weapon, IFullCapacitor capacitor, ItemStack source, boolean force, @Nullable Slot targetSlot
    ) {
        if (source.isEmpty() || !isWeapon(weapon) || !player.isAlive() || player.isSpectator()) return false;
        InteractionHand hand = holdingHand(player, weapon);
        if (hand == null) return false;
        if (player instanceof ServerPlayer serverPlayer) cancelIfNotHeld(serverPlayer);
        if (!player.level().isClientSide() && SESSIONS.containsKey(player.getUUID())) return false;
        ICapacitorChargeable chargeable = (ICapacitorChargeable) weapon.getItem();
        ItemStack probe = weapon.copy();
        if (!(force ? chargeable.chargeForce(probe, capacitor, source.copy()) : chargeable.charge(probe, capacitor, source.copy()))) {
            return false;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) return true;
        Session session = new Session(serverPlayer, weapon, capacitor, source.copyWithCount(1), force, targetSlot);
        SESSIONS.put(player.getUUID(), session);
        source.shrink(1);
        session.saveEscrow(serverPlayer);
        session.sync(serverPlayer);
        return true;
    }

    @SubscribeEvent
    public static void checkHolding(PlayerTickEvent.Pre event) {
        if (event.getEntity() instanceof ServerPlayer player) cancelIfNotHeld(player);
    }

    private static void cancelIfNotHeld(ServerPlayer player) {
        Session session = SESSIONS.get(player.getUUID());
        if (session != null && !session.isValid(player)) finish(player);
    }

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Session session = SESSIONS.get(player.getUUID());
        if (session == null) return;
        if (!session.isValid(player)) {
            finish(player);
            return;
        }
        session.ticks++;
        if (session.ticks == CHARGE_TICK) {
            ICapacitorChargeable chargeable = (ICapacitorChargeable) session.weapon.getItem();
            boolean charged = session.force
                ? chargeable.chargeForce(session.weapon, session.capacitor, session.full)
                : chargeable.charge(session.weapon, session.capacitor, session.full);
            if (!charged) {
                finish(player);
                return;
            }
            chargeable.onCharged(session.weapon, session.capacitor, session.full);
            session.returned = session.capacitor.getEmpty(session.full);
            session.saveEscrow(player);
            session.sync(player);
        }
        if (session.ticks >= DURATION) finish(player);
    }

    private static void finish(ServerPlayer player) {
        Session session = SESSIONS.remove(player.getUUID());
        if (session == null) return;
        player.getPersistentData().remove(ESCROW);
        player.getInventory().placeItemBackInInventory(session.returned);
        player.containerMenu.broadcastChanges();
        PacketDistributor.sendToPlayer(player, new EnergyWeaponReloadPacket(ItemStack.EMPTY, ItemStack.EMPTY, -1, DURATION));
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) finish(player);
    }

    @SubscribeEvent
    public static void death(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) finish(player);
    }

    @SubscribeEvent
    public static void changeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) finish(player);
    }

    @SubscribeEvent
    public static void stopServer(ServerStoppingEvent event) {
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) finish(player);
        SESSIONS.clear();
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        CompoundTag data = player.getPersistentData();
        if (!data.contains(ESCROW)) return;
        ItemStack returned = ItemStack.parseOptional(player.registryAccess(), data.getCompound(ESCROW));
        data.remove(ESCROW);
        if (!returned.isEmpty()) player.getInventory().placeItemBackInInventory(returned);
    }

    private static final class Session {
        private final ItemStack weapon;
        private final IFullCapacitor capacitor;
        private final ItemStack full;
        private final boolean force;
        @Nullable
        private final Slot targetSlot;
        private final AbstractContainerMenu menu;
        private final InteractionHand hand;
        private final int inventorySlot;
        private ItemStack returned;
        private int ticks;

        private Session(
            ServerPlayer player, ItemStack weapon, IFullCapacitor capacitor, ItemStack full, boolean force, @Nullable Slot targetSlot
        ) {
            this.weapon = weapon;
            this.capacitor = capacitor;
            this.full = full;
            this.returned = full.copy();
            this.force = force;
            this.targetSlot = targetSlot;
            this.menu = player.containerMenu;
            this.hand = player.getMainHandItem() == weapon ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            this.inventorySlot = this.hand == InteractionHand.MAIN_HAND ? player.getInventory().selected : Inventory.SLOT_OFFHAND;
        }

        private boolean isValid(ServerPlayer player) {
            if (!player.isAlive() || player.isSpectator() || player.getItemInHand(this.hand) != this.weapon) return false;
            if (this.targetSlot != null) {
                return player.containerMenu == this.menu && this.targetSlot.getItem() == this.weapon
                    && this.targetSlot.allowModification(player);
            }
            return this.inventorySlot >= 0 && player.getInventory().getItem(this.inventorySlot) == this.weapon;
        }

        private void saveEscrow(ServerPlayer player) {
            player.getPersistentData().put(ESCROW, this.returned.save(player.registryAccess()));
        }

        private void sync(ServerPlayer player) {
            player.getInventory().setChanged();
            if (this.targetSlot != null) this.targetSlot.setChanged();
            player.containerMenu.broadcastChanges();
            PacketDistributor.sendToPlayer(player, new EnergyWeaponReloadPacket(
                this.weapon.copy(), this.returned.copy(), this.inventorySlot, this.ticks));
        }
    }
}
