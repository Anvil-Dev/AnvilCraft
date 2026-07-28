package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.api.item.IChargerDischargeable;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class SuperCapacitorItem extends Item implements IChargerDischargeable {
    public static final int ENERGY = 160_000_000;

    public SuperCapacitorItem(Properties properties) {
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
            ModItems.SUPER_CAPACITOR_EMPTY.asStack(1)
        );
    }

    @Override
    public ItemStack discharge(ItemStack input) {
        return ModItems.SUPER_CAPACITOR_EMPTY.asStack(1);
    }
}
