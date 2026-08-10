package dev.dubhe.anvilcraft.client.renderer.blockentity;

import dev.dubhe.anvilcraft.block.entity.FeCollectorBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.PowerGeneratorRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

public class FeCollectorRenderer extends PowerProducerRenderer<FeCollectorBlockEntity, PowerGeneratorRenderState> {
    public static final StandaloneModelKey<BlockStateModel> MODEL = new StandaloneModelKey<>(
        () -> "AnvilCraft: FE Collector Head Model"
    );

    @SuppressWarnings("unused")
    public FeCollectorRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public PowerGeneratorRenderState createRenderState() {
        return new PowerGeneratorRenderState();
    }

    @Override
    protected StandaloneModelKey<BlockStateModel> getModel() {
        return FeCollectorRenderer.MODEL;
    }

    @Override
    protected float elevation() {
        return 0.68F;
    }

    @Override
    protected float rotation(FeCollectorBlockEntity be, float partialTick) {
        return be.getRotation() + (float) (Math.log(be.getServerPower() + 1) * 2.5F * partialTick);
    }
}
