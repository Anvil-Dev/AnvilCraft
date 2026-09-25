package dev.dubhe.anvilcraft.item.ingredients;

import dev.dubhe.anvilcraft.api.item.IChargerDischargeable;
import dev.dubhe.anvilcraft.api.item.IFullCapacitor;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public class CapacitorItem extends Item implements IFullCapacitor, IChargerDischargeable {
    public static final int ENERGY = 8_000_000;

    public CapacitorItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack stack, Slot slot, ClickAction clickAction, Player player) {
        return IFullCapacitor.tryForceChargeTarget(this, stack, slot, clickAction, player);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);
        if (!(entity instanceof Player player)) {
            return;
        }
        IFullCapacitor.super.inventoryTick(stack, player);
    }

    @Override
    public int getEnergyStored(ItemStack stack) {
        return CapacitorItem.ENERGY;
    }

    @Override
    public ItemStack getEmpty(ItemStack full) {
        return full.transmuteCopy(ModItems.CAPACITOR_EMPTY, 1);
    }

    @Override
    public ItemStack discharge(ItemStack input) {
        return this.getEmpty(input);
    }
}
