package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.block.entity.HypercubeBlockEntity;
import dev.dubhe.anvilcraft.client.init.ModRenderTypes;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.HypercubeRenderState;
import dev.dubhe.anvilcraft.client.selection.ModelSelectionRenderer;
import dev.dubhe.anvilcraft.client.support.FeatureRendererSupport;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import org.jspecify.annotations.Nullable;

public class HypercubeBERenderer
    implements BlockEntityRenderer<HypercubeBlockEntity, HypercubeRenderState>, ModelSelectionRenderer<HypercubeBlockEntity> {
    public static final StandaloneModelKey<BlockStateModel> MODEL = new StandaloneModelKey<>(() -> "AnvilCraft: Hypercube");

    public HypercubeBERenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public HypercubeRenderState createRenderState() {
        return new HypercubeRenderState();
    }

    @Override
    public void collectSelectionModels(HypercubeBlockEntity entity, float partialTick, PoseStack pose, ModelConsumer consumer) {
        consumer.accept(MODEL, pose);
    }

    @Override
    public void extractRenderState(
        HypercubeBlockEntity entity, HypercubeRenderState state, float partialTick,
        Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(entity, state, partialTick, cameraPosition, breakProgress);
        state.model = FeatureRendererSupport.initialize(MODEL, entity, true);
    }

    @Override
    public void submit(HypercubeRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        state.model.submitModel(ModRenderTypes.HYPERCUBE, pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
    }
}
