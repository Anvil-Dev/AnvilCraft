package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.api.item.IChargerChargeable;
import dev.dubhe.anvilcraft.init.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class EmptyCapacitorItem extends Item implements IChargerChargeable {

    public EmptyCapacitorItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack charge(ItemStack input) {
        return ModItems.CAPACITOR.asStack(1);
    }
}
