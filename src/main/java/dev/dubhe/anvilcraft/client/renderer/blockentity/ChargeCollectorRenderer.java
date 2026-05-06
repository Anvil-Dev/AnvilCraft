package dev.dubhe.anvilcraft.client.renderer.blockentity;

import dev.dubhe.anvilcraft.block.entity.ChargeCollectorBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.PowerGeneratorRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

public class ChargeCollectorRenderer extends PowerProducerRenderer<ChargeCollectorBlockEntity, PowerGeneratorRenderState> {
    public static final StandaloneModelKey<BlockStateModel> CUBE = new StandaloneModelKey<>(
        () -> "AnvilCraft: Charge Collector Cube Model"
    );

    public ChargeCollectorRenderer(BlockEntityRendererProvider.Context ignored) {
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
    protected float rotation(ChargeCollectorBlockEntity be, float partialTick) {
        return be.getRotation() + (float) (Math.log(be.getServerPower() + 1) * 2.5F * partialTick);
    }

    @Override
    protected StandaloneModelKey<BlockStateModel> getModel() {
        return CUBE;
    }
}
