package dev.dubhe.anvilcraft.client.event;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.EmptyBufferSource;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.EmptyOutlineBufferSource;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.hammer.IHasHammerEffect;
import dev.dubhe.anvilcraft.block.multipart.IMultiPartBlockModelHolder.ModelRenderTarget;
import dev.dubhe.anvilcraft.client.support.HammerPreviewEntity;
import dev.dubhe.anvilcraft.client.support.HammerPreviewState;
import dev.dubhe.anvilcraft.integration.iris.IrisState;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.RandomSource;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import net.neoforged.neoforge.event.level.LevelEvent;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@EventBusSubscriber(Dist.CLIENT)
public class HammerEffectRenderEventListener {
    public static final Pair<Direction, Component>[] DIRECTION_TEXTS;
    public static final StandaloneModelKey<BlockStateModel> MODEL = new StandaloneModelKey<>(
        () -> "AnvilCraft: Axis Block Model"
    );
    private static final ContextKey<HammerRenderState> HAMMER_STATE = new ContextKey<>(AnvilCraft.of("hammer_state"));

    private static @Nullable GhostPass ghostPass;
    private static @Nullable HammerRenderState deferred;

    static {
        Pair<Direction, Component>[] texts = Util.cast(new Pair[Direction.values().length - 2]);
        int idx = 0;
        for (int i = 0; i < Direction.values().length; i++) {
            Direction direction = Direction.values()[i];
            MutableComponent component = Component.translatable("screen.anvilcraft.button.direction." + direction.getName());
            if (direction.getStepY() != 0) {
                continue;
            }
            texts[idx++] = Pair.of(direction, component);
        }
        DIRECTION_TEXTS = texts;
    }

    @SubscribeEvent
    public static void onExtract(ExtractLevelRenderStateEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (IrisState.isRenderingShadows() || !(mc.screen instanceof IHasHammerEffect effect) || !effect.shouldRender()) {
            return;
        }
        var frame = event.getRenderState();
        var outline = frame.blockOutlineRenderState;
        if (outline != null && HammerPreviewState.hidesBlock(outline.pos())) {
            frame.blockOutlineRenderState = null;
        }
        var level = event.getLevel();
        var model = new ModelRenderTarget(effect.renderingBlockPos(), effect.renderingBlockState());
        var originalPos = effect.hiddenBlockPos();
        var original = new ModelRenderTarget(originalPos, level.getBlockState(originalPos));
        var sources = original.parts();
        var targets = model.parts();
        if (!original.state().is(model.state().getBlock()) || sources.size() != targets.size()) {
            return;
        }
        List<GhostPart> parts = new ArrayList<>();
        var models = mc.getModelManager();
        float partialTick = event.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        for (int index = 0; index < targets.size(); index++) {
            var target = targets.get(index);
            var source = sources.get(index);
            if (!level.getBlockState(source.pos()).is(source.state().getBlock())) {
                continue;
            }
            List<BlockStateModelPart> geometry = new ArrayList<>();
            BlockState state = target.state();
            models.getBlockStateModelSet().get(state).collectParts(level, target.pos(), state,
                RandomSource.create(state.getSeed(target.pos())), geometry);
            var entity = HammerPreviewEntity.extract(level.getBlockEntity(source.pos()), state, partialTick, event.getCamera(), false);
            parts.add(new GhostPart(target.pos(), geometry, entity));
        }
        List<BlockStateModelPart> axes = new ArrayList<>();
        BlockStateModel axisModel = models.getStandaloneModel(MODEL);
        if (axisModel != null) {
            axisModel.collectParts(level, model.pos(), model.state(), RandomSource.create(42), axes);
            parts.add(new GhostPart(model.pos(), axes, null));
        }
        frame.setRenderData(HAMMER_STATE, new HammerRenderState(effect.renderType(), parts));
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent.AfterTranslucentFeatures event) {
        var level = event.getLevelRenderState();
        HammerRenderState state = level.getRenderData(HAMMER_STATE);
        if (IrisState.isRenderingShadows()) {
            return;
        }
        if (IrisState.isShaderEnabled()) {
            deferred = state;
        } else {
            draw(state, event);
        }
    }

    @SubscribeEvent
    public static void afterLevel(RenderLevelStageEvent.AfterLevel event) {
        final var state = deferred;
        deferred = null;
        if (!IrisState.isShaderEnabled() || IrisState.isRenderingShadows()) {
            return;
        }
        var matrices = RenderSystem.getModelViewStack();
        matrices.pushMatrix().set(event.getModelViewMatrix());
        try {
            draw(state, event);
        } finally {
            matrices.popMatrix();
        }
    }

    @SubscribeEvent
    public static void unload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ClientLevel) {
            clear();
        }
    }

    @SubscribeEvent
    public static void reload(ModelEvent.BakingCompleted event) {
        clear();
        HammerGhostBuffers.clearLayers();
    }

    private static void clear() {
        deferred = null;
        if (ghostPass != null) {
            ghostPass.close();
            ghostPass = null;
        }
    }

    private static void draw(@Nullable HammerRenderState state, RenderLevelStageEvent event) {
        if (ghostPass != null && (state == null || state.renderType() != ghostPass.type)) {
            ghostPass.close();
            ghostPass = null;
        }
        if (state == null) {
            return;
        }
        if (ghostPass == null) {
            ghostPass = new GhostPass(state.renderType());
        }
        ghostPass.draw(state.parts(), event.getPoseStack(), event.getLevelRenderState().cameraRenderState);
    }

    private record GhostPart(BlockPos pos, List<BlockStateModelPart> geometry, @Nullable HammerPreviewEntity entity) {
    }

    private record HammerRenderState(RenderType renderType, List<GhostPart> parts) {
    }

    private static final class GhostPass implements AutoCloseable {
        private final RenderType type;
        private final HammerGhostBuffers buffers;
        private final SubmitNodeStorage nodes = new SubmitNodeStorage();
        private final FeatureRenderDispatcher features;

        GhostPass(RenderType type) {
            this.type = type;
            this.buffers = new HammerGhostBuffers(type);
            var client = Minecraft.getInstance();
            this.features = new FeatureRenderDispatcher(this.nodes, client.getModelManager(), this.buffers, client.getAtlasManager(),
                EmptyOutlineBufferSource.INSTANCE, EmptyBufferSource.INSTANCE, client.font, client.gameRenderer.getGameRenderState());
        }

        void draw(List<GhostPart> parts, PoseStack pose, CameraRenderState camera) {
            for (var part : parts) {
                pose.pushPose();
                try {
                    BlockPos pos = part.pos();
                    pose.translate(pos.getX() - camera.pos.x, pos.getY() - camera.pos.y, pos.getZ() - camera.pos.z);
                    this.nodes.submitBlockModel(pose, this.type, part.geometry(), BlockModelRenderState.EMPTY_TINTS,
                        LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
                    if (part.entity() != null) {
                        part.entity().submit(pose, this.nodes, camera);
                    }
                } finally {
                    pose.popPose();
                }
            }
            this.features.renderAllFeatures();
            this.buffers.endBatch();
            this.features.endFrame();
            this.nodes.endFrame();
        }

        @Override
        public void close() {
            this.features.close();
            this.buffers.sharedBuffer.close();
        }
    }
}
