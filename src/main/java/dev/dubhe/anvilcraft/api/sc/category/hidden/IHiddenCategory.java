package dev.dubhe.anvilcraft.api.sc.category.hidden;

import dev.dubhe.anvilcraft.api.sc.category.ICategory;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public interface IHiddenCategory extends ICategory {
    @Override
    default ItemStack icon() {
        return ItemStack.EMPTY;
    }

    @Override
    default Component name() {
        return Component.literal("A hidden category");
    }
}
