package dev.dubhe.anvilcraft.client.renderer.blockentity;

import dev.dubhe.anvilcraft.block.entity.SiftingTableBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public class SiftingTableBlockEntityRenderer extends ProcessingItemStackRenderer<SiftingTableBlockEntity> {
    public SiftingTableBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }
}
