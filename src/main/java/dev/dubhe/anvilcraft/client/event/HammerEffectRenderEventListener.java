package dev.dubhe.anvilcraft.client.event;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.hammer.IHasHammerEffect;
import dev.dubhe.anvilcraft.mixin.accessor.LevelRendererAccessor;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.RandomSource;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import org.joml.Matrix4f;

@Slf4j
@EventBusSubscriber(Dist.CLIENT)
public class HammerEffectRenderEventListener {
    public static final Pair<Direction, Component>[] DIRECTION_TEXTS;
    public static final StandaloneModelKey<BlockStateModel> MODEL = new StandaloneModelKey<>(
        () -> "AnvilCraft: Axis Block Model"
    );
    private static final ContextKey<BlockModelRenderState> HAMMER_STATE = new ContextKey<>(AnvilCraft.of("hammer_state"));

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
        if (!(mc.screen instanceof IHasHammerEffect hasHammerEffect)) return;
        if (!hasHammerEffect.shouldRender()) return;
        BlockModelRenderState model = new BlockModelRenderState();
        BlockState state = hasHammerEffect.renderingBlockState();
        mc.getModelManager().getBlockStateModelSet().get(state).collectParts(
            mc.level,
            hasHammerEffect.renderingBlockPos(),
            state,
            RandomSource.create(),
            model.setupModel(new Matrix4f(), false)
        );
        event.getRenderState().setRenderData(HammerEffectRenderEventListener.HAMMER_STATE, model);
    }


    //TODO use custom render type for colored overlay
    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent.AfterOpaqueFeatures event) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.screen instanceof IHasHammerEffect hasHammerEffect)) return;
        if (!hasHammerEffect.shouldRender()) return;
        BlockPos pos = hasHammerEffect.renderingBlockPos();
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        LevelRenderState renderState = event.getLevelRenderState();
        CameraRenderState camera = renderState.cameraRenderState;
        Vec3 cameraPos = camera.pos;
        poseStack.translate(
            pos.getX() - cameraPos.x - 0.0005,
            pos.getY() - cameraPos.y - 0.0005,
            pos.getZ() - cameraPos.z - 0.0005
        );
        poseStack.scale(1.001F, 1.001F, 1.001F);
        BlockModelRenderState model = renderState.getRenderData(HammerEffectRenderEventListener.HAMMER_STATE);
        LevelRendererAccessor accessor = Util.cast(event.getLevelRenderer());
        model.submit(poseStack, accessor.getSubmitNodeStorage(), 1, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }
}
