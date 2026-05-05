package dev.dubhe.anvilcraft.client.renderer.blockentity;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.HeatCollectorBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelIdentifier;

public class HeatCollectorRenderer extends PowerProducerRenderer<HeatCollectorBlockEntity> {
    public static final ModelIdentifier MODEL = ModelIdentifier.standalone(
        AnvilCraft.of("block/heat_collector_cube")
    );

    public HeatCollectorRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    protected float elevation() {
        return 0.75F;
    }

    @Override
    protected float rotation(HeatCollectorBlockEntity blockEntity, float partialTick) {
        return blockEntity.getRotation() + (float) (Math.log(blockEntity.getServerPower() + 1) * 2.5F * partialTick);
    }

    @Override
    protected ModelIdentifier getModel() {
        return MODEL;
    }
}
