package dev.dubhe.anvilcraft.client.renderer.blockentity;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.CreativeGeneratorBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;

public class CreativeGeneratorRenderer extends PowerProducerRenderer<CreativeGeneratorBlockEntity> {
    public static final ModelResourceLocation MODEL = ModelResourceLocation.standalone(
        AnvilCraft.of("block/creative_generator_cube")
    );

    /**
     * 创造发电机渲染
     */
    public CreativeGeneratorRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    protected float elevation() {
        return 0.75f;
    }

    @Override
    protected ModelResourceLocation getModel() {
        return MODEL;
    }
}
