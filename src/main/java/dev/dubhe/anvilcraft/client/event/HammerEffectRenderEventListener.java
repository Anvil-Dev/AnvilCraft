package dev.dubhe.anvilcraft.client.event;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.hammer.IHasHammerEffect;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@EventBusSubscriber(value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME, modid = AnvilCraft.MOD_ID)
public class HammerEffectRenderEventListener {

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) return;
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.screen instanceof IHasHammerEffect hasHammerEffect))return;
        if (!hasHammerEffect.shouldRender()) return;
        BlockPos pos = hasHammerEffect.renderingBlockPos();
        BlockState state = hasHammerEffect.renderingBlockState();
        RenderType renderType = hasHammerEffect.renderType();
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        Camera camera = event.getCamera();
        Vec3 cameraPos = camera.getPosition();
        poseStack.translate(
            pos.getX() - cameraPos.x - 0.0005,
            pos.getY() - cameraPos.y - 0.0005,
            pos.getZ() - cameraPos.z - 0.0005
        );
        poseStack.scale(1.001f, 1.001f, 1.001f);
        BakedModel model = mc.getBlockRenderer().getBlockModel(state);
        ModelBlockRenderer renderer = mc.getBlockRenderer().getModelRenderer();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        VertexConsumer vertexConsumer = bufferSource.getBuffer(renderType);
        renderer.renderModel(
            poseStack.last(),
            vertexConsumer,
            state,
            model,
            1f,
            1f,
            1f,
            LightTexture.FULL_BLOCK,
            OverlayTexture.NO_OVERLAY
        );
        poseStack.popPose();
    }
}
