package dev.dubhe.anvilcraft.inventory.component;

import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.function.BooleanSupplier;

/**
 * 只允许放入Structure Disk物品的槽位
 * 可选择是否限制结构大小不超过 5x5x5
 */
public class StructureDiskOnlySlot extends Slot {
    @Nullable
    private final BooleanSupplier canExtractCondition;
    private final boolean enforceSizeLimit;
    
    /**
     * 创建结构磁盘槽位
     * 
     * @param container 容器
     * @param slot 槽位索引
     * @param x X坐标
     * @param y Y坐标
     * @param enforceSizeLimit 是否强制执行 5x5x5 大小限制（Smart Block Placer 需要，Structure Scanner 不需要）
     */
    public StructureDiskOnlySlot(net.minecraft.world.Container container, int slot, int x, int y,
                                boolean enforceSizeLimit) {
        this(container, slot, x, y, enforceSizeLimit, null);
    }
    
    /**
     * 创建结构磁盘槽位（带提取条件）
     */
    public StructureDiskOnlySlot(net.minecraft.world.Container container, int slot, int x, int y,
                                @Nullable BooleanSupplier canExtractCondition) {
        this(container, slot, x, y, false, canExtractCondition);
    }
    
    /**
     * 创建结构磁盘槽位（完整参数）
     */
    public StructureDiskOnlySlot(net.minecraft.world.Container container, int slot, int x, int y,
                                boolean enforceSizeLimit,
                                @Nullable BooleanSupplier canExtractCondition) {
        super(container, slot, x, y);
        this.canExtractCondition = canExtractCondition;
        this.enforceSizeLimit = enforceSizeLimit;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        // 首先检查是否是结构磁盘
        if (!stack.is(ModItems.STRUCTURE_DISK.get())) {
            return false;
        }
        
        // 如果不强制限制大小，直接允许放入
        if (!enforceSizeLimit) {
            return true;
        }
        
        // 检查结构大小是否超过 5x5x5（仅用于 Smart Block Placer）
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
    public boolean mayPickup(net.minecraft.world.entity.player.Player playerIn) {
        // 检查提取条件(如果书槽位有书,则不允许取出)
        return canExtractCondition == null || canExtractCondition.getAsBoolean();
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
