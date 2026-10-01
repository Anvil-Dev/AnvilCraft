package dev.dubhe.anvilcraft.client.event;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.hammer.IHasHammerEffect;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.RandomSource;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

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

    static {
        Pair<Direction, Component>[] texts = Util.cast(new Pair[Direction.values().length - 2]);
        int idx = 0;
        for (int i = 0; i < Direction.values().length; i++) {
            Direction direction = Direction.values()[i];
            MutableComponent component = Component.literal(direction.getName());
            if (direction.getStepY() != 0) continue;
            texts[idx++] = Pair.of(direction, component);
        }
        DIRECTION_TEXTS = texts;
    }

    @SubscribeEvent
    public static void onExtract(ExtractLevelRenderStateEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !(mc.screen instanceof IHasHammerEffect effect) || !effect.shouldRender()) return;
        BlockState state = effect.renderingBlockState();
        BlockPos pos = effect.renderingBlockPos().immutable();
        List<BlockStateModelPart> parts = new ArrayList<>();
        mc.getModelManager().getBlockStateModelSet().get(state).collectParts(
            mc.level, pos, state, RandomSource.create(42), parts);
        List<BlockStateModelPart> axes = new ArrayList<>();
        BlockStateModel axisModel = mc.getModelManager().getStandaloneModel(HammerEffectRenderEventListener.MODEL);
        if (axisModel != null) axisModel.collectParts(mc.level, pos, state, RandomSource.create(42), axes);
        event.getRenderState().setRenderData(HammerEffectRenderEventListener.HAMMER_STATE,
            new HammerRenderState(pos, effect.renderType(), List.copyOf(parts), List.copyOf(axes)));
    }

    @SubscribeEvent
    public static void onRender(SubmitCustomGeometryEvent event) {
        LevelRenderState renderState = event.getLevelRenderState();
        HammerRenderState state = renderState.getRenderData(HammerEffectRenderEventListener.HAMMER_STATE);
        if (state == null) return;
        BlockPos pos = state.pos();
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        CameraRenderState camera = renderState.cameraRenderState;
        Vec3 cameraPos = camera.pos;
        poseStack.translate(pos.getX() - cameraPos.x - 0.0005, pos.getY() - cameraPos.y - 0.0005, pos.getZ() - cameraPos.z - 0.0005);
        poseStack.scale(1.001F, 1.001F, 1.001F);
        var collector = event.getSubmitNodeCollector();
        collector.submitBlockModel(poseStack, state.renderType(), state.parts(), BlockModelRenderState.EMPTY_TINTS,
            LightCoordsUtil.pack(15, 0), OverlayTexture.NO_OVERLAY, 0);
        collector.submitBlockModel(poseStack, state.renderType(), state.axes(), BlockModelRenderState.EMPTY_TINTS,
            LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }

    private record HammerRenderState(BlockPos pos, RenderType renderType, List<BlockStateModelPart> parts,
                                     List<BlockStateModelPart> axes) {
    }
}
