package dev.dubhe.anvilcraft.client.renderer.blockentity;

import dev.dubhe.anvilcraft.block.entity.batch.BaseBatchCraftingBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public class BatchCraftingBERenderer extends BaseShowItemRenderer<BaseBatchCraftingBlockEntity> {
    public BatchCraftingBERenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected @Nullable ItemStack getDisplayItemStack(BaseBatchCraftingBlockEntity blockEntity) {
        return blockEntity.getDisplayingStack();
    }

    @Override
    protected int getSeed(BaseBatchCraftingBlockEntity blockEntity) {
        return blockEntity.getId();
    }
}
