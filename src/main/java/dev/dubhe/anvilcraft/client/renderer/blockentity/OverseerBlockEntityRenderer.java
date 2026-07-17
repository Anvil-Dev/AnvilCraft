package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.block.OverseerBlock;
import dev.dubhe.anvilcraft.block.entity.OverseerBlockEntity;
import dev.dubhe.anvilcraft.block.state.Vertical3PartHalf;
import dev.dubhe.anvilcraft.init.ModParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.data.ModelData;

import java.util.Map;
import java.util.WeakHashMap;

public class OverseerBlockEntityRenderer implements BlockEntityRenderer<OverseerBlockEntity> {
    private static final float HEAD_ROTATION_DEGREES_PER_TICK = 0.6f;
    private static final float HEAD_BOB_AMPLITUDE = 0.035f;
    private static final float HEAD_BOB_ANGULAR_SPEED = (float) (Math.PI * 2.0 / 160.0);
    private static final double HEAD_MIN_Y = -0.5;
    private static final double HEAD_MAX_Y = 2.0;
    private final Map<OverseerBlockEntity, Long> lastParticleTicks = new WeakHashMap<>();

    public OverseerBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
        OverseerBlockEntity blockEntity,
        float partialTick,
        PoseStack poseStack,
        MultiBufferSource buffer,
        int packedLight,
        int packedOverlay
    ) {
        Level level = blockEntity.getLevel();
        BlockState state = blockEntity.getBlockState();
        if (level == null
            || state.getValue(OverseerBlock.HALF) != Vertical3PartHalf.MID
            || state.getValue(OverseerBlock.LEVEL) != OverseerBlock.MAX_LEVEL) {
            return;
        }

        float time = level.getGameTime() + partialTick;
        float bobOffset = getHeadBobOffset(time);
        spawnTrailParticles(blockEntity, level);

        poseStack.pushPose();
        poseStack.translate(0.5, bobOffset, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(time * HEAD_ROTATION_DEGREES_PER_TICK));
        poseStack.translate(-0.5, 0, -0.5);

        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        BakedModel model = dispatcher.getBlockModel(state);
        RandomSource random = RandomSource.create(state.getSeed(blockEntity.getBlockPos()));
        ChunkRenderTypeSet renderTypes = model.getRenderTypes(state, random, ModelData.EMPTY);
        for (RenderType renderType : renderTypes.asList()) {
            dispatcher.getModelRenderer().renderModel(
                poseStack.last(),
                buffer.getBuffer(renderType),
                state,
                model,
                1.0f,
                1.0f,
                1.0f,
                packedLight,
                packedOverlay
            );
        }
        poseStack.popPose();
    }

    private static float getHeadBobOffset(float time) {
        return Mth.sin(time * HEAD_BOB_ANGULAR_SPEED) * HEAD_BOB_AMPLITUDE;
    }

    private void spawnTrailParticles(OverseerBlockEntity blockEntity, Level level) {
        long gameTime = level.getGameTime();
        Long previousParticleTick = this.lastParticleTicks.put(blockEntity, gameTime);
        if (previousParticleTick != null && previousParticleTick == gameTime) return;

        float currentBob = getHeadBobOffset(gameTime);
        float previousBob = getHeadBobOffset(gameTime - 1.0f);
        float movement = currentBob - previousBob;
        if (Math.abs(movement) < 0.0001f) return;

        BlockPos pos = blockEntity.getBlockPos();
        double trailY = pos.getY() + previousBob + (movement > 0 ? HEAD_MIN_Y : HEAD_MAX_Y);
        for (int i = 0; i < 2; i++) {
            double x = pos.getX() + 0.125 + level.random.nextDouble() * 0.75;
            double z = pos.getZ() + 0.125 + level.random.nextDouble() * 0.75;
            double driftX = (level.random.nextDouble() - 0.5) * 0.006;
            double driftZ = (level.random.nextDouble() - 0.5) * 0.006;
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
