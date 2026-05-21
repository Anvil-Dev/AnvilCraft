package dev.dubhe.anvilcraft.client.renderer.blockentity;

import dev.dubhe.anvilcraft.block.entity.HeatCollectorBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.PowerGeneratorRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

public class HeatCollectorRenderer extends PowerProducerRenderer<HeatCollectorBlockEntity, PowerGeneratorRenderState> {
    public static final StandaloneModelKey<BlockStateModel> CUBE = new StandaloneModelKey<>(
        () -> "AnvilCraft: Heat Collector Cube Model"
    );

    public HeatCollectorRenderer(BlockEntityRendererProvider.Context ignored) {
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
    protected float rotation(HeatCollectorBlockEntity blockEntity, float partialTick) {
        return blockEntity.getRotation() + (float) (Math.log(blockEntity.getServerPower() + 1) * 2.5F * partialTick);
    }

    @Override
    protected StandaloneModelKey<BlockStateModel> getModel() {
        return CUBE;
    }
}
