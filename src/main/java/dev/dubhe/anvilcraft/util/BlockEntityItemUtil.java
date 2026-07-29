package dev.dubhe.anvilcraft.util;

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

    public static void saveToItem(BlockEntity blockEntity, ItemStack stack, HolderLookup.Provider registries) {
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
        blockEntity.saveCustomOnly(output);
        blockEntity.removeComponentsFromTag(output);
        BlockItem.setBlockEntityData(stack, blockEntity.getType(), output);
        stack.applyComponents(blockEntity.collectComponents());
    }
}
