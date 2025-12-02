package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.block.CorruptedBeaconBlock;
import dev.dubhe.anvilcraft.block.entity.CorruptedBeaconBlockEntity;
import dev.dubhe.anvilcraft.client.init.ModRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.data.ModelData;

import java.util.List;

public class CorruptedBeaconRenderer implements BlockEntityRenderer<CorruptedBeaconBlockEntity> {
    public static final ResourceLocation BEAM_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/beacon_beam.png");
    public static final int MAX_RENDER_Y = 1024;
    private final BlockRenderDispatcher blockRenderer;
    private final BlockState defaultLightState = Blocks.WHITE_CONCRETE.defaultBlockState();

    @SuppressWarnings("unused")
    public CorruptedBeaconRenderer(BlockEntityRendererProvider.Context context) {
        blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(
        CorruptedBeaconBlockEntity blockEntity,
        float partialTick,
        PoseStack poseStack,
        MultiBufferSource buffer,
        int packedLight,
        int packedOverlay
    ) {
        Level level = blockEntity.getLevel();
        if (level == null) return;
        final BakedModel model = blockRenderer.getBlockModel(defaultLightState);
        poseStack.pushPose();
        poseStack.translate(0.005f, 0.005f, 0.005f);
        poseStack.scale(0.99f, 0.99f, 0.99f);
        VertexConsumer vertexConsumer = buffer.getBuffer(ModRenderTypes.BEACON_GLASS);
        for (Direction value : Direction.values()) {
            List<BakedQuad> quads = model.getQuads(
                null,
                value,
                level.random,
                ModelData.EMPTY,
                null
            );
            for (BakedQuad quad : quads) {
                vertexConsumer.putBulkData(
                    poseStack.last(),
                    quad,
                    109 / 255f,
                    1 / 255f,
                    206 / 255f,
                    0.3f,
                    packedLight,
                    packedOverlay
                );
            }
        }

        BlockState state = level.getBlockState(blockEntity.getBlockPos());
        if (!state.hasProperty(CorruptedBeaconBlock.LIT) || !state.getValue(CorruptedBeaconBlock.LIT)) {
            poseStack.popPose();
            return;
        }
        long l = level.getGameTime();
        List<CorruptedBeaconBlockEntity.BeaconBeamSection> list = blockEntity.getBeamSections();
        int i = 0;
        for (int j = 0; j < list.size(); ++j) {
            CorruptedBeaconBlockEntity.BeaconBeamSection beaconBeamSection = list.get(j);
            CorruptedBeaconRenderer.renderBeaconBeam(
                poseStack,
                buffer,
                partialTick,
                l,
                i,
                j == list.size() - 1 ? MAX_RENDER_Y : beaconBeamSection.getHeight(),
                beaconBeamSection.getColor()
            );
            i += beaconBeamSection.getHeight();
        }
        poseStack.popPose();
    }

    private static void renderBeaconBeam(
        PoseStack pose,
        MultiBufferSource bufferSource,
        float partialTick,
        long gameTime,
        int offsetY,
        int height,
        int color
    ) {
        renderBeaconBeam(
            pose,
            bufferSource,
            BEAM_LOCATION,
            partialTick,
            1.0F,
            gameTime,
            offsetY,
            height,
            color,
            0.2F,
            0.25F
        );
    }

    public static void renderBeaconBeam(
        PoseStack pose,
        MultiBufferSource bufferSource,
        ResourceLocation beamLocation,
        float partialTick,
        float textureScale,
        long gameTime,
        int offsetY,
        int height,
        int color,
        float beamRadius,
        float glowRadius
    ) {
        final int maxY = offsetY + height;
        pose.pushPose();
        pose.translate(0.5, 0.0, 0.5);
        float f = (float) Math.floorMod(gameTime, 40) + partialTick;
        float f1 = height < 0 ? f : -f;
        float f2 = Mth.frac(f1 * 0.2F - (float) Mth.floor(f1 * 0.1F));
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(f * 2.25F - 45.0F));
        float f3;
        final float f5;
        float f6 = -beamRadius;
        float f9 = -beamRadius;
        float f12 = -1.0F + f2;
        float f13 = (float) height * textureScale * (0.5F / beamRadius) + f12;
        renderPart(
            pose,
            bufferSource.getBuffer(RenderType.beaconBeam(beamLocation, false)),
            color,
            offsetY,
            maxY,
            0.0F,
            beamRadius,
            beamRadius,
            0.0F,
            f6,
            0.0F,
            0.0F,
            f9,
            0.0F,
            1.0F,
            f13,
            f12
        );
        pose.popPose();
        f3 = -glowRadius;
        final float f4 = -glowRadius;
        f5 = -glowRadius;
        f6 = -glowRadius;
        f12 = -1.0F + f2;
        f13 = (float) height * textureScale + f12;
        renderPart(
            pose,
            bufferSource.getBuffer(RenderType.beaconBeam(beamLocation, true)),
            FastColor.ARGB32.color(32, color),
            offsetY,
            maxY,
            f3,
            f4,
            glowRadius,
            f5,
            f6,
            glowRadius,
            glowRadius,
            glowRadius,
            0.0F,
            1.0F,
            f13,
            f12
        );
        pose.popPose();
    }

    private static void renderPart(
        PoseStack poseStack,
        VertexConsumer consumer,
        int color,
        int minY,
        int maxY,
        float x1,
        float z1,
        float x2,
        float z2,
        float x3,
        float z3,
        float x4,
        float z4,
        float minU,
        float maxU,
        float minV,
        float maxV
    ) {
        PoseStack.Pose pose = poseStack.last();
        renderQuad(pose, consumer, color, minY, maxY, x1, z1, x2, z2, minU, maxU, minV, maxV);
        renderQuad(pose, consumer, color, minY, maxY, x4, z4, x3, z3, minU, maxU, minV, maxV);
        renderQuad(pose, consumer, color, minY, maxY, x2, z2, x4, z4, minU, maxU, minV, maxV);
        renderQuad(pose, consumer, color, minY, maxY, x3, z3, x1, z1, minU, maxU, minV, maxV);
    }

    private static void renderQuad(
        PoseStack.Pose pose,
        VertexConsumer consumer,
        int color,
        int minY,
        int maxY,
        float minX,
        float minZ,
        float maxX,
        float maxZ,
        float minU,
        float maxU,
        float minV,
        float maxV
    ) {
        addVertex(pose, consumer, color, maxY, minX, minZ, maxU, minV);
        addVertex(pose, consumer, color, minY, minX, minZ, maxU, maxV);
        addVertex(pose, consumer, color, minY, maxX, maxZ, minU, maxV);
        addVertex(pose, consumer, color, maxY, maxX, maxZ, minU, minV);
    }

    private static void addVertex(
        PoseStack.Pose pose,
        VertexConsumer consumer,
        int color,
        int y,
        float x,
        float z,
        float u,
        float v
    ) {
        consumer
            .addVertex(pose, x, (float) y, z)
            .setColor(color)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(15728880)
            .setUv2(0xF000F0 & '\uffff', 0xF000F0 & '\uffff')
            .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    @Override
    public boolean shouldRenderOffScreen(CorruptedBeaconBlockEntity blockEntity) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public boolean shouldRender(CorruptedBeaconBlockEntity blockEntity, Vec3 cameraPos) {
        return Vec3.atCenterOf(blockEntity.getBlockPos())
            .multiply(1.0, 0.0, 1.0)
            .closerThan(cameraPos.multiply(1.0, 0.0, 1.0), this.getViewDistance());
    }

    @Override
    public AABB getRenderBoundingBox(CorruptedBeaconBlockEntity blockEntity) {
        BlockPos pos = blockEntity.getBlockPos();
        return new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, 1024, pos.getZ() + 1);
    }
}
