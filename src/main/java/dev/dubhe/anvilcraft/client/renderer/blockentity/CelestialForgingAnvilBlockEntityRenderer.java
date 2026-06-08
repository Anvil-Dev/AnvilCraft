package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.cfa.CelestialForgingAnvilBlock;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyData;
import dev.dubhe.anvilcraft.block.entity.celestial.GiantPlanetData;
import dev.dubhe.anvilcraft.block.entity.celestial.RingType;
import dev.dubhe.anvilcraft.block.entity.celestial.RockyPlanetData;
import dev.dubhe.anvilcraft.block.entity.celestial.StarData;
import dev.dubhe.anvilcraft.block.entity.celestial.Temperature;
import dev.dubhe.anvilcraft.client.init.ModRenderTypes;
import dev.dubhe.anvilcraft.client.renderer.blockentity.celestial.CelestialBodyRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.celestial.CelestialBodyTextureBakery;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.model.data.ModelData;

@SuppressWarnings(
    {
        "checkstyle:Linelength",
        "checkstyle:VariableDeclarationUsageDistance",
        "deprecation",
        "checkstyle:Indentation",
        "checkstyle:NeedBraces"
    }
)
public class CelestialForgingAnvilBlockEntityRenderer implements BlockEntityRenderer<CelestialForgingAnvilBlockEntity> {
    public static final ModelResourceLocation RING1 = ModelResourceLocation.standalone(AnvilCraft.of("block/celestial_forging_anvil_ring_1"));
    public static final ModelResourceLocation RING2 = ModelResourceLocation.standalone(AnvilCraft.of("block/celestial_forging_anvil_ring_2"));
    public static final ModelResourceLocation RING3 = ModelResourceLocation.standalone(AnvilCraft.of("block/celestial_forging_anvil_ring_3"));
    public static final ModelResourceLocation RING4 = ModelResourceLocation.standalone(AnvilCraft.of("block/celestial_forging_anvil_ring_4"));
    public static final ModelResourceLocation RING5 = ModelResourceLocation.standalone(AnvilCraft.of("block/celestial_forging_anvil_ring_5"));
    public static final ModelResourceLocation RING6 = ModelResourceLocation.standalone(AnvilCraft.of("block/celestial_forging_anvil_ring_6"));

    private final BlockRenderDispatcher blockRenderer;
    private final BlockState whiteConcrete = Blocks.WHITE_CONCRETE.defaultBlockState();

    public CelestialForgingAnvilBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(
        CelestialForgingAnvilBlockEntity blockEntity, float partialTick,
        PoseStack poseStack, MultiBufferSource multiBufferSource,
        int packedLight, int packedOverlay
    ) {
        ModelBlockRenderer modelRenderer = Minecraft.getInstance().getBlockRenderer().getModelRenderer();
        float rot = blockEntity.getRotation() + (blockEntity.getRotation() - blockEntity.getPreRotation()) * partialTick;
        float centerY = blockEntity.isAmplify() ? 6.5f : 4.5f;
        CelestialBodyData bodyData = blockEntity.getCelestialBodyData();

        poseStack.pushPose();
        poseStack.translate(0.5, centerY, 0.5);
        poseStack.mulPose(Axis.XP.rotationDegrees(rot));
        VertexConsumer ringConsumer = multiBufferSource.getBuffer(RenderType.cutout());
        boolean isHugeStar = bodyData instanceof StarData s && s.size() >= 20;
        if (blockEntity.isAmplify()) {
            poseStack.scale(4, 4, 4);
            modelRenderer.renderModel(poseStack.last(), ringConsumer, null, Minecraft.getInstance().getModelManager().getModel(RING6), 0, 0, 0, LightTexture.FULL_BRIGHT, packedOverlay);
            poseStack.mulPose(Axis.ZP.rotationDegrees(rot));
            modelRenderer.renderModel(poseStack.last(), ringConsumer, null, Minecraft.getInstance().getModelManager().getModel(RING5), 0, 0, 0, LightTexture.FULL_BRIGHT, packedOverlay);
            if (!isHugeStar) {
                poseStack.mulPose(Axis.XP.rotationDegrees(rot));
                modelRenderer.renderModel(poseStack.last(), ringConsumer, null, Minecraft.getInstance().getModelManager().getModel(RING4), 0, 0, 0, LightTexture.FULL_BRIGHT, packedOverlay);
            }
        } else {
            boolean isGiantPlanet = bodyData instanceof GiantPlanetData;
            boolean isRockyPlanet = bodyData instanceof RockyPlanetData;
            poseStack.scale(4, 4, 4);
            if (!isRockyPlanet) {
                modelRenderer.renderModel(poseStack.last(), ringConsumer, null, Minecraft.getInstance().getModelManager().getModel(RING3), 0, 0, 0, LightTexture.FULL_BRIGHT, packedOverlay);
            }
            poseStack.mulPose(Axis.ZP.rotationDegrees(rot));
            modelRenderer.renderModel(poseStack.last(), ringConsumer, null, Minecraft.getInstance().getModelManager().getModel(RING2), 0, 0, 0, LightTexture.FULL_BRIGHT, packedOverlay);
            if (!isGiantPlanet) {
                poseStack.mulPose(Axis.XP.rotationDegrees(rot));
                modelRenderer.renderModel(poseStack.last(), ringConsumer, null, Minecraft.getInstance().getModelManager().getModel(RING1), 0, 0, 0, LightTexture.FULL_BRIGHT, packedOverlay);
            }
        }
        poseStack.popPose();

        if (bodyData != null) {
            float bodyRot = blockEntity.getBodyRotation() + partialTick;
            renderCelestialBody(bodyData, centerY, bodyRot, poseStack, multiBufferSource, packedOverlay, blockEntity.getBlockPos().asLong());
            renderCelestialRing(bodyData, centerY, bodyRot, poseStack, multiBufferSource, packedOverlay);
        }
    }

    private void renderCelestialBody(
        CelestialBodyData bodyData, float centerY, float bodyRotation,
        PoseStack poseStack, MultiBufferSource bufferSource, int packedOverlay, long seed
    ) {
        poseStack.pushPose();
        poseStack.translate(0.5, centerY, 0.5);
        float scale = getBodyScale(bodyData);
        poseStack.scale(scale, scale, scale);
        poseStack.mulPose(Axis.XP.rotationDegrees(bodyData.axialTilt()));
        poseStack.mulPose(Axis.YP.rotationDegrees(bodyRotation * bodyData.rotationSpeed()));
        poseStack.translate(-0.5, -0.5, -0.5);

        if (bodyData instanceof StarData star) {
            renderStarBody(star, poseStack, bufferSource, packedOverlay, seed);
        } else {
            renderPlanetBody(bodyData, poseStack, bufferSource, packedOverlay, seed);
        }
        poseStack.popPose();
    }

    private void renderCelestialRing(
        CelestialBodyData bodyData, float centerY, float bodyRotation,
        PoseStack poseStack, MultiBufferSource bufferSource, int packedOverlay
    ) {
        if (bodyData.ringType() == RingType.NONE) return;
        ResourceLocation ringTexture = CelestialBodyTextureBakery.getOrBakeRing(bodyData);
        if (ringTexture == null) return;

        poseStack.pushPose();
        poseStack.translate(0.5, centerY, 0.5);
        float ringScale = getRingScale(bodyData);
        poseStack.scale(ringScale, ringScale, ringScale);
        poseStack.mulPose(Axis.XP.rotationDegrees(bodyData.axialTilt()));
        poseStack.mulPose(Axis.YP.rotationDegrees(bodyRotation * bodyData.rotationSpeed()));
        poseStack.translate(-0.5, -0.5, -0.5);

        VertexConsumer ringConsumer = bufferSource.getBuffer(RenderType.entityTranslucent(ringTexture));
        CelestialBodyRenderer.renderRing(poseStack, ringConsumer, LightTexture.FULL_BRIGHT, packedOverlay);
        poseStack.popPose();
    }

    private void renderStarBody(
        StarData star, PoseStack poseStack, MultiBufferSource bufferSource,
        int packedOverlay, long seed
    ) {
        ResourceLocation starTexture = CelestialBodyTextureBakery.getOrBakeBody(star);
        if (starTexture == null) return;
        VertexConsumer starConsumer = bufferSource.getBuffer(ModRenderTypes.STAR_CUTOUT.apply(starTexture));
        CelestialBodyRenderer.renderStarBody(poseStack, starConsumer, LightTexture.FULL_BRIGHT, packedOverlay);

        float[] rgb = CelestialBodyTextureBakery.starColor(star);
        int haloIterations = 10;

        for (int i = 0; i < haloIterations; i++) {
            float progress = (float) i / haloIterations;
            float haloScale = 2.0f + progress * 1.2f;
            float alpha = (2.4f - 2.25f * progress) / haloIterations;
            poseStack.pushPose();
            poseStack.translate(0.5, 0.5, 0.5);
            poseStack.scale(haloScale, haloScale, haloScale);
            poseStack.translate(-0.5, -0.5, -0.5);
            renderTranslucentCube(poseStack, bufferSource, rgb[0], rgb[1], rgb[2], alpha, LightTexture.FULL_BRIGHT, packedOverlay, seed);
            poseStack.popPose();
        }
    }

    private void renderPlanetBody(
        CelestialBodyData bodyData, PoseStack poseStack, MultiBufferSource bufferSource,
        int packedOverlay, long seed
    ) {
        ResourceLocation bodyTexture = CelestialBodyTextureBakery.getOrBakeBody(bodyData);
        if (bodyTexture != null) {
            VertexConsumer bodyConsumer = bufferSource.getBuffer(ModRenderTypes.STAR_CUTOUT.apply(bodyTexture));
            CelestialBodyRenderer.renderPlanetBody(poseStack, bodyConsumer, LightTexture.FULL_BRIGHT, packedOverlay);
        }

        if (bodyData instanceof RockyPlanetData rp && rp.hasAtmosphere()) {
            float[] atmosRgb = getAtmosphereColor(rp.temperature());
            poseStack.pushPose();
            poseStack.translate(0.5, 0.5, 0.5);
            poseStack.scale(1.125f, 1.125f, 1.125f);
            poseStack.translate(-0.5, -0.5, -0.5);
            renderTranslucentCube(poseStack, bufferSource, atmosRgb[0], atmosRgb[1], atmosRgb[2], 0.2f, LightTexture.FULL_BRIGHT, packedOverlay, seed);
            poseStack.popPose();
        }
    }

    private void renderTranslucentCube(
        PoseStack poseStack, MultiBufferSource bufferSource,
        float r, float g, float b, float a, int light, int overlay, long seed
    ) {
        BakedModel cubeModel = blockRenderer.getBlockModel(whiteConcrete);
        VertexConsumer consumer = bufferSource.getBuffer(ModRenderTypes.CELESTIAL_ATMOSPHERE);
        RandomSource random = RandomSource.create(seed);
        for (Direction dir : Direction.values()) {
            for (BakedQuad quad : cubeModel.getQuads(null, dir, random, ModelData.EMPTY, null))
                consumer.putBulkData(poseStack.last(), quad, r, g, b, a, light, overlay);
        }
        for (BakedQuad quad : cubeModel.getQuads(null, null, random, ModelData.EMPTY, null))
            consumer.putBulkData(poseStack.last(), quad, r, g, b, a, light, overlay);
    }

    /**
     * Get the visual scale for a celestial body.
     * Stars use a 32³ cube, planets use a 16³ cube — stars are naturally 2× larger.
     * Scale ranges account for this so that the smallest red dwarf is slightly
     * smaller than the largest giant planet, while red giants are enormous.
     */
    private float getBodyScale(CelestialBodyData data) {
        return switch (data) {
            case RockyPlanetData rp -> 0.41f + (rp.size() - 1) * 0.07f / 7f;
            case GiantPlanetData gp -> 0.53f + (gp.size() - 1) * 0.27f / 7f;
            case StarData s -> 0.39f + (s.size() - 1) * 0.86f / 27f;
            default -> 0.5f;
        };
    }

    private float getRingScale(CelestialBodyData data) {
        float bodyScale = getBodyScale(data);
        return switch (data) {
            case RockyPlanetData rp -> bodyScale * 1.35f;
            case GiantPlanetData gp -> bodyScale * 1.3f;
            default -> bodyScale * 1.4f;
        };
    }

    private float[] getAtmosphereColor(Temperature temperature) {
        return switch (temperature) {
            case FREEZING -> new float[]{0.4f, 0.6f, 0.9f};
            case COLD -> new float[]{0.5f, 0.7f, 0.9f};
            case MILD -> new float[]{0.6f, 0.8f, 1.0f};
            case HOT -> new float[]{0.9f, 0.5f, 0.3f};
            case SCORCHED -> new float[]{1.0f, 0.3f, 0.1f};
        };
    }

    @Override
    public AABB getRenderBoundingBox(CelestialForgingAnvilBlockEntity blockEntity) {
        BlockState state = blockEntity.getBlockState();
        if (!blockEntity.isAmplify()) {
            AABB aabb = new AABB(blockEntity.getBlockPos().offset(state.getValue(CelestialForgingAnvilBlock.HALF).getOffset())).inflate(1, 0, 0);
            return aabb.setMaxY(aabb.maxY + 5);
        }
        AABB aabb = new AABB(blockEntity.getBlockPos().offset(state.getValue(CelestialForgingAnvilBlock.HALF).getOffset())).inflate(3, 0, 3);
        return aabb.setMaxY(aabb.maxY + 7);
    }
}
