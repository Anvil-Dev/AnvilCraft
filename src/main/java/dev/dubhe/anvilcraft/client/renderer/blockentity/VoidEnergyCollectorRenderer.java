package dev.dubhe.anvilcraft.client.renderer.blockentity;

import dev.dubhe.anvilcraft.block.entity.VoidEnergyCollectorBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.PowerGeneratorRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

public class VoidEnergyCollectorRenderer extends PowerProducerRenderer<VoidEnergyCollectorBlockEntity, PowerGeneratorRenderState> {
    public static final StandaloneModelKey<BlockStateModel> HEAD = new StandaloneModelKey<>(
        () -> "AnvilCraft: Void Energy Collector Head Model"
    );

    public VoidEnergyCollectorRenderer(BlockEntityRendererProvider.Context ignored) {
    }

    @Override
    public PowerGeneratorRenderState createRenderState() {
        return new PowerGeneratorRenderState();
    }

    @Override
    protected float elevation() {
        return 0.75F;
    }

    @Override
    protected float rotation(VoidEnergyCollectorBlockEntity blockEntity, float partialTick) {
        return blockEntity.getRotation() + (float) (Math.log(blockEntity.getServerPower() + 1) * 2.5F * partialTick);
    }

    @Override
    protected StandaloneModelKey<BlockStateModel> getModel() {
        return HEAD;
    }
}
