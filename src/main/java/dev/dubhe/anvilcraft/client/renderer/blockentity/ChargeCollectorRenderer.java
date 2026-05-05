package dev.dubhe.anvilcraft.client.renderer.blockentity;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.ChargeCollectorBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelIdentifier;

public class ChargeCollectorRenderer extends PowerProducerRenderer<ChargeCollectorBlockEntity> {
    public static final ModelIdentifier MODEL = ModelIdentifier.standalone(
        AnvilCraft.of("block/charge_collector_cube")
    );

    public ChargeCollectorRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    protected float elevation() {
        return 0.75F;
    }

    @Override
    protected float rotation(ChargeCollectorBlockEntity blockEntity, float partialTick) {
        return blockEntity.getRotation() + (float) (Math.log(blockEntity.getServerPower() + 1) * 2.5F * partialTick);
    }

    @Override
    protected ModelIdentifier getModel() {
        return MODEL;
    }
}
