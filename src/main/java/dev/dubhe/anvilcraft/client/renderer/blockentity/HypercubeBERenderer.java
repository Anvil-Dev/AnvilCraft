package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.HypercubeBlockEntity;
import dev.dubhe.anvilcraft.client.init.ModRenderTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.neoforged.neoforge.client.model.data.ModelData;

public class HypercubeBERenderer implements BlockEntityRenderer<HypercubeBlockEntity> {
    private static final ModelResourceLocation HYPERCUBE_MODEL = ModelResourceLocation.standalone(AnvilCraft.of("block/hypercube"));

    public HypercubeBERenderer(BlockEntityRendererProvider.Context ignore) {
    }

    @Override
    public void render(
        HypercubeBlockEntity be,
        float partialTick,
        PoseStack pose,
        MultiBufferSource source,
        int packedLight,
        int packedOverlay
    ) {
        ModelManager modelManager = Minecraft.getInstance().getModelManager();
        BakedModel model = modelManager.getModel(HYPERCUBE_MODEL);
        if (model == modelManager.getMissingModel()) return;

        VertexConsumer consumer = source.getBuffer(ModRenderTypes.HYPERCUBE);
        Minecraft.getInstance()
            .getBlockRenderer()
            .getModelRenderer()
            .renderModel(
                pose.last(),
                consumer,
                null,
                model,
                1.0f, 1.0f, 1.0f,
                packedLight,
                packedOverlay,
                ModelData.EMPTY,
                ModRenderTypes.HYPERCUBE
            );
    }
}
