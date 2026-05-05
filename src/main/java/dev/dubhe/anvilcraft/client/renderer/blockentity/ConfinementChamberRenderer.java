package dev.dubhe.anvilcraft.client.renderer.blockentity;

import dev.dubhe.anvilcraft.block.entity.ConfinementChamberBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.BaseShowItemRenderState;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public class ConfinementChamberRenderer extends BaseShowItemRenderer<ConfinementChamberBlockEntity, BaseShowItemRenderState> {
    public ConfinementChamberRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public BaseShowItemRenderState createRenderState() {
        return new BaseShowItemRenderState();
    }

    @Override
    protected @Nullable ItemStack getDisplayItemStack(ConfinementChamberBlockEntity blockEntity) {
        return blockEntity.getItemHandler().copyToList().getFirst();
    }
}
