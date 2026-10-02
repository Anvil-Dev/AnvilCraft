package dev.dubhe.anvilcraft.item.weapon;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.item.ICapacitorChargeable;
import dev.dubhe.anvilcraft.api.item.IFullCapacitor;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.util.ColorUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;

public abstract class EnergyWeaponItem extends Item implements ICapacitorChargeable {
    /** 仅用于注册期声明组件默认值的编译期常量；运行时上限请使用 {@link #maxEnergy()}。 */
    public static final int MAX_ENERGY = 640_000_000;
    private static final int FULL_BAR_COLOR = 0xFF5454FF;
    private static final int BAR_COLOR = 0x7087FFFF;
    private static final Component INSUFFICIENT_POWER = Component.translatable("screen.anvilcraft.cfa.power_fail")
        .withStyle(ChatFormatting.RED);

    protected EnergyWeaponItem(Properties properties) {
        super(properties
            .component(ModComponents.STORED_ENERGY, MAX_ENERGY)
            .component(DataComponents.CUSTOM_MODEL_DATA, CustomModelData.DEFAULT));
    }

    /** 能量上限，运行时读取配置。 */
    public static int maxEnergy() {
        return AnvilCraft.CONFIG.equipment.energyWeaponMaxEnergy;
    }

    /** 每次射击所需的最低能量，运行时读取配置。 */
    protected abstract int minimumEnergy();

    public boolean canFire(Player player, ItemStack weapon) {
        return this.hasEnergyAvailable(weapon, this.minimumEnergy());
    }

    protected boolean canContinueUsing(Player player, ItemStack weapon) {
        if (this.hasEnergyAvailable(weapon, this.minimumEnergy())) return true;
        this.stopForInsufficientPower(player, weapon);
        return false;
    }

    protected boolean consumeEnergy(Player player, ItemStack weapon, int amount) {
        int energy = weapon.getOrDefault(ModComponents.STORED_ENERGY, 0);
        if (energy < amount) {
            weapon.set(ModComponents.STORED_ENERGY, energy);
            this.stopForInsufficientPower(player, weapon);
            return false;
        }
        energy -= amount;
        weapon.set(ModComponents.STORED_ENERGY, energy);
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
        int energy = weapon.getOrDefault(ModComponents.STORED_ENERGY, 0);
        return energy >= amount;
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
        return energy <= 0 ? 0 : Math.max(1, Math.round(Math.clamp((float) energy / maxEnergy(), 0, 1) * 13));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        float energy = stack.getOrDefault(ModComponents.STORED_ENERGY, 0);
        return ColorUtil.lerpColor(energy / maxEnergy(), BAR_COLOR, FULL_BAR_COLOR);
    }

    @Override
    public void onCharged(ItemStack stack, IFullCapacitor capacitor, ItemStack capacitorStack) {
        stack.set(DataComponents.CUSTOM_MODEL_DATA, CustomModelData.DEFAULT);
    }

}
