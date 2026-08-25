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
}
