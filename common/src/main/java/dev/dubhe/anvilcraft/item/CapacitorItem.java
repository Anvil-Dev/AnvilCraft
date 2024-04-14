package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.init.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class CapacitorItem extends Item {
    public CapacitorItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull ItemStack getDefaultInstance() {
        ItemStack stack = new ItemStack(ModItems.CAPACITOR);
        stack.setDamageValue(1);
        return stack;
    }

    public static boolean isFull(@NotNull ItemStack stack) {
        return stack.getDamageValue() < stack.getMaxDamage();
    }
}
