package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.block.RuinsBlock;
import dev.dubhe.anvilcraft.block.entity.RuinsBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.RuinsRenderState;
import dev.dubhe.anvilcraft.client.selection.ModelSelectionRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RuinsBlockEntityRenderer implements BlockEntityRenderer<RuinsBlockEntity, RuinsRenderState>,
    ModelSelectionRenderer<RuinsBlockEntity> {
    private final ModelBlockRenderer ambient = new ModelBlockRenderer(true, true, Minecraft.getInstance().getBlockColors());
    private final ModelBlockRenderer flat = new ModelBlockRenderer(false, true, Minecraft.getInstance().getBlockColors());

    public RuinsBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public RuinsRenderState createRenderState() {
        return new RuinsRenderState();
    }

    @Override
    public void extractRenderState(RuinsBlockEntity entity, RuinsRenderState state, float partialTick, Vec3 camera,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breaking) {
        BlockEntityRenderer.super.extractRenderState(entity, state, partialTick, camera, breaking);
        state.clearQuads();
        state.display = null;
        var client = Minecraft.getInstance();
        var level = client.level;
        if (level == null) return;
        try (var ignored = RuinsRenderContext.enter(level)) {
            var display = RuinsBlock.connectedDisplayState(level, entity.getBlockPos(), entity.getDisplayState());
            if (display.getRenderShape() == RenderShape.MODEL) {
                var model = client.getModelManager().getBlockStateModelSet().get(display);
                var renderer = client.options.ambientOcclusion().get() ? this.ambient : this.flat;
                renderer.tesselateBlock(state::addQuad, 0, 0, 0, level, entity.getBlockPos(), display, model,
                    display.getSeed(entity.getBlockPos()));
            }
            BlockEntity visual = entity.getDisplayEntity();
            if (visual != null) {
                var renderer = client.getBlockEntityRenderDispatcher().getRenderer(visual);
                if (renderer != null) state.display = capture(renderer, visual, partialTick, camera, breaking);
            }
        }
    }

    private static <T extends BlockEntity, S extends BlockEntityRenderState> RuinsRenderState.DisplayRenderer capture(
        BlockEntityRenderer<T, S> renderer, T entity, float partialTick, Vec3 camera,
        ModelFeatureRenderer.@Nullable CrumblingOverlay breaking
    ) {
        S state = renderer.createRenderState();
        renderer.extractRenderState(entity, state, partialTick, camera, breaking);
        return (pose, collector, view) -> renderer.submit(state, pose, collector, view);
    }

    @Override
    public void collectSelectionModels(RuinsBlockEntity entity, float partialTick, PoseStack pose, ModelConsumer consumer) {
        consumer.accept(entity.getDisplayState(), pose);
    }

    @Override
    public void submit(RuinsRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        state.submitGeometry(pose, collector);
        if (state.display != null) state.display.submit(pose, collector, camera);
    }
}
