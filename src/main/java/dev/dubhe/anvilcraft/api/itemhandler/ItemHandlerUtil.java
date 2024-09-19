package dev.dubhe.anvilcraft.api.itemhandler;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import java.util.function.Predicate;

public class ItemHandlerUtil {
    public static void exportToTarget(IItemHandler source, int maxAmount, Predicate<ItemStack> predicate, IItemHandler target) {
        for (int srcIndex = 0; srcIndex < source.getSlots(); srcIndex++) {
            ItemStack sourceStack = source.extractItem(srcIndex, Integer.MAX_VALUE, true);
            if (sourceStack.isEmpty() || !predicate.test(sourceStack)) {
                continue;
            }
            ItemStack remainder = ItemHandlerHelper.insertItem(target, sourceStack, true);
            int amountToInsert = sourceStack.getCount() - remainder.getCount();
            if (amountToInsert > 0) {
                sourceStack = source.extractItem(srcIndex, Math.min(maxAmount, amountToInsert), false);
                ItemHandlerHelper.insertItem(target, sourceStack, false);
                maxAmount -= Math.min(maxAmount, amountToInsert);
                if (maxAmount <= 0) return;
            }
        }
    }

    public static void importFromTarget(IItemHandler target, int maxAmount, Predicate<ItemStack> predicate, IItemHandler source) {
        for (int srcIndex = 0; srcIndex < source.getSlots(); srcIndex++) {
            ItemStack sourceStack = source.extractItem(srcIndex, Integer.MAX_VALUE, true);
            if (sourceStack.isEmpty() || !predicate.test(sourceStack)) {
                continue;
            }
            ItemStack remainder = ItemHandlerHelper.insertItem(target, sourceStack, true);
            int amountToInsert = sourceStack.getCount() - remainder.getCount();
            if (amountToInsert > 0) {
                sourceStack = source.extractItem(srcIndex, Math.min(maxAmount, amountToInsert), false);
                ItemHandlerHelper.insertItem(target, sourceStack, false);
                maxAmount -= Math.min(maxAmount, amountToInsert);
            }
            if (maxAmount <= 0) return;
        }
    }
}
