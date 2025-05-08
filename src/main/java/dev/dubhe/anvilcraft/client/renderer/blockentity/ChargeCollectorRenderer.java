package dev.dubhe.anvilcraft.client.renderer.blockentity;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.ChargeCollectorBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;

public class ChargeCollectorRenderer extends PowerProducerRenderer<ChargeCollectorBlockEntity> {
    public static final ModelResourceLocation MODEL = ModelResourceLocation.standalone(
        AnvilCraft.of("block/charge_collector_cube")
    );

    public ChargeCollectorRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    protected float elevation() {
        return 0.75f;
    }

    @Override
    protected float rotation(ChargeCollectorBlockEntity blockEntity, float partialTick) {
        return blockEntity.getRotation() + blockEntity.getServerPower() * 0.03f * partialTick;
    }

    @Override
    protected ModelResourceLocation getModel() {
        return MODEL;
    }
}
