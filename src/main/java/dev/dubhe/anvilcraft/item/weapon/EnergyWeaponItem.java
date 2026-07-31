package dev.dubhe.anvilcraft.item.weapon;

import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.property.component.StoredEnergy;
import dev.dubhe.anvilcraft.util.ColorUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public abstract class EnergyWeaponItem extends Item {
    public static final int MAX_ENERGY = 640_000_000;
    public static final int REFILL_THRESHOLD = 480_000_000;
    private static final int FULL_BAR_COLOR = 0xFF5454FF;
    private static final int BAR_COLOR = 0x7087FFFF;
    private static final Component INSUFFICIENT_POWER = Component.translatable("screen.anvilcraft.cfa.power_fail")
        .withStyle(ChatFormatting.RED);

    protected EnergyWeaponItem(Properties properties) {
        super(properties.component(ModComponents.STORED_ENERGY, new StoredEnergy(EnergyWeaponItem.MAX_ENERGY)));
    }

    protected boolean consumeEnergy(Player player, ItemStack weapon, int amount, int refillAmount) {
        int energy = EnergyWeaponItem.getEnergy(weapon);
        if (energy < EnergyWeaponItem.REFILL_THRESHOLD) {
            int slot = player.getInventory().findSlotMatchingItem(ModItems.SUPER_CAPACITOR.asStack());
            if (slot >= 0) {
                if (!player.hasInfiniteMaterials()) {
                    player.getInventory().removeItem(slot, 1);
                    player.getInventory().placeItemBackInInventory(ModItems.SUPER_CAPACITOR_EMPTY.asStack());
                }
                energy = Math.min(EnergyWeaponItem.MAX_ENERGY, energy + refillAmount);
            }
        }
        if (energy < amount) {
            EnergyWeaponItem.setEnergy(weapon, energy);
            this.stopForInsufficientPower(player);
            return false;
        }
        EnergyWeaponItem.setEnergy(weapon, energy - amount);
        if (!this.hasEnergyAvailable(player, weapon, amount)) {
            this.stopForInsufficientPower(player);
        }
        return true;
    }

    protected boolean canStartUsing(Player player, ItemStack weapon, int minimumEnergy) {
        if (this.hasEnergyAvailable(player, weapon, minimumEnergy)) return true;
        EnergyWeaponItem.showInsufficientPower(player);
        return false;
    }

    protected boolean hasEnergyAvailable(Player player, ItemStack weapon, int amount) {
        int energy = EnergyWeaponItem.getEnergy(weapon);
        if (energy >= amount) return true;
        return energy < EnergyWeaponItem.REFILL_THRESHOLD
               && player.getInventory().findSlotMatchingItem(ModItems.SUPER_CAPACITOR.asStack()) >= 0;
    }

    protected void stopForInsufficientPower(Player player) {
        EnergyWeaponItem.showInsufficientPower(player);
        player.stopUsingItem();
    }

    public static void showInsufficientPower(Player player) {
        player.sendOverlayMessage(EnergyWeaponItem.INSUFFICIENT_POWER);
    }

    private static int getEnergy(ItemStack stack) {
        return stack.getOrDefault(ModComponents.STORED_ENERGY, StoredEnergy.EMPTY).value();
    }

    private static void setEnergy(ItemStack stack, int energy) {
        stack.set(ModComponents.STORED_ENERGY, new StoredEnergy(energy));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(Math.clamp((float) EnergyWeaponItem.getEnergy(stack) / EnergyWeaponItem.MAX_ENERGY, 0, 1) * 13);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return ColorUtil.lerpColor((float) EnergyWeaponItem.getEnergy(stack) / EnergyWeaponItem.MAX_ENERGY, EnergyWeaponItem.BAR_COLOR, EnergyWeaponItem.FULL_BAR_COLOR);
    }
}
