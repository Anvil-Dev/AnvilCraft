package dev.dubhe.anvilcraft.util;

import dev.dubhe.anvilcraft.block.entity.StorageFluidPortBlockEntity;
import dev.dubhe.anvilcraft.block.entity.StoragePortBlockEntity;
import net.minecraft.core.HolderLookup;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.TagValueOutput;

/// 把方块实体的自定义数据写入物品堆，用于掉落物/中键拾取保留内容
public final class BlockEntityItemUtil {
    private BlockEntityItemUtil() {
    }

    @SuppressWarnings("deprecation")
    public static void saveToItem(BlockEntity blockEntity, ItemStack stack, HolderLookup.Provider registries) {
        if (blockEntity instanceof StorageFluidPortBlockEntity port) {
            port.saveToDrop(stack, registries);
            return;
        }
        if (blockEntity instanceof StoragePortBlockEntity port) {
            port.saveToDrop(stack, registries);
            return;
        }
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
        blockEntity.saveCustomOnly(output);
        blockEntity.removeComponentsFromTag(output);
        BlockItem.setBlockEntityData(stack, blockEntity.getType(), output);
        stack.applyComponents(blockEntity.collectComponents());
    }
}
