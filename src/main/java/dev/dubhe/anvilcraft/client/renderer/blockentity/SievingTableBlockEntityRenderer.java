package dev.dubhe.anvilcraft.client.renderer.blockentity;

import dev.dubhe.anvilcraft.block.entity.SievingTableBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public class SievingTableBlockEntityRenderer extends ProcessingItemStackRenderer<SievingTableBlockEntity> {
    public SievingTableBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }
}
