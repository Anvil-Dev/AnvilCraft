package dev.dubhe.anvilcraft.client.renderer.blockentity;

import dev.dubhe.anvilcraft.block.entity.InfiniteCollectorBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.PowerGeneratorRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

public class InfiniteCollectorRenderer extends PowerProducerRenderer<InfiniteCollectorBlockEntity, PowerGeneratorRenderState> {
    public static final StandaloneModelKey<BlockStateModel> MODEL = new StandaloneModelKey<>(
        () -> "AnvilCraft: Infinite Collector Head Model"
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
    protected StandaloneModelKey<BlockStateModel> getModel() {
        return MODEL;
    }

    @Override
    public PowerGeneratorRenderState createRenderState() {
        return new PowerGeneratorRenderState();
    }
}
