package dev.dubhe.anvilcraft.client.renderer.blockentity;

import dev.dubhe.anvilcraft.block.entity.CreativeGeneratorBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.PowerGeneratorRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

public class CreativeGeneratorRenderer extends PowerProducerRenderer<CreativeGeneratorBlockEntity, PowerGeneratorRenderState> {
    public static final StandaloneModelKey<BlockStateModel> HEAD = new StandaloneModelKey<>(
        () -> "AnvilCraft: Creative Generator Cube Model"
    );

    public CreativeGeneratorRenderer(BlockEntityRendererProvider.Context ignored) {
    }

    @Override
    public PowerGeneratorRenderState createRenderState() {
        return new PowerGeneratorRenderState();
    }

    @Override
    protected float magic() {
        return 0.06f;
    }

    @Override
    protected float elevation() {
        return 0.75F;
    }

    @Override
    protected StandaloneModelKey<BlockStateModel> getModel() {
        return CreativeGeneratorRenderer.HEAD;
    }
}
