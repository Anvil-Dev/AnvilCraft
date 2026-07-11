package dev.dubhe.anvilcraft.item.weapon;

import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.util.ColorUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;

public abstract class EnergyWeaponItem extends Item {
    public static final int MAX_ENERGY = 640_000_000;
    public static final int REFILL_THRESHOLD = 480_000_000;
    private static final int FULL_BAR_COLOR = 0xFF5454FF;
    private static final int BAR_COLOR = 0x7087FFFF;
    private static final Component INSUFFICIENT_POWER = Component.translatable("screen.anvilcraft.cfa.power_fail")
        .withStyle(ChatFormatting.RED);

    protected EnergyWeaponItem(Properties properties) {
        super(properties
            .component(ModComponents.STORED_ENERGY, MAX_ENERGY)
            .component(DataComponents.CUSTOM_MODEL_DATA, CustomModelData.DEFAULT));
    }

    protected boolean consumeEnergy(Player player, ItemStack weapon, int amount, int refillAmount) {
        int energy = weapon.getOrDefault(ModComponents.STORED_ENERGY, 0);
        if (energy < REFILL_THRESHOLD) {
            int slot = player.getInventory().findSlotMatchingItem(ModItems.SUPER_CAPACITOR.asStack());
            if (slot >= 0) {
                player.getInventory().removeItem(slot, 1);
                player.getInventory().placeItemBackInInventory(ModItems.SUPER_CAPACITOR_EMPTY.asStack());
                energy = Math.min(MAX_ENERGY, energy + refillAmount);
            }
        }
        if (energy < amount) {
            weapon.set(ModComponents.STORED_ENERGY, energy);
            stopForInsufficientPower(player, weapon);
            return false;
        }
        energy -= amount;
        weapon.set(ModComponents.STORED_ENERGY, energy);
        if (hasEnergyAvailable(player, weapon, amount)) {
            setExhausted(weapon, false);
        } else {
            stopForInsufficientPower(player, weapon);
        }
        return true;
    }

    protected boolean canStartUsing(Player player, ItemStack weapon, int minimumEnergy) {
        if (hasEnergyAvailable(player, weapon, minimumEnergy)) {
            setExhausted(weapon, false);
            return true;
        }
        setExhausted(weapon, true);
        showInsufficientPower(player);
        return false;
    }

    protected boolean hasEnergyAvailable(Player player, ItemStack weapon, int amount) {
        int energy = weapon.getOrDefault(ModComponents.STORED_ENERGY, 0);
        if (energy >= amount) return true;
        return energy < REFILL_THRESHOLD
            && player.getInventory().findSlotMatchingItem(ModItems.SUPER_CAPACITOR.asStack()) >= 0;
    }

    protected void stopForInsufficientPower(Player player, ItemStack weapon) {
        setExhausted(weapon, true);
        showInsufficientPower(player);
        player.stopUsingItem();
    }

    public static void showInsufficientPower(Player player) {
        player.displayClientMessage(INSUFFICIENT_POWER, true);
    }

    protected static void setExhausted(ItemStack stack, boolean exhausted) {
        stack.set(DataComponents.CUSTOM_MODEL_DATA, exhausted ? new CustomModelData(1) : CustomModelData.DEFAULT);
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || oldStack.getItem() != newStack.getItem();
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        int energy = stack.getOrDefault(ModComponents.STORED_ENERGY, 0);
        return Math.round(Math.clamp((float) energy / MAX_ENERGY, 0, 1) * 13);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        float energy = stack.getOrDefault(ModComponents.STORED_ENERGY, 0);
        return ColorUtil.lerpColor(energy / MAX_ENERGY, BAR_COLOR, FULL_BAR_COLOR);
    }
}
