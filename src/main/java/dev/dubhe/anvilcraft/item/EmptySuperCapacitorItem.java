package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.api.item.IEmptyCapacitor;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class EmptySuperCapacitorItem extends Item implements IEmptyCapacitor {
    public EmptySuperCapacitorItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack getFull(ItemStack empty) {
        return empty.transmuteCopy(ModItems.SUPER_CAPACITOR, 1);
    }
}
