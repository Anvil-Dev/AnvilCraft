package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.block.entity.OverseerBlockEntity;
import dev.dubhe.anvilcraft.block.state.Vertical3PartHalf;
import dev.dubhe.anvilcraft.block.utility.OverseerBlock;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.OverseerRenderState;
import dev.dubhe.anvilcraft.client.support.FeatureRendererSupport;
import dev.dubhe.anvilcraft.init.ModParticles;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.WeakHashMap;

public class OverseerBlockEntityRenderer implements BlockEntityRenderer<OverseerBlockEntity, OverseerRenderState> {
    private static final float HEAD_ROTATION_DEGREES_PER_TICK = 0.6F;
    private static final float HEAD_BOB_AMPLITUDE = 0.035F;
    private static final float HEAD_BOB_ANGULAR_SPEED = (float) (Math.PI * 2.0 / 160.0);
    private static final double HEAD_MIN_Y = -0.5;
    private static final double HEAD_MAX_Y = 2.0;
    private final Map<OverseerBlockEntity, Long> lastParticleTicks = new WeakHashMap<>();

    public OverseerBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public OverseerRenderState createRenderState() {
        return new OverseerRenderState();
    }

    @Override
    public void extractRenderState(
        OverseerBlockEntity blockEntity,
        OverseerRenderState state,
        float partialTicks,
        Vec3 cameraPosition,
        ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        state.setModel(null);
        Level level = blockEntity.getLevel();
        BlockState blockState = blockEntity.getBlockState();
        if (level == null
            || blockState.getValue(OverseerBlock.HALF) != Vertical3PartHalf.MID
            || blockState.getValue(OverseerBlock.LEVEL) != OverseerBlock.MAX_LEVEL) {
            return;
        }

        float time = level.getGameTime() + partialTicks;
        state.setTime(time);
        state.setBobOffset(OverseerBlockEntityRenderer.getHeadBobOffset(time));
        state.setModel(FeatureRendererSupport.initialize(blockState, blockEntity));
        this.spawnTrailParticles(blockEntity, level);
    }

    @Override
    public void submit(
        OverseerRenderState state,
        PoseStack poseStack,
        SubmitNodeCollector submitNodeCollector,
        CameraRenderState camera
    ) {
        BlockModelRenderState model = state.getModel();
        if (model == null) return;

        poseStack.pushPose();
        poseStack.translate(0.5, state.getBobOffset(), 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.getTime() * OverseerBlockEntityRenderer.HEAD_ROTATION_DEGREES_PER_TICK));
        poseStack.translate(-0.5, 0, -0.5);
        model.submit(
            poseStack,
            submitNodeCollector,
            state.lightCoords,
            OverlayTexture.NO_OVERLAY,
            0
        );
        poseStack.popPose();
    }

    private static float getHeadBobOffset(float time) {
        return Mth.sin(time * OverseerBlockEntityRenderer.HEAD_BOB_ANGULAR_SPEED) * OverseerBlockEntityRenderer.HEAD_BOB_AMPLITUDE;
    }

    private void spawnTrailParticles(OverseerBlockEntity blockEntity, Level level) {
        long gameTime = level.getGameTime();
        Long previousParticleTick = this.lastParticleTicks.put(blockEntity, gameTime);
        if (previousParticleTick != null && previousParticleTick == gameTime) return;

        float currentBob = OverseerBlockEntityRenderer.getHeadBobOffset(gameTime);
        float previousBob = OverseerBlockEntityRenderer.getHeadBobOffset(gameTime - 1.0F);
        float movement = currentBob - previousBob;
        if (Math.abs(movement) < 0.0001F) return;

        BlockPos pos = blockEntity.getBlockPos();
        double trailY = pos.getY() + previousBob + (movement > 0 ? OverseerBlockEntityRenderer.HEAD_MIN_Y : OverseerBlockEntityRenderer.HEAD_MAX_Y);
        for (int i = 0; i < 2; i++) {
            double x = pos.getX() + 0.125 + level.getRandom().nextDouble() * 0.75;
            double z = pos.getZ() + 0.125 + level.getRandom().nextDouble() * 0.75;
            double driftX = (level.getRandom().nextDouble() - 0.5) * 0.006;
            double driftZ = (level.getRandom().nextDouble() - 0.5) * 0.006;
            level.addParticle(
                ModParticles.OVERSEER_TRAIL.get(),
                x,
                trailY,
                z,
                driftX,
                -movement * 0.5,
                driftZ
            );
        }
    }

    @Override
    public AABB getRenderBoundingBox(OverseerBlockEntity blockEntity) {
        BlockPos pos = blockEntity.getBlockPos();
        return AABB.ofSize(new Vec3(pos.getX() + 0.5, pos.getY() + 0.75, pos.getZ() + 0.5), 1.5, 2.75, 1.5);
    }
}
