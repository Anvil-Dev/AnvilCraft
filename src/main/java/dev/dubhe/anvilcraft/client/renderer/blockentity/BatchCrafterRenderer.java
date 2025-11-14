package dev.dubhe.anvilcraft.client.renderer.blockentity;

import dev.dubhe.anvilcraft.block.entity.BatchCrafterBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public class BatchCrafterRenderer extends BaseShowItemRenderer<BatchCrafterBlockEntity> {
    public BatchCrafterRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected @Nullable ItemStack getDisplayItemStack(BatchCrafterBlockEntity blockEntity) {
        return blockEntity.getDisplayItemStack();
    }

    @Override
    protected int getSeed(BatchCrafterBlockEntity blockEntity) {
        return blockEntity.getId();
    }
}
