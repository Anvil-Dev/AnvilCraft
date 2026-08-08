package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.api.item.IEmptyCapacitor;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class EmptyCapacitorItem extends Item implements IEmptyCapacitor {
    public EmptyCapacitorItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack getFull(ItemStack empty) {
        return empty.transmuteCopy(ModItems.CAPACITOR, 1);
    }
}
