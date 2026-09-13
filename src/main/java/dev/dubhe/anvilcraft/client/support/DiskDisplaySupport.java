package dev.dubhe.anvilcraft.client.support;

import dev.dubhe.anvilcraft.init.item.ModComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.Comparator;

public final class DiskDisplaySupport {
    private DiskDisplaySupport() {
    }

    public static ItemStack recordedBlock(ItemStack stack) {
        var data = stack.get(ModComponents.DISK_DATA);
        if (data == null) return ItemStack.EMPTY;
        var source = Identifier.tryParse(data.tag().getStringOr("StoredFrom", ""));
        if (source == null) return ItemStack.EMPTY;
        var type = BuiltInRegistries.BLOCK_ENTITY_TYPE.getOptional(source).orElse(null);
        if (type == null) return ItemStack.EMPTY;
        var blockId = Identifier.tryParse(data.tag().getStringOr("StoredBlock", ""));
        if (blockId != null) {
            var block = BuiltInRegistries.BLOCK.getOptional(blockId).orElse(Blocks.AIR);
            if (type.isValid(block.defaultBlockState())) return block.asItem().getDefaultInstance();
        }
        var sameName = BuiltInRegistries.BLOCK.getOptional(source).orElse(Blocks.AIR);
        if (type.isValid(sameName.defaultBlockState()) && sameName.asItem() != Items.AIR) return sameName.asItem().getDefaultInstance();
        return type.getValidBlocks().stream().filter(block -> block.asItem() != Items.AIR)
            .min(Comparator.comparing(block -> BuiltInRegistries.BLOCK.getKey(block).toString()))
            .map(block -> block.asItem().getDefaultInstance()).orElse(ItemStack.EMPTY);
    }
}
