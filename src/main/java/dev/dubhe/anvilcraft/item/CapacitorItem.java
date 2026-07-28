package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.api.item.IChargerDischargeable;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class CapacitorItem extends Item implements IChargerDischargeable {
    public static final int ENERGY = 8_000_000;

    public CapacitorItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack stack, Slot slot, ClickAction clickAction, Player player) {
        return CapacitorInventoryCharge.tryChargeTarget(
            stack,
            slot,
            clickAction,
            player,
            ENERGY,
            ModItems.CAPACITOR_EMPTY.asStack(1)
        );
    }

    @Override
    public ItemStack discharge(ItemStack input) {
        return ModItems.CAPACITOR_EMPTY.asStack(1);
    }
}
