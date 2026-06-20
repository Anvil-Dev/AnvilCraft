package dev.dubhe.anvilcraft.client.renderer.blockentity;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.InfiniteCollectorBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;

public class InfiniteCollectorRenderer extends PowerProducerRenderer<InfiniteCollectorBlockEntity> {
    public static final ModelResourceLocation MODEL = ModelResourceLocation.standalone(
        AnvilCraft.of("block/infinite_collector_head")
    );

    public InfiniteCollectorRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    protected float elevation() {
        return 0.75f;
    }

    @Override
    protected float rotation(InfiniteCollectorBlockEntity blockEntity, float partialTick) {
        return blockEntity.getRotation() + (float) (Math.log(blockEntity.getServerPower() + 1) * 0.5f * partialTick);
    }

    @Override
    protected ModelResourceLocation getModel() {
        return MODEL;
    }
}
