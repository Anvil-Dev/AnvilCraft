package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.block.RuinsBlock;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import dev.dubhe.anvilcraft.block.entity.RuinsBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.RuinsRenderState;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.WorldFluidRenderState;
import dev.dubhe.anvilcraft.client.selection.ModelSelectionRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.WeakHashMap;

public class RuinsBlockEntityRenderer implements BlockEntityRenderer<RuinsBlockEntity, RuinsRenderState>,
    ModelSelectionRenderer<RuinsBlockEntity> {
    private final Map<BlockEntity, Long> lastVisualTick = new WeakHashMap<>();
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
        state.fluid = null;
        var client = Minecraft.getInstance();
        var level = client.level;
        if (level == null) return;
        try (var ignored = RuinsRenderContext.enter(level)) {
            BlockEntity visual = this.prepareDisplay(entity);
            var display = RuinsBlock.connectedDisplayState(level, entity.getBlockPos(), entity.getDisplayState());
            if (!display.getFluidState().isEmpty()) {
                state.fluid = WorldFluidRenderState.extract(level, entity.getBlockPos(), display);
            }
            if (display.getRenderShape() == RenderShape.MODEL) {
                var model = client.getModelManager().getBlockStateModelSet().get(display);
                var renderer = client.options.ambientOcclusion().get() ? this.ambient : this.flat;
                renderer.tesselateBlock(state::addQuad, 0, 0, 0, level, entity.getBlockPos(), display, model,
                    display.getSeed(entity.getBlockPos()));
            }
            if (visual != null) {
                var renderer = client.getBlockEntityRenderDispatcher().getRenderer(visual);
                if (renderer != null) state.display = capture(renderer, visual, partialTick, camera, breaking);
            }
        }
    }

    @Nullable
    public BlockEntity prepareDisplay(RuinsBlockEntity ruins) {
        Level level = ruins.getLevel();
        BlockEntity display = ruins.getDisplayEntity();
        if (level == null || display == null || !level.isClientSide()) return display;
        Long last = this.lastVisualTick.get(display);
        if (last == null || last != level.getGameTime()) {
            this.lastVisualTick.put(display, level.getGameTime());
            try (var ignored = RuinsRenderContext.enter(level)) {
                if (display instanceof CelestialForgingAnvilBlockEntity anvil) anvil.tickRuinsVisuals();
                else tickDisplay(level, ruins.getDisplayState(), display.getType(), ruins.getBlockPos());
            }
        }
        return display;
    }

    private static <T extends BlockEntity> void tickDisplay(Level level, BlockState state, BlockEntityType<T> type, BlockPos pos) {
        var ticker = state.getTicker(level, type);
        T entity = type.getBlockEntity(level, pos);
        if (ticker != null && entity != null) ticker.tick(level, pos, state, entity);
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
        if (state.fluid != null) state.fluid.submit(pose, collector);
        state.submitGeometry(pose, collector);
        if (state.display != null) state.display.submit(pose, collector, camera);
    }
}
