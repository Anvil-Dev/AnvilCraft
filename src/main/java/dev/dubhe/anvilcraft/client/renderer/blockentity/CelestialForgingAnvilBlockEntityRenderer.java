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
import dev.dubhe.anvilcraft.block.entity.celestial.SpecialCelestialBodyData;
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
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

@SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
public class CelestialForgingAnvilBlockEntityRenderer implements BlockEntityRenderer<CelestialForgingAnvilBlockEntity> {
    public static final ModelResourceLocation R1 = ModelResourceLocation.standalone(AnvilCraft.of("block/celestial_forging_anvil_ring_1"));
    public static final ModelResourceLocation R2 = ModelResourceLocation.standalone(AnvilCraft.of("block/celestial_forging_anvil_ring_2"));
    public static final ModelResourceLocation R3 = ModelResourceLocation.standalone(AnvilCraft.of("block/celestial_forging_anvil_ring_3"));
    public static final ModelResourceLocation R4 = ModelResourceLocation.standalone(AnvilCraft.of("block/celestial_forging_anvil_ring_4"));
    public static final ModelResourceLocation R5 = ModelResourceLocation.standalone(AnvilCraft.of("block/celestial_forging_anvil_ring_5"));
    public static final ModelResourceLocation R6 = ModelResourceLocation.standalone(AnvilCraft.of("block/celestial_forging_anvil_ring_6"));

    // Excavator megastructure models
    public static final ModelResourceLocation R1_EXCAVATOR = ModelResourceLocation.standalone(AnvilCraft.of(
        "block/celestial_forging_anvil_ring_1_excavator"));
    public static final ModelResourceLocation R1_EXCAVATOR_OFF = ModelResourceLocation.standalone(AnvilCraft.of(
        "block/celestial_forging_anvil_ring_1_excavator_off"));

    // Extractor megastructure model
    public static final ModelResourceLocation R1_EXCTRACTOR = ModelResourceLocation.standalone(AnvilCraft.of(
        "block/celestial_forging_anvil_ring_1_exctractor"));

    // Eco Station megastructure model
    public static final ModelResourceLocation R1_ECO_STATION = ModelResourceLocation.standalone(AnvilCraft.of(
        "block/celestial_forging_anvil_ring_1_eco_station"));

    // Temple megastructure model
    public static final ModelResourceLocation R1_TEMPLE = ModelResourceLocation.standalone(AnvilCraft.of(
        "block/celestial_forging_anvil_ring_1_temple"));

    // Giant Extractor megastructure model (replaces ring 2)
    public static final ModelResourceLocation R2_EXCTRACTOR = ModelResourceLocation.standalone(AnvilCraft.of(
        "block/celestial_forging_anvil_ring_2_exctractor"));

    // Stellar Ring Collider megastructure model (replaces ring 4)
    public static final ModelResourceLocation R4_COLLIDER = ModelResourceLocation.standalone(AnvilCraft.of(
        "block/celestial_forging_anvil_ring_4_collider"));

    // Dyson Sphere megastructure models (replace ring 4 / ring 5)
    public static final ModelResourceLocation R4_DYSON_SPHERE = ModelResourceLocation.standalone(AnvilCraft.of(
        "block/celestial_forging_anvil_ring_4_dyson_sphere"));
    public static final ModelResourceLocation R5_DYSON_SPHERE = ModelResourceLocation.standalone(AnvilCraft.of(
        "block/celestial_forging_anvil_ring_5_dyson_sphere"));

    private final BlockRenderDispatcher blockRenderer;
    private final BlockState whiteConcrete = Blocks.WHITE_CONCRETE.defaultBlockState();

    public CelestialForgingAnvilBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(
        CelestialForgingAnvilBlockEntity blockEntity,
        float partialTick,
        PoseStack poseStack,
        MultiBufferSource multiBufferSource,
        int packedLight,
        int packedOverlay
    ) {
        ModelBlockRenderer modelRenderer = Minecraft.getInstance().getBlockRenderer().getModelRenderer();
        float rot = blockEntity.getRotation() + (blockEntity.getRotation() - blockEntity.getPreRotation()) * partialTick;
        float centerY = blockEntity.isAmplify() ? 6.5f : 4.5f;
        CelestialBodyData bodyData = blockEntity.getCelestialBodyData();

        poseStack.pushPose();
        poseStack.translate(0.5, centerY, 0.5);

        // Track previous body and animation state for ring fade transitions
        CelestialBodyData prevBody = blockEntity.getAnimationPreviousBodyData();
        float animProgress = blockEntity.getAnimationProgress(partialTick);
        boolean isAnimating = blockEntity.getAnimationTicks() > 0;
        boolean animForward = blockEntity.isAnimationForward();

        // Build ring assembly pose from Blockbench animation, rendering rings
        // at their respective bone levels so each ring gets a different cumulative
        // rotation — this keeps the three rings crossed and never coplanar.
        // Bone hierarchy: outout(Y rot) → out(static tilt) → mid(X rot) → in(Z rot)
        // Ring attachment: outermost ring → out, middle ring → mid, innermost ring → in
        poseStack.scale(6, 6, 6);

        // outout + out: root + static tilt (outer bone)
        poseStack.mulPose(Axis.YP.rotationDegrees(-rot));        // outout: Y-axis rotation
        poseStack.mulPose(Axis.XP.rotationDegrees(14.5108f));    // out: static tilt X
        poseStack.mulPose(Axis.YP.rotationDegrees(-3.8411f));    // out: static tilt Y
        poseStack.mulPose(Axis.ZP.rotationDegrees(14.5109f));    // out: static tilt Z

        // === Outermost ring — child of "out" ===
        if (blockEntity.isAmplify()) {
            boolean isDysonSphereR5 = isDysonSphereActive(blockEntity, 5);
            boolean anyDysonSphere = isDysonSphereActive(blockEntity, 4) || isDysonSphereR5;
            if (!anyDysonSphere) {
                renderRingMaybe(R6, 6, bodyData, prevBody, isAnimating, animForward, animProgress,
                    poseStack, multiBufferSource, packedOverlay, modelRenderer);
            }
        } else {
            renderRingMaybe(R3, 3, bodyData, prevBody, isAnimating, animForward, animProgress,
                poseStack, multiBufferSource, packedOverlay, modelRenderer);
        }

        // mid: X rotation (middle bone)
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0f + rot));

        // === Middle ring — child of "mid" ===
        if (blockEntity.isAmplify()) {
            boolean anyDysonSphere = isDysonSphereActive(blockEntity, 4)
                || isDysonSphereActive(blockEntity, 5);
            if (!anyDysonSphere) {
                ModelResourceLocation r5Model = getRing5Model(blockEntity);
                renderRingMaybe(r5Model, 5, bodyData, prevBody, isAnimating, animForward, animProgress,
                    poseStack, multiBufferSource, packedOverlay, modelRenderer);
            }
        } else {
            ModelResourceLocation r2Model = getRing2Model(blockEntity);
            renderRingMaybe(r2Model, 2, bodyData, prevBody, isAnimating, animForward, animProgress,
                poseStack, multiBufferSource, packedOverlay, modelRenderer);
        }

        // in: Z rotation (inner bone)
        poseStack.mulPose(Axis.ZP.rotationDegrees(rot));

        // === Innermost ring — child of "in" ===
        if (blockEntity.isAmplify()) {
            if (!isDysonSphereActive(blockEntity, 4)) {
                ModelResourceLocation r4Model = getRing4Model(blockEntity);
                renderRingMaybe(r4Model, 4, bodyData, prevBody, isAnimating, animForward, animProgress,
                    poseStack, multiBufferSource, packedOverlay, modelRenderer);
            }
        } else {
            ModelResourceLocation r1Model = getRing1Model(blockEntity);
            renderRingMaybe(r1Model, 1, bodyData, prevBody, isAnimating, animForward, animProgress,
                poseStack, multiBufferSource, packedOverlay, modelRenderer);
        }
        poseStack.popPose();

        // Render Dyson Sphere rings with star-synchronous rotation
        if (blockEntity.isAmplify()) {
            boolean isDysonSphereR4 = isDysonSphereActive(blockEntity, 4);
            boolean isDysonSphereR5 = isDysonSphereActive(blockEntity, 5);
            if ((isDysonSphereR4 || isDysonSphereR5) && bodyData instanceof StarData star) {
                float rotationBoost = blockEntity.getAnimationRotationBoost(partialTick);
                float bodyRot = (blockEntity.getBodyRotation() + partialTick) * rotationBoost;
                renderDysonSphereRings(
                    isDysonSphereR4,
                    isDysonSphereR5,
                    centerY,
                    bodyRot,
                    star,
                    animProgress,
                    poseStack,
                    multiBufferSource,
                    packedOverlay,
                    modelRenderer
                );
            }
        }

        // Use effective body data (considers reverse animation where celestialBodyData is already null)
        CelestialBodyData effectiveBodyData = blockEntity.getEffectiveBodyDataForRendering();
        boolean canRender = effectiveBodyData != null && !(effectiveBodyData instanceof StarData && !blockEntity.isAmplifierPresent());
        if (canRender) {
            float rotationBoost = blockEntity.getAnimationRotationBoost(partialTick);
            float bodyRot = (blockEntity.getBodyRotation() + partialTick) * rotationBoost;
            renderCelestialBody(
                effectiveBodyData,
                centerY,
                bodyRot,
                poseStack,
                multiBufferSource,
                packedOverlay,
                blockEntity.getBlockPos().asLong(),
                animProgress
            );
            renderCelestialRing(effectiveBodyData, centerY, bodyRot, poseStack, multiBufferSource, packedOverlay, animProgress);
        }
    }

    /**
     * Get the appropriate ring 1 model, accounting for excavator megastructure.
     */
    private ModelResourceLocation getRing1Model(CelestialForgingAnvilBlockEntity blockEntity) {
        if (blockEntity.getActiveMegastructureIndex() >= 0) {
            var option = blockEntity.getActiveMegastructureOption();
            if (option != null) {
                if ("planet_excavator".equals(option.megastructure())) {
                    return blockEntity.isExcavatorLaserActive() ? R1_EXCAVATOR : R1_EXCAVATOR_OFF;
                }
                if ("planet_exctractor".equals(option.megastructure())) {
                    return R1_EXCTRACTOR;
                }
                if ("eco_station".equals(option.megastructure())) {
                    return R1_ECO_STATION;
                }
                if ("temple".equals(option.megastructure())) {
                    return R1_TEMPLE;
                }
            }
        }
        return R1;
    }

    /**
     * Get the appropriate ring 2 model, accounting for giant planet extractor megastructure.
     */
    private ModelResourceLocation getRing2Model(CelestialForgingAnvilBlockEntity blockEntity) {
        if (blockEntity.getActiveMegastructureIndex() >= 0) {
            var option = blockEntity.getActiveMegastructureOption();
            if (option != null) {
                if ("giant_planet_exctractor".equals(option.megastructure())) {
                    return R2_EXCTRACTOR;
                }
            }
        }
        return R2;
    }

    /**
     * Get the appropriate ring 4 model, accounting for megastructures that replace ring 4.
     */
    private ModelResourceLocation getRing4Model(CelestialForgingAnvilBlockEntity blockEntity) {
        if (blockEntity.getActiveMegastructureIndex() >= 0) {
            var option = blockEntity.getActiveMegastructureOption();
            if (option != null) {
                if ("stellar_ring_collider".equals(option.megastructure())) {
                    // When amplifier is missing, the star is not rendered — collider is off
                    if (!blockEntity.isAmplifierPresent()) {
                        return R4;
                    }
                    return R4_COLLIDER;
                }
                if ("dyson_sphere_small".equals(option.megastructure())) {
                    return R4_DYSON_SPHERE;
                }
            }
        }
        return R4;
    }

    /**
     * Get the appropriate ring 5 model, accounting for megastructures that replace ring 5.
     */
    private ModelResourceLocation getRing5Model(CelestialForgingAnvilBlockEntity blockEntity) {
        if (blockEntity.getActiveMegastructureIndex() >= 0) {
            var option = blockEntity.getActiveMegastructureOption();
            if (option != null) {
                if ("dyson_sphere_large".equals(option.megastructure())) {
                    return R5_DYSON_SPHERE;
                }
            }
        }
        return R5;
    }

    /**
     * Check whether a Dyson Sphere megastructure is active on the given ring index.
     */
    private static boolean isDysonSphereActive(CelestialForgingAnvilBlockEntity blockEntity, int ring) {
        if (blockEntity.getActiveMegastructureIndex() < 0) return false;
        var option = blockEntity.getActiveMegastructureOption();
        if (option == null) return false;
        if (ring == 4) return "dyson_sphere_small".equals(option.megastructure());
        if (ring == 5) return "dyson_sphere_large".equals(option.megastructure());
        return false;
    }

    /**
     * Render Dyson Sphere rings with star-synchronous rotation (Y rotation matching the star),
     * instead of the mechanical X/Z rotation used by other rings.
     */
    private void renderDysonSphereRings(
        boolean renderR4,
        boolean renderR5,
        float centerY,
        float bodyRot,
        StarData star,
        float scale,
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int packedOverlay,
        ModelBlockRenderer modelRenderer
    ) {
        if (!renderR4 && !renderR5) return;
        if (scale < 0.001f) return;

        poseStack.pushPose();
        poseStack.translate(0.5, centerY, 0.5);
        poseStack.scale(6, 6, 6);
        // Apply star's axial tilt and Y rotation — same as star body rendering
        poseStack.mulPose(Axis.XP.rotationDegrees(star.axialTilt()));
        poseStack.mulPose(Axis.YP.rotationDegrees(bodyRot * star.rotationSpeed()));

        if (renderR4) {
            renderRingCutout(R4_DYSON_SPHERE, poseStack, bufferSource, packedOverlay, modelRenderer);
        }
        if (renderR5) {
            renderRingCutout(R5_DYSON_SPHERE, poseStack, bufferSource, packedOverlay, modelRenderer);
        }
        poseStack.popPose();
    }

    /**
     * Determine whether a mechanical ring should be visible for the given body data.
     */
    private static boolean isRingVisible(int ring, @Nullable CelestialBodyData bodyData, boolean isAmplify) {
        if (isAmplify) {
            return switch (ring) {
                case 4 -> bodyData == null || bodyData.size() < 26;
                case 5 -> true;
                case 6 -> bodyData != null && bodyData.size() >= 26;
                default -> false;
            };
        } else {
            if (bodyData == null) return ring >= 1 && ring <= 3;
            return switch (ring) {
                case 1 -> !(bodyData instanceof GiantPlanetData);
                case 2 -> true;
                case 3 -> !(bodyData instanceof RockyPlanetData)
                && !(bodyData instanceof SpecialCelestialBodyData);
                default -> false;
            };
        }
    }

    /**
     * Render a mechanical ring with fade-in / fade-out transitions
     * when its visibility changes between the previous and current celestial body.
     */
    private void renderRingMaybe(
        ModelResourceLocation modelId,
        int ringIndex,
        @Nullable CelestialBodyData currBody,
        @Nullable CelestialBodyData prevBody,
        boolean isAnimating,
        boolean animForward,
        float animProgress,
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int packedOverlay,
        ModelBlockRenderer modelRenderer
    ) {
        // Rings 1-3: non-amplify mode, Rings 4-6: amplify mode
        boolean isAmplify = ringIndex >= 4;
        boolean visibleNow = isRingVisible(ringIndex, currBody, isAmplify);
        // null prevBody means no previous body → all rings were visible
        boolean wasVisible = prevBody == null || isRingVisible(ringIndex, prevBody, isAmplify);

        if (!isAnimating) {
            // No animation — simple render if visible
            if (visibleNow) {
                renderRingCutout(modelId, poseStack, bufferSource, packedOverlay, modelRenderer);
            }
            return;
        }

        if (visibleNow && wasVisible) {
            // Always visible — render normally with cutout
            renderRingCutout(modelId, poseStack, bufferSource, packedOverlay, modelRenderer);
        } else if (visibleNow) {
            // Hidden → Visible: scale from near-zero to 1.0 and rise into position
            float scale = animForward ? animProgress : (1.0f - animProgress);
            if (scale > 0.01f) {
                renderRingScaled(modelId, scale, poseStack, bufferSource, packedOverlay, modelRenderer);
            }
        } else if (wasVisible) {
            // Visible → Hidden: scale from 1.0 to near-zero and sink into the anvil
            float scale = animForward ? (1.0f - animProgress) : animProgress;
            if (scale > 0.01f) {
                renderRingScaled(modelId, scale, poseStack, bufferSource, packedOverlay, modelRenderer);
            }
        }
        // else: !visibleNow && !wasVisible — always hidden, do nothing
    }

    /**
     * Render a ring model with the standard cutout render type.
     */
    private void renderRingCutout(
        ModelResourceLocation modelId,
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int packedOverlay,
        ModelBlockRenderer modelRenderer
    ) {
        modelRenderer.renderModel(
            poseStack.last(),
            bufferSource.getBuffer(RenderType.cutout()),
            null,
            Minecraft.getInstance().getModelManager().getModel(modelId),
            0,
            0,
            0,
            LightTexture.FULL_BRIGHT,
            packedOverlay
        );
    }

    /**
     * Render a ring model scaled around its center (the model origin, since rings are centered at 0,0,0).
     *
     * @param scale 0.0 = invisible, 1.0 = full size
     */
    private void renderRingScaled(
        ModelResourceLocation modelId,
        float scale,
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int packedOverlay,
        ModelBlockRenderer modelRenderer
    ) {
        poseStack.pushPose();
        poseStack.scale(scale, scale, scale);
        renderRingCutout(modelId, poseStack, bufferSource, packedOverlay, modelRenderer);
        poseStack.popPose();
    }

    private void renderCelestialBody(
        CelestialBodyData bodyData,
        float centerY,
        float bodyRotation,
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int packedOverlay,
        long seed,
        float animProgress
    ) {
        poseStack.pushPose();
        poseStack.translate(0.5, centerY, 0.5);
        float baseScale = getBodyScale(bodyData);
        if (bodyData instanceof SpecialCelestialBodyData s && s.specialType().isErrorPlanet()) {
            baseScale *= 0.25f;
        }
        float scale = baseScale * animProgress;
        if (scale < 0.001f) {
            poseStack.popPose();
            return;
        }
        poseStack.scale(scale, scale, scale);
        poseStack.mulPose(Axis.XP.rotationDegrees(bodyData.axialTilt()));
        poseStack.mulPose(Axis.YP.rotationDegrees(bodyRotation * bodyData.rotationSpeed()));
        poseStack.translate(-0.5, -0.5, -0.5);

        // Complex custom models (shattered, hollow, flesh, intelligence, error)
        if (bodyData instanceof SpecialCelestialBodyData s && s.specialType().needsCustomModel()) {
            renderComplexModelBody(s, poseStack, bufferSource, packedOverlay);
            // Atmosphere for complex-model bodies that have it (flesh, intelligence)
            if (s.hasAtmosphere() && s.temperature() != null) {
                poseStack.pushPose();
                poseStack.translate(0.5, 0.5, 0.5);
                poseStack.scale(1.125f, 1.125f, 1.125f);
                poseStack.translate(-0.5, -0.5, -0.5);
                float[] atmosRgb = CelestialBodyRenderer.getAtmosphereColor(s.temperature());
                renderAtmosphereCube(
                    poseStack,
                    bufferSource,
                    atmosRgb[0],
                    atmosRgb[1],
                    atmosRgb[2],
                    0.2f,
                    LightTexture.FULL_BRIGHT,
                    packedOverlay,
                    seed
                );
                poseStack.popPose();
            }
        } else if (bodyData instanceof StarData star) {
            renderStarModel(star, poseStack, bufferSource, packedOverlay, seed);
        } else {
            renderPlanetBody(bodyData, poseStack, bufferSource, packedOverlay, seed);
        }
        poseStack.popPose();
    }

    private static final ModelResourceLocation STAR_MODEL =
        ModelResourceLocation.standalone(AnvilCraft.of("block/celestial_body/star"));

    /**
     * Render a complex-model celestial body (shattered planet, hollow planet)
     * via its block model file.
     */
    private void renderComplexModelBody(
        SpecialCelestialBodyData special,
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int packedOverlay
    ) {
        BakedModel model = Minecraft.getInstance().getModelManager()
            .getModel(special.specialType().getModelLocation());
        if (model == Minecraft.getInstance().getModelManager().getMissingModel()) return;

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.cutout());
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(
            poseStack.last(), consumer, null, model,
            1.0f, 1.0f, 1.0f, LightTexture.FULL_BRIGHT, packedOverlay
        );
    }

    /**
     * Render a star: animated base model + color-tint overlay + halo.
     * The block model provides animation (via .mcmeta), the translucent
     * overlay cube provides the star-specific color from energy anvil count.
     */
    private void renderStarModel(
        StarData star,
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int packedOverlay,
        long seed
    ) {
        // Animated grayscale star model (block atlas, supports .mcmeta)
        BakedModel model = Minecraft.getInstance().getModelManager().getModel(STAR_MODEL);
        if (model != Minecraft.getInstance().getModelManager().getMissingModel()) {
            VertexConsumer consumer = bufferSource.getBuffer(RenderType.cutout());
            Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(
                poseStack.last(), consumer, null, model,
                1.0f, 1.0f, 1.0f, LightTexture.FULL_BRIGHT, packedOverlay
            );
        }

        // Color overlay — multiplicative blend for accurate palette coloring
        float[] rgb = CelestialBodyTextureBakery.starColor(star);
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.scale(1.005f, 1.005f, 1.005f);
        poseStack.translate(-0.5, -0.5, -0.5);
        renderColorOverlay(poseStack, bufferSource, rgb[0], rgb[1], rgb[2], packedOverlay);
        poseStack.popPose();

        // Star halo
        int haloIterations = 10;
        for (int i = 0; i < haloIterations; i++) {
            float progress = (float) i / haloIterations;
            float haloScale = 1.0f + progress * 0.6f;
            float alpha = (1.2f - 1.125f * progress) / haloIterations;
            poseStack.pushPose();
            poseStack.translate(0.5, 0.5, 0.5);
            poseStack.scale(haloScale, haloScale, haloScale);
            poseStack.translate(-0.5, -0.5, -0.5);
            renderTranslucentCube(poseStack, bufferSource, rgb[0], rgb[1], rgb[2], alpha, LightTexture.FULL_BRIGHT, packedOverlay, seed);
            poseStack.popPose();
        }
    }

    private void renderCelestialRing(
        CelestialBodyData bodyData,
        float centerY,
        float bodyRotation,
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int packedOverlay,
        float animProgress
    ) {
        if (bodyData.ringType() == RingType.NONE) return;
        ResourceLocation ringTexture = CelestialBodyTextureBakery.getOrBakeRing(bodyData);
        if (ringTexture == null) return;

        poseStack.pushPose();
        poseStack.translate(0.5, centerY, 0.5);
        float ringScale = getRingScale(bodyData) * animProgress;
        if (ringScale < 0.001f) {
            poseStack.popPose();
            return;
        }
        poseStack.scale(ringScale, ringScale, ringScale);
        poseStack.mulPose(Axis.XP.rotationDegrees(bodyData.axialTilt()));
        poseStack.mulPose(Axis.YP.rotationDegrees(bodyRotation * bodyData.rotationSpeed()));
        poseStack.translate(-0.5, -0.5, -0.5);

        VertexConsumer ringConsumer = bufferSource.getBuffer(RenderType.entityTranslucent(ringTexture));
        CelestialBodyRenderer.renderRing(poseStack, ringConsumer, LightTexture.FULL_BRIGHT, packedOverlay);
        poseStack.popPose();
    }

    private void renderPlanetBody(
        CelestialBodyData bodyData,
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int packedOverlay,
        long seed
    ) {
        ResourceLocation bodyTexture = CelestialBodyTextureBakery.getOrBakeBody(bodyData);
        if (bodyTexture != null) {
            VertexConsumer bodyConsumer = bufferSource.getBuffer(ModRenderTypes.STAR_CUTOUT.apply(bodyTexture));
            CelestialBodyRenderer.renderPlanetBody(poseStack, bodyConsumer, LightTexture.FULL_BRIGHT, packedOverlay);
        }

        // Atmosphere — for rocky planets and special bodies that have atmosphere
        boolean hasAtmos;
        Temperature atmosTemp;
        if (bodyData instanceof RockyPlanetData rp) {
            hasAtmos = rp.hasAtmosphere();
            atmosTemp = rp.temperature();
        } else if (bodyData instanceof SpecialCelestialBodyData s) {
            hasAtmos = s.hasAtmosphere();
            atmosTemp = s.temperature();
        } else {
            hasAtmos = false;
            atmosTemp = null;
        }
        if (hasAtmos && atmosTemp != null) {
            float[] atmosRgb = getAtmosphereColor(atmosTemp);
            poseStack.pushPose();
            poseStack.translate(0.5, 0.5, 0.5);
            poseStack.scale(1.125f, 1.125f, 1.125f);
            poseStack.translate(-0.5, -0.5, -0.5);
            renderAtmosphereCube(
                poseStack, bufferSource,
                atmosRgb[0], atmosRgb[1], atmosRgb[2],
                0.2f, LightTexture.FULL_BRIGHT, packedOverlay, seed
            );
            poseStack.popPose();
        }

        // Brown dwarf: weak star-like halo
        if (bodyData instanceof GiantPlanetData gp && gp.brownDwarf()) {
            float[] rgb = getAtmosphereColor(Temperature.SCORCHED);
            int haloIterations = 3;
            for (int i = 0; i < haloIterations; i++) {
                float progress = (float) i / haloIterations;
                float haloScale = 1.15f + progress * 0.25f;
                float alpha = (0.45f - 0.38f * progress) / haloIterations;
                poseStack.pushPose();
                poseStack.translate(0.5, 0.5, 0.5);
                poseStack.scale(haloScale, haloScale, haloScale);
                poseStack.translate(-0.5, -0.5, -0.5);
                renderTranslucentCube(
                    poseStack, bufferSource,
                    rgb[0], rgb[1], rgb[2],
                    alpha, LightTexture.FULL_BRIGHT, packedOverlay, seed
                );
                poseStack.popPose();
            }
        }
    }

    /**
     * Renders a cube with multiplicative blending ({@code DST_COLOR * SRC_COLOR}),
     * used for star color overlay to achieve accurate palette-like coloring.
     */
    private void renderColorOverlay(
        PoseStack poseStack, MultiBufferSource bufferSource,
        float r, float g, float b, int packedOverlay
    ) {
        BakedModel cubeModel = blockRenderer.getBlockModel(whiteConcrete);
        VertexConsumer consumer = bufferSource.getBuffer(ModRenderTypes.STAR_COLOR_OVERLAY);
        RandomSource random = RandomSource.create(42L);
        for (Direction dir : Direction.values()) {
            for (BakedQuad quad : cubeModel.getQuads(null, dir, random, ModelData.EMPTY, null)) {
                consumer.putBulkData(poseStack.last(), quad, r, g, b, 1.0f, LightTexture.FULL_BRIGHT, packedOverlay);
            }
        }
        for (BakedQuad quad : cubeModel.getQuads(null, null, random, ModelData.EMPTY, null)) {
            consumer.putBulkData(poseStack.last(), quad, r, g, b, 1.0f, LightTexture.FULL_BRIGHT, packedOverlay);
        }
    }

    private void renderTranslucentCube(
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        float r,
        float g,
        float b,
        float a,
        int light,
        int overlay,
        long seed
    ) {
        BakedModel cubeModel = blockRenderer.getBlockModel(whiteConcrete);
        VertexConsumer consumer = bufferSource.getBuffer(ModRenderTypes.CELESTIAL_ATMOSPHERE);
        RandomSource random = RandomSource.create(seed);
        for (Direction dir : Direction.values()) {
            for (BakedQuad quad : cubeModel.getQuads(null, dir, random, ModelData.EMPTY, null)) {
                consumer.putBulkData(poseStack.last(), quad, r, g, b, a, light, overlay);
            }
        }
        for (BakedQuad quad : cubeModel.getQuads(null, null, random, ModelData.EMPTY, null)) {
            consumer.putBulkData(poseStack.last(), quad, r, g, b, a, light, overlay);
        }
    }

    /**
     * Like {@link #renderTranslucentCube} but alpha varies per face based on
     * direction: faces pointing away from the light scatter more, and faces
     * edge-on to the camera (the planet's limb) glow strongest — creating
     * an atmospheric rim around the dark hemisphere.
     */
    private void renderAtmosphereCube(
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        float r,
        float g,
        float b,
        float baseAlpha,
        int light,
        int overlay,
        long seed
    ) {
        BakedModel cubeModel = blockRenderer.getBlockModel(whiteConcrete);
        VertexConsumer consumer = bufferSource.getBuffer(ModRenderTypes.CELESTIAL_ATMOSPHERE);
        RandomSource random = RandomSource.create(seed);
        PoseStack.Pose pose = poseStack.last();

        // Compute view direction from body centre toward camera, in eye space
        Vector3f bodyCenter = new Vector3f(0.5f, 0.5f, 0.5f);
        bodyCenter.mulPosition(pose.pose());
        float vx = -bodyCenter.x;
        float vy = -bodyCenter.y;
        float vz = -bodyCenter.z;
        float vlen = (float) Math.sqrt(vx * vx + vy * vy + vz * vz);
        if (vlen > 1e-6f) {
            vx /= vlen;
            vy /= vlen;
            vz /= vlen;
        }

        for (Direction dir : Direction.values()) {
            float alpha = CelestialBodyRenderer.computeAtmosphereAlpha(
                pose,
                dir.getStepX(),
                dir.getStepY(),
                dir.getStepZ(),
                baseAlpha,
                vx,
                vy,
                vz
            );
            for (BakedQuad quad : cubeModel.getQuads(null, dir, random, ModelData.EMPTY, null)) {
                consumer.putBulkData(pose, quad, r, g, b, alpha, light, overlay);
            }
        }
        for (BakedQuad quad : cubeModel.getQuads(null, null, random, ModelData.EMPTY, null)) {
            consumer.putBulkData(pose, quad, r, g, b, baseAlpha, light, overlay);
        }
    }

    /**
     * Get the visual scale for a celestial body.
     * All bodies use the same 16³ cube base and the same scale formula —
     * only the space anvil count (size) matters for visual size.
     * Below size 20: linear growth. Above size 20: accelerating (quadratic) growth.
     * Max size 64 ≈ old size-30 effective scale (2.63), prevents clipping.
     */
    private float getBodyScale(CelestialBodyData data) {
        int size = data.size();
        if (size <= 20) {
            // Linear: size 1 → 0.3, size 20 → 1.5
            return 1.5f * (0.2f + (size - 1) * 0.8f / 19f);
        } else {
            // Quadratic acceleration: size 20 → 1.5, size 64 → 3.95
            float t = (size - 20) / 44f;
            return 1.5f * (1.0f + t * t * 1.63f);
        }
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
        return CelestialBodyRenderer.getAtmosphereColor(temperature);
    }

    @Override
    public AABB getRenderBoundingBox(CelestialForgingAnvilBlockEntity blockEntity) {
        BlockState state = blockEntity.getBlockState();
        if (!blockEntity.isAmplify()) {
            AABB aabb = new AABB(blockEntity.getBlockPos().offset(state.getValue(CelestialForgingAnvilBlock.HALF).getOffset())).inflate(
                1,
                0,
                0
            );
            return aabb.setMaxY(aabb.maxY + 5);
        }
        AABB aabb = new AABB(blockEntity.getBlockPos().offset(state.getValue(CelestialForgingAnvilBlock.HALF).getOffset())).inflate(
            3,
            0,
            3
        );
        return aabb.setMaxY(aabb.maxY + 7);
    }
}
