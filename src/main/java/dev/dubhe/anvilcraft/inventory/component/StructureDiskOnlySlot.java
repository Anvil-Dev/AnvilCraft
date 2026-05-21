package dev.dubhe.anvilcraft.inventory.component;

import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 只允许放入Structure Disk物品的槽位
 */
public class StructureDiskOnlySlot extends Slot {
    public StructureDiskOnlySlot(net.minecraft.world.Container container, int slot, int x, int y) {
        super(container, slot, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return stack.is(ModItems.STRUCTURE_DISK);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return 1;
    }
}
