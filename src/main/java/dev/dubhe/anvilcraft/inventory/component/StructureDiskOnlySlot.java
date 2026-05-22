package dev.dubhe.anvilcraft.inventory.component;

import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 只允许放入Structure Disk物品的槽位
 * 并且结构大小不能超过 5x5x5
 */
public class StructureDiskOnlySlot extends Slot {
    public StructureDiskOnlySlot(net.minecraft.world.Container container, int slot, int x, int y) {
        super(container, slot, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        // 首先检查是否是结构磁盘
        if (!stack.is(ModItems.STRUCTURE_DISK.get())) {
            return false;
        }
        
        // 检查结构大小是否超过 5x5x5
        var customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains("SizeX") && tag.contains("SizeY") && tag.contains("SizeZ")) {
                int sizeX = tag.getInt("SizeX");
                int sizeY = tag.getInt("SizeY");
                int sizeZ = tag.getInt("SizeZ");
                
                // 如果结构大小超过 5x5x5，拒绝放入
                return sizeX <= 5 && sizeY <= 5 && sizeZ <= 5;
            }
        }
        
        return true;
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
