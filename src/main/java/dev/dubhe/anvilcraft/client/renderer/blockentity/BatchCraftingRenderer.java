package dev.dubhe.anvilcraft.client.renderer.blockentity;

import dev.dubhe.anvilcraft.block.entity.batch.BaseBatchCraftingBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.BaseShowItemRenderState;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public class BatchCraftingRenderer extends BaseShowItemRenderer<BaseBatchCraftingBlockEntity, BaseShowItemRenderState> {
    public BatchCraftingRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public BaseShowItemRenderState createRenderState() {
        return new BaseShowItemRenderState();
    }

    @Override
    protected @Nullable ItemStack getDisplayItemStack(BaseBatchCraftingBlockEntity blockEntity) {
        return blockEntity.getDisplayingStack();
    }
}
