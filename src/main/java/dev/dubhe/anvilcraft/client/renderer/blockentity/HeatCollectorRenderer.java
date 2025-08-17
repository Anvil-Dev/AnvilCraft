package dev.dubhe.anvilcraft.client.renderer.blockentity;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.HeatCollectorBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;

public class HeatCollectorRenderer extends PowerProducerRenderer<HeatCollectorBlockEntity> {
    public static final ModelResourceLocation MODEL = ModelResourceLocation.standalone(
        AnvilCraft.of("block/heat_collector_cube")
    );

    public HeatCollectorRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    protected float elevation() {
        return 0.75f;
    }

    @Override
    protected float rotation(HeatCollectorBlockEntity blockEntity, float partialTick) {
        return blockEntity.getRotation() + blockEntity.getServerPower() * 0.03f * partialTick;
    }

    @Override
    protected ModelResourceLocation getModel() {
        return MODEL;
    }
}
