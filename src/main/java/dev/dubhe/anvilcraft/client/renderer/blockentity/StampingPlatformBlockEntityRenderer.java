package dev.dubhe.anvilcraft.client.renderer.blockentity;

import dev.dubhe.anvilcraft.block.entity.StampingPlatformBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

    /**
     * 冲压平台内容物渲染：物品躺平散开分布在台面上，不使用方块特判放大渲染。
     */
public class StampingPlatformBlockEntityRenderer
    extends ProcessingItemStackRenderer<StampingPlatformBlockEntity> {
    public StampingPlatformBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected boolean isBlockStateRenderEnabled() {
        return false;
    }

    @Override
    protected float getItemBaseXRotationDeg() {
        return 90;
    }
}
