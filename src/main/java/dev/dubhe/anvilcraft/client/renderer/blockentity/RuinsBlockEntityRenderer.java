package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.lib.v2.cube.client.SelectionPart;
import dev.dubhe.anvilcraft.block.RuinsBlock;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import dev.dubhe.anvilcraft.block.entity.RuinsBlockEntity;
import dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.RuinsRenderState;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.WorldFluidRenderState;
import dev.dubhe.anvilcraft.client.selection.ModelBlockSelection;
import dev.dubhe.anvilcraft.client.selection.ModelSelectionBlacklist;
import dev.dubhe.anvilcraft.client.selection.ModelSelectionRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public class RuinsBlockEntityRenderer implements BlockEntityRenderer<RuinsBlockEntity, RuinsRenderState>,
    ModelSelectionRenderer<RuinsBlockEntity> {
    private final Cache<BlockState, AABB> structureBounds = CacheBuilder.newBuilder().maximumSize(512).build();
    private final Cache<BlockStateModelPart, AABB> modelBounds = CacheBuilder.newBuilder().weakKeys().maximumSize(512).build();
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
    public void collectSelectionParts(RuinsBlockEntity ruins, float partialTick, List<SelectionPart> output) {
        Level level = ruins.getLevel();
        if (level == null) return;
        try (var ignored = RuinsRenderContext.enter(level)) {
            BlockEntity display = this.prepareDisplay(ruins);
            if (display != null && !ModelSelectionBlacklist.excludesBlockEntity(display.getBlockState().getBlock())) {
                output.addAll(ModelBlockSelection.rendererParts(display, partialTick));
            }
        }
    }

    public static void updateVisibility(RuinsBlockEntity ruins) {
        if (!(ruins.getLevel() instanceof ClientLevel level)
            || level.getChunkAt(ruins.getBlockPos()).getBlockEntity(ruins.getBlockPos()) != ruins) return;
        Object renderer = Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(ruins);
        if (renderer instanceof RuinsBlockEntityRenderer display && display.shouldRenderOffScreen(ruins)) {
            level.getGloballyRenderedBlockEntities().add(ruins);
        } else {
            level.getGloballyRenderedBlockEntities().remove(ruins);
        }
    }

    public boolean shouldRenderOffScreen(RuinsBlockEntity ruins) {
        if (ruins.getDisplayState().getBlock() instanceof AbstractMultiPartBlock<?>) return true;
        BlockEntity display = ruins.getDisplayEntity();
        var renderer = display == null ? null : Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(display);
        return renderer != null && renderer.shouldRenderOffScreen();
    }

    @Override
    public AABB getRenderBoundingBox(RuinsBlockEntity ruins) {
        AABB bounds = new AABB(ruins.getBlockPos());
        if (!(ruins.getLevel() instanceof ClientLevel level)) return bounds;
        try (var ignored = RuinsRenderContext.enter(level)) {
            BlockState state = ruins.getDisplayState();
            if (state.getRenderShape() == RenderShape.MODEL) {
                var model = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(state);
                List<BlockStateModelPart> parts = new ArrayList<>();
                model.collectParts(level, ruins.getBlockPos(), state,
                    RandomSource.create(state.getSeed(ruins.getBlockPos())), parts);
                for (var part : parts) {
                    bounds = bounds.minmax(this.staticBounds(part).move(ruins.getBlockPos()).move(state.getOffset(ruins.getBlockPos())));
                }
            }
            if (state.getBlock() instanceof AbstractMultiPartBlock<?> multipart) {
                bounds = bounds.minmax(this.multipartBounds(multipart, state).move(ruins.getBlockPos()));
            }
            BlockEntity display = ruins.getDisplayEntity();
            var renderer = display == null ? null : Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(display);
            return renderer == null ? bounds : bounds.minmax(renderer.getRenderBoundingBox(display));
        }
    }

    private <P extends Enum<P>> AABB multipartBounds(AbstractMultiPartBlock<P> block, BlockState state) {
        AABB cached = this.structureBounds.getIfPresent(state);
        if (cached != null) return cached;
        AABB bounds = new AABB(BlockPos.ZERO);
        var shape = block.getMultiPartShape(state);
        if (!shape.isEmpty()) bounds = bounds.minmax(shape.bounds());
        for (var part : ModelBlockSelection.multipartOutline(state)) bounds = bounds.minmax(part.bounds());
        this.structureBounds.put(state, bounds);
        return bounds;
    }

    private AABB staticBounds(BlockStateModelPart part) {
        AABB cached = this.modelBounds.getIfPresent(part);
        if (cached != null) return cached;
        AABB bounds = new AABB(BlockPos.ZERO);
        Direction[] directions = Direction.values();
        for (int side = 0; side <= directions.length; side++) {
            for (var quad : part.getQuads(side == directions.length ? null : directions[side])) {
                for (int vertex = 0; vertex < 4; vertex++) {
                    var pos = quad.position(vertex);
                    bounds = bounds.minmax(new AABB(pos.x(), pos.y(), pos.z(), pos.x(), pos.y(), pos.z()));
                }
            }
        }
        this.modelBounds.put(part, bounds);
        return bounds;
    }

    @Override
    public boolean shouldRender(RuinsBlockEntity ruins, Vec3 camera) {
        if (this.getRenderBoundingBox(ruins).distanceToSqr(camera) < this.getViewDistance() * this.getViewDistance()) return true;
        BlockEntity display = ruins.getDisplayEntity();
        var renderer = display == null ? null : Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(display);
        return renderer != null && renderer.shouldRender(display, camera);
    }

    @Override
    public int getViewDistance() {
        return Math.max(64, Minecraft.getInstance().options.getEffectiveRenderDistance() * 16);
    }

    @Override
    public void submit(RuinsRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.fluid != null) state.fluid.submit(pose, collector);
        state.submitGeometry(pose, collector);
        if (state.display != null) state.display.submit(pose, collector, camera);
    }
}
