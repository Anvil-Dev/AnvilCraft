package dev.dubhe.anvilcraft.client.renderer.blockentity;

import dev.dubhe.anvilcraft.block.entity.SiftingTableBlockEntity;
import dev.dubhe.anvilcraft.client.AnvilCraftClient;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public class SiftingTableBlockEntityRenderer extends ProcessingItemStackRenderer<SiftingTableBlockEntity> {
    public SiftingTableBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected boolean isBlockStateRenderEnabled() {
        return AnvilCraftClient.CONFIG.siftingUnpackingBlockRenderEnabled;
    }

    @Override
    protected boolean isBlockStateRenderBlocked(SiftingTableBlockEntity table) {
        return table.getLevel() != null
            && !table.getLevel().getBlockState(table.getBlockPos().above()).isAir();
    }
}
