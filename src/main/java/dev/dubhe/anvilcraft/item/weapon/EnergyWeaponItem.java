package dev.dubhe.anvilcraft.item.weapon;

import dev.dubhe.anvilcraft.api.item.ICapacitorChargeable;
import dev.dubhe.anvilcraft.api.item.IFullCapacitor;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.StoredEnergy;
import dev.dubhe.anvilcraft.util.ColorUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;

public abstract class EnergyWeaponItem extends Item implements ICapacitorChargeable {
    public static final int MAX_ENERGY = 640_000_000;
    private static final int FULL_BAR_COLOR = 0xFF5454FF;
    private static final int BAR_COLOR = 0x7087FFFF;
    private static final Component INSUFFICIENT_POWER = Component.translatable("screen.anvilcraft.cfa.power_fail")
        .withStyle(ChatFormatting.RED);
    private final int minimumEnergy;

    protected EnergyWeaponItem(Properties properties, int minimumEnergy) {
        super(properties
            .component(ModComponents.STORED_ENERGY, new StoredEnergy(MAX_ENERGY))
            .component(DataComponents.CUSTOM_MODEL_DATA, CustomModelData.EMPTY));
        this.minimumEnergy = minimumEnergy;
    }

    public boolean canFire(Player player, ItemStack weapon) {
        return this.hasEnergyAvailable(weapon, this.minimumEnergy);
    }

    protected boolean canContinueUsing(Player player, ItemStack weapon) {
        if (this.hasEnergyAvailable(weapon, this.minimumEnergy)) return true;
        this.stopForInsufficientPower(player, weapon);
        return false;
    }

    protected boolean consumeEnergy(Player player, ItemStack weapon, int amount) {
        int energy = weapon.getOrDefault(ModComponents.STORED_ENERGY, StoredEnergy.EMPTY).value();
        if (energy < amount) {
            weapon.set(ModComponents.STORED_ENERGY, new StoredEnergy(energy));
            this.stopForInsufficientPower(player, weapon);
            return false;
        }
        energy -= amount;
        weapon.set(ModComponents.STORED_ENERGY, new StoredEnergy(energy));
        if (this.hasEnergyAvailable(weapon, amount)) {
            setExhausted(weapon, false);
        } else {
            this.stopForInsufficientPower(player, weapon);
        }
        return true;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    protected boolean canStartUsing(Player player, ItemStack weapon, int minimumEnergy) {
        if (this.hasEnergyAvailable(weapon, minimumEnergy)) {
            setExhausted(weapon, false);
            return true;
        }
        setExhausted(weapon, true);
        showInsufficientPower(player);
        return false;
    }

    protected boolean hasEnergyAvailable(ItemStack weapon, int amount) {
        int energy = weapon.getOrDefault(ModComponents.STORED_ENERGY, StoredEnergy.EMPTY).value();
        return energy >= amount;
    }

    protected void stopForInsufficientPower(Player player, ItemStack weapon) {
        setExhausted(weapon, true);
        showInsufficientPower(player);
        player.stopUsingItem();
    }

    public static void showInsufficientPower(Player player) {
        player.sendOverlayMessage(INSUFFICIENT_POWER);
    }

    protected static void setExhausted(ItemStack stack, boolean exhausted) {
        stack.set(DataComponents.CUSTOM_MODEL_DATA, exhausted
            ? new CustomModelData(java.util.List.of(1F), java.util.List.of(), java.util.List.of(), java.util.List.of())
            : CustomModelData.EMPTY);
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
        int energy = stack.getOrDefault(ModComponents.STORED_ENERGY, StoredEnergy.EMPTY).value();
        return energy <= 0 ? 0 : Math.max(1, Math.round(Math.clamp((float) energy / MAX_ENERGY, 0, 1) * 13));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        float energy = stack.getOrDefault(ModComponents.STORED_ENERGY, StoredEnergy.EMPTY).value();
        return ColorUtil.lerpColor(energy / MAX_ENERGY, BAR_COLOR, FULL_BAR_COLOR);
    }

    @Override
    public void onCharged(ItemStack stack, IFullCapacitor capacitor, ItemStack capacitorStack) {
        stack.set(DataComponents.CUSTOM_MODEL_DATA, CustomModelData.EMPTY);
    }

}
