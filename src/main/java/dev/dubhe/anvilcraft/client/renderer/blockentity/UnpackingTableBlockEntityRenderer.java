package dev.dubhe.anvilcraft.client.renderer.blockentity;

import dev.dubhe.anvilcraft.block.entity.UnpackingTableBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public class UnpackingTableBlockEntityRenderer extends ProcessingItemStackRenderer<UnpackingTableBlockEntity> {
    public UnpackingTableBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }
}
