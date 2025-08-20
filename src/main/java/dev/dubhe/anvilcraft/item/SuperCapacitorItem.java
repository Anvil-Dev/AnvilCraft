package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.api.item.IChargerDischargeable;
import dev.dubhe.anvilcraft.init.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class SuperCapacitorItem extends Item implements IChargerDischargeable {
    public SuperCapacitorItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack discharge(ItemStack input) {
        return ModItems.CAPACITOR_EMPTY.asStack(1);
    }
}
