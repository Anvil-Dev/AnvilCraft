package dev.dubhe.anvilcraft.client.renderer;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialTravelManager;
import dev.dubhe.anvilcraft.client.AnvilCraftClient;
import dev.dubhe.anvilcraft.client.init.ModRenderTypes;
import dev.dubhe.anvilcraft.client.renderer.blockentity.CelestialForgingAnvilBlockEntityRenderer;
import dev.dubhe.anvilcraft.client.support.OverworldLikeClientState;
import dev.dubhe.anvilcraft.saved.OverworldLikeWorldState;
import dev.dubhe.anvilcraft.worldgen.OverworldLikeOrbitMath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import javax.annotation.Nullable;

/** Draws the generation-global amplified rings as world-space sky geometry. */
public final class OverworldLikeOrbitalSkyRenderer {
    private static final float SKY_SCALE = 1200.0F;
    private static @Nullable BakedModel ring4;
    private static @Nullable BakedModel ring5;

    private OverworldLikeOrbitalSkyRenderer() {
    }

    public static void cacheModels(ModelEvent.BakingCompleted event) {
        ring4 = event.getModels().get(CelestialForgingAnvilBlockEntityRenderer.R4);
        ring5 = event.getModels().get(CelestialForgingAnvilBlockEntityRenderer.R5);
    }

    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY || !AnvilCraftClient.CONFIG.renderOverworldLikeSky) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        boolean overworldLike = CelestialTravelManager.isOverworldLike(minecraft.level.dimension());
        if (!overworldLike && !CelestialTravelManager.VOID_PLANET_LEVEL.equals(minecraft.level.dimension())) return;
        if (overworldLike && (!OverworldLikeClientState.isInitialized()
            || OverworldLikeClientState.phase() == OverworldLikeWorldState.Phase.RESET_PENDING)) {
            return;
        }
        if (ring4 == null || ring5 == null) return;

        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(minecraft.isPaused());
        long gameTime = minecraft.level.getGameTime();
        long orbitEpoch = overworldLike ? OverworldLikeClientState.orbitEpochGameTime() : 0L;
        long visualSeed = overworldLike ? OverworldLikeClientState.visualSeed() : 0L;
        float eclipse = overworldLike ? OverworldLikeClientState.eclipseFactor(minecraft.level) : 0.0F;
        float collapse = overworldLike ? OverworldLikeClientState.collapseProgress() : 0.0F;
        float brightness = 0.34F + eclipse * 0.22F + collapse * 0.44F;

        PoseStack poseStack = new PoseStack();
        // AFTER_SKY supplies an empty pose stack, so sky geometry must include the camera rotation explicitly.
        poseStack.mulPose(event.getModelViewMatrix());
        ModelBlockRenderer renderer = minecraft.getBlockRenderer().getModelRenderer();
        // Iris defers the shared entity buffer even after endBatch(RenderType); sky geometry must draw in this stage.
        BufferBuilder consumer = Tesselator.getInstance().begin(
            ModRenderTypes.OVERWORLD_LIKE_SKY_RING.mode(), ModRenderTypes.OVERWORLD_LIKE_SKY_RING.format()
        );

        poseStack.pushPose();
        poseStack.scale(SKY_SCALE, SKY_SCALE, SKY_SCALE);
        OverworldLikeOrbitMath.RingPose outer = OverworldLikeOrbitMath.ringPose(
            6,
            gameTime,
            partialTick,
            orbitEpoch,
            visualSeed
        );
        poseStack.mulPose(Axis.YP.rotationDegrees((float) -outer.outerRotation()));
        poseStack.mulPose(Axis.XP.rotationDegrees(14.5108F));
        poseStack.mulPose(Axis.YP.rotationDegrees(-3.8411F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(14.5109F));

        OverworldLikeOrbitMath.RingPose middle = OverworldLikeOrbitMath.ringPose(
            5,
            gameTime,
            partialTick,
            orbitEpoch,
            visualSeed
        );
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F + (float) middle.middleRotation()));
        renderModel(renderer, poseStack, consumer, ring5, brightness * 0.92F);

        OverworldLikeOrbitMath.RingPose inner = OverworldLikeOrbitMath.ringPose(
            4,
            gameTime,
            partialTick,
            orbitEpoch,
            visualSeed
        );
        poseStack.mulPose(Axis.ZP.rotationDegrees((float) inner.innerRotation()));
        renderModel(renderer, poseStack, consumer, ring4, brightness * 0.86F);
        poseStack.popPose();
        ModRenderTypes.OVERWORLD_LIKE_SKY_RING.draw(consumer.buildOrThrow());
    }

    private static void renderModel(
        ModelBlockRenderer renderer,
        PoseStack poseStack,
        VertexConsumer consumer,
        BakedModel model,
        float brightness
    ) {
        renderer.renderModel(
            poseStack.last(),
            consumer,
            null,
            model,
            brightness,
            brightness,
            brightness,
            LightTexture.FULL_BRIGHT,
            OverlayTexture.NO_OVERLAY
        );
    }
}
