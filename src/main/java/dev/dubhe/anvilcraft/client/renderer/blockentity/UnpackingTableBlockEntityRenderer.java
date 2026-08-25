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
}
