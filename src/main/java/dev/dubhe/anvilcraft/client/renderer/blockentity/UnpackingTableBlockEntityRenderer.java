package dev.dubhe.anvilcraft.client.renderer.blockentity;

import dev.dubhe.anvilcraft.block.entity.UnpackingTableBlockEntity;
import dev.dubhe.anvilcraft.client.AnvilCraftClient;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public class UnpackingTableBlockEntityRenderer extends ProcessingItemStackRenderer<UnpackingTableBlockEntity> {
    public UnpackingTableBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected boolean isBlockStateRenderEnabled() {
        return AnvilCraftClient.CONFIG.siftingUnpackingBlockRenderEnabled;
    }

    @Override
    protected boolean isBlockStateRenderBlocked(UnpackingTableBlockEntity table) {
        return table.getLevel() != null
            && !table.getLevel().getBlockState(table.getBlockPos().above()).isAir();
    }
}
