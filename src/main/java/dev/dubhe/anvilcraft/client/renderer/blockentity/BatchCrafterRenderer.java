package dev.dubhe.anvilcraft.client.renderer.blockentity;

import dev.dubhe.anvilcraft.block.entity.BatchCrafterBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemStack;

public class BatchCrafterRenderer extends BaseShowItemRenderer<BatchCrafterBlockEntity> {
    public BatchCrafterRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    ItemStack getDisplayItemStack(BatchCrafterBlockEntity blockEntity) {
        return blockEntity.getDisplayItemStack();
    }

    @Override
    int getSeed(BatchCrafterBlockEntity blockEntity) {
        return blockEntity.getId();
    }
}
