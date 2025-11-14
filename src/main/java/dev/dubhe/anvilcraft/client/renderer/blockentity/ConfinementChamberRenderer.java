package dev.dubhe.anvilcraft.client.renderer.blockentity;

import dev.dubhe.anvilcraft.block.entity.ConfinementChamberBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public class ConfinementChamberRenderer extends BaseShowItemRenderer<ConfinementChamberBlockEntity> {
    public ConfinementChamberRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected @Nullable ItemStack getDisplayItemStack(ConfinementChamberBlockEntity blockEntity) {
        return blockEntity.getItemHandler().getStackInSlot(0);
    }

    @Override
    protected int getSeed(ConfinementChamberBlockEntity blockEntity) {
        return blockEntity.getId();
    }
}
