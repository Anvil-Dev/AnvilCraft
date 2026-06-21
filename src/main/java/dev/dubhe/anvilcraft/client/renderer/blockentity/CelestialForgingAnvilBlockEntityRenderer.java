package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.cfa.CelestialForgingAnvilBlock;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyClass;
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
import net.minecraft.world.phys.Vec3;
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

    // Magnetar Coil megastructure models (replace ring 4 with dual-model)
    public static final ModelResourceLocation R4_COIL_FIX = ModelResourceLocation.standalone(AnvilCraft.of(
        "block/celestial_forging_anvil_ring_4_coil_fix"));
    public static final ModelResourceLocation R4_COIL_RING = ModelResourceLocation.standalone(AnvilCraft.of(
        "block/celestial_forging_anvil_ring_4_coil_ring"));

    // Penrose Sphere megastructure models (replace ring 4 with dual-model)
    public static final ModelResourceLocation R4_PENROSE_SPHERE_FIX = ModelResourceLocation.standalone(AnvilCraft.of(
        "block/celestial_forging_anvil_ring_4_penrose_sphere_fix"));
    public static final ModelResourceLocation R4_PENROSE_SPHERE_LASER = ModelResourceLocation.standalone(AnvilCraft.of(
        "block/celestial_forging_anvil_ring_4_penrose_sphere_laser"));
    public static final ModelResourceLocation R4_PENROSE_SPHERE_LASER_OFF = ModelResourceLocation.standalone(AnvilCraft.of(
        "block/celestial_forging_anvil_ring_4_penrose_sphere_laser_off"));

    // Matter Decompressor megastructure models (replace ring 4 with dual-model)
    public static final ModelResourceLocation R4_MATTER_DECOMPRESSOR_FIX = ModelResourceLocation.standalone(AnvilCraft.of(
        "block/celestial_forging_anvil_ring_4_matter_decompressor_fix"));
    public static final ModelResourceLocation R4_MATTER_DECOMPRESSOR_RING = ModelResourceLocation.standalone(AnvilCraft.of(
        "block/celestial_forging_anvil_ring_4_matter_decompressor_ring"));

    // Wormhole Stabilizer megastructure model (replaces ring 4)
    public static final ModelResourceLocation R4_WORMHOLE_STABILIZER = ModelResourceLocation.standalone(AnvilCraft.of(
        "block/celestial_forging_anvil_ring_4_wormhole_stabilizer"));

    // Stellar Evolution Accelerator models
    public static final ModelResourceLocation R5_STELLAR_EVOLUTION_ACCELERATOR = ModelResourceLocation.standalone(AnvilCraft.of(
        "block/celestial_forging_anvil_ring_5_stellar_evolution_accelerator"));
    public static final ModelResourceLocation R6_STELLAR_EVOLUTION_ACCELERATOR = ModelResourceLocation.standalone(AnvilCraft.of(
        "block/celestial_forging_anvil_ring_6_stellar_evolution_accelerator"));

    // Stellar remnant body models
    private static final ModelResourceLocation NEUTRON_STAR_MODEL = ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(
        "anvilcraft",
        "block/celestial_body/neutron_star"
    ));
    private static final ModelResourceLocation NEUTRON_STAR_JET_MODEL =
        ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(
        "anvilcraft",
        "block/celestial_body/neutron_star_jet"
    ));
    private static final ModelResourceLocation BLACK_HOLE_MODEL = ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(
        "anvilcraft",
        "block/celestial_body/black_hole"
    ));

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
            boolean hideOutermostForPenrose = isPenroseSphereActive(blockEntity) && !blockEntity.isAcceleratorActive();
            if (!anyDysonSphere && !hideOutermostForPenrose) {
                ModelResourceLocation r6Model = getRing6Model(blockEntity);
                renderRingMaybe(
                    r6Model,
                    6,
                    bodyData,
                    prevBody,
                    isAnimating,
                    animForward,
                    animProgress,
                    poseStack,
                    multiBufferSource,
                    packedOverlay,
                    modelRenderer
                );
            }
        } else {
            renderRingMaybe(
                R3,
                3,
                bodyData,
                prevBody,
                isAnimating,
                animForward,
                animProgress,
                poseStack,
                multiBufferSource,
                packedOverlay,
                modelRenderer
            );
        }

        // mid: X rotation (middle bone)
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0f + rot));

        // === Middle ring — child of "mid" ===
        if (blockEntity.isAmplify()) {
            boolean anyDysonSphere = isDysonSphereActive(blockEntity, 4) || isDysonSphereActive(blockEntity, 5);
            // Hide ring 5 when Penrose Sphere is active AND ring 5 is the outermost (small star)
            boolean isSmallStar = bodyData != null && bodyData.size() < 48;
            boolean hideMiddleForPenrose = isPenroseSphereActive(blockEntity) && isSmallStar && !blockEntity.isAcceleratorActive();
            if (!anyDysonSphere && !hideMiddleForPenrose) {
                ModelResourceLocation r5Model = getRing5Model(blockEntity);
                renderRingMaybe(
                    r5Model,
                    5,
                    bodyData,
                    prevBody,
                    isAnimating,
                    animForward,
                    animProgress,
                    poseStack,
                    multiBufferSource,
                    packedOverlay,
                    modelRenderer
                );
            }
        } else {
            ModelResourceLocation r2Model = getRing2Model(blockEntity);
            renderRingMaybe(
                r2Model,
                2,
                bodyData,
                prevBody,
                isAnimating,
                animForward,
                animProgress,
                poseStack,
                multiBufferSource,
                packedOverlay,
                modelRenderer
            );
        }

        // in: Z rotation (inner bone)
        poseStack.mulPose(Axis.ZP.rotationDegrees(rot));

        // === Innermost ring — child of "in" ===
        if (blockEntity.isAmplify()) {
            if (!isDysonSphereActive(blockEntity, 4) && !isMagnetarCoilActive(blockEntity)
                && !isPenroseSphereActive(blockEntity)
                && !isMatterDecompressorActive(blockEntity)) {
                ModelResourceLocation r4Model = getRing4Model(blockEntity);
                renderRingMaybe(
                    r4Model,
                    4,
                    bodyData,
                    prevBody,
                    isAnimating,
                    animForward,
                    animProgress,
                    poseStack,
                    multiBufferSource,
                    packedOverlay,
                    modelRenderer
                );
            }
        } else {
            ModelResourceLocation r1Model = getRing1Model(blockEntity);
            renderRingMaybe(
                r1Model,
                1,
                bodyData,
                prevBody,
                isAnimating,
                animForward,
                animProgress,
                poseStack,
                multiBufferSource,
                packedOverlay,
                modelRenderer
            );
        }
        poseStack.popPose();

        // Render Dyson Sphere rings with star-synchronous rotation.
        // The Dyson Sphere models replace the inner ring(s); the outer ring
        // (R5 for small stars, R6 for large stars) is also rendered here with
        // star-synchronous rotation instead of being hidden.
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
                // Render the outer ring with star-synchronous rotation (like a real Dyson Sphere)
                boolean isSmallStar = bodyData.size() < 48;
                if (isDysonSphereR4 && isSmallStar) {
                    // Small star: R5 is the outer ring (may be replaced by accelerator model)
                    poseStack.pushPose();
                    poseStack.translate(0.5, centerY, 0.5);
                    poseStack.scale(6, 6, 6);
                    poseStack.mulPose(Axis.XP.rotationDegrees(star.axialTilt()));
                    poseStack.mulPose(
                        Axis.YP.rotationDegrees(bodyRot * CelestialBodyData.getVisualRotationSpeed(star.rotationSpeed())));
                    renderRingCutout(getRing5Model(blockEntity), poseStack, multiBufferSource, packedOverlay, modelRenderer);
                    poseStack.popPose();
                } else if (isDysonSphereR5 && !isSmallStar) {
                    // Large star: R6 is the outer ring (may be replaced by accelerator model)
                    poseStack.pushPose();
                    poseStack.translate(0.5, centerY, 0.5);
                    poseStack.scale(6, 6, 6);
                    poseStack.mulPose(Axis.XP.rotationDegrees(star.axialTilt()));
                    poseStack.mulPose(
                        Axis.YP.rotationDegrees(bodyRot * CelestialBodyData.getVisualRotationSpeed(star.rotationSpeed())));
                    renderRingCutout(getRing6Model(blockEntity), poseStack, multiBufferSource, packedOverlay, modelRenderer);
                    poseStack.popPose();
                }
            }
        }

        // Render Penrose Sphere rings: fix (star-synchronous) + laser/off (mechanical rotation)
        if (blockEntity.isAmplify() && isPenroseSphereActive(blockEntity) && bodyData instanceof StarData star) {
            float rotationBoost = blockEntity.getAnimationRotationBoost(partialTick);
            float bodyRot = (blockEntity.getBodyRotation() + partialTick) * rotationBoost;
            renderPenroseSphereRings(centerY, rot, bodyRot, star, animProgress,
                blockEntity.isPenroseSphereLaserActive(),
                poseStack, multiBufferSource, packedOverlay, modelRenderer);
        }

        // Render Magnetar Coil rings: fix (star-synchronous) + ring (mechanical rotation)
        if (blockEntity.isAmplify() && isMagnetarCoilActive(blockEntity) && bodyData instanceof StarData star) {
            float rotationBoost = blockEntity.getAnimationRotationBoost(partialTick);
            float bodyRot = (blockEntity.getBodyRotation() + partialTick) * rotationBoost;
            renderMagnetarCoilRings(centerY, rot, bodyRot, star, animProgress, poseStack, multiBufferSource, packedOverlay, modelRenderer);
        }

        // Render Matter Decompressor rings: fix (star-synchronous, like Dyson Sphere) + ring (mechanical rotation)
        if (blockEntity.isAmplify() && isMatterDecompressorActive(blockEntity) && bodyData instanceof StarData star) {
            float rotationBoost = blockEntity.getAnimationRotationBoost(partialTick);
            float bodyRot = (blockEntity.getBodyRotation() + partialTick) * rotationBoost;
            renderMatterDecompressorRings(
                centerY,
                rot,
                bodyRot,
                star,
                animProgress,
                poseStack,
                multiBufferSource,
                packedOverlay,
                modelRenderer
            );
        }

        // Use effective body data (considers reverse animation where celestialBodyData is already null)
        CelestialBodyData effectiveBodyData = blockEntity.getEffectiveBodyDataForRendering();
        boolean canRender = effectiveBodyData != null
            && (effectiveBodyData instanceof StarData ? blockEntity.isAmplifierPresent() : true);
        if (canRender) {
            float rotationBoost = blockEntity.getAnimationRotationBoost(partialTick);
            float bodyRot = (blockEntity.getBodyRotation() + partialTick) * rotationBoost;
            // Body scale is driven entirely by StarData.size() which shrinks
            // during collapse via applyCollapseColor() — no additional pose scaling.
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
                    return R4_COLLIDER;
                }
                if ("dyson_sphere_small".equals(option.megastructure())) {
                    return R4_DYSON_SPHERE;
                }
                if ("wormhole_stabilizer".equals(option.megastructure())) {
                    return R4_WORMHOLE_STABILIZER;
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
        // Stellar Evolution Accelerator for small stars replaces ring 5
        if (blockEntity.isAcceleratorActive() && blockEntity.getCelestialBodyData() instanceof StarData star && star.size() < 48) {
            return R5_STELLAR_EVOLUTION_ACCELERATOR;
        }
        return R5;
    }

    /**
     * Get the appropriate ring 6 model, accounting for megastructures that replace ring 6.
     */
    private ModelResourceLocation getRing6Model(CelestialForgingAnvilBlockEntity blockEntity) {
        // Stellar Evolution Accelerator for large stars replaces ring 6
        if (blockEntity.isAcceleratorActive() && blockEntity.getCelestialBodyData() instanceof StarData star && star.size() >= 48) {
            return R6_STELLAR_EVOLUTION_ACCELERATOR;
        }
        return R6;
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
     * Check whether the Magnetar Coil megastructure is active.
     */
    private static boolean isMagnetarCoilActive(CelestialForgingAnvilBlockEntity blockEntity) {
        if (blockEntity.getActiveMegastructureIndex() < 0) return false;
        var option = blockEntity.getActiveMegastructureOption();
        if (option == null) return false;
        return "magnetar_coil".equals(option.megastructure());
    }

    /**
     * Check whether the Penrose Sphere megastructure is active.
     */
    private static boolean isPenroseSphereActive(CelestialForgingAnvilBlockEntity blockEntity) {
        if (blockEntity.getActiveMegastructureIndex() < 0) return false;
        var option = blockEntity.getActiveMegastructureOption();
        if (option == null) return false;
        return "penrose_sphere".equals(option.megastructure());
    }

    /**
     * Check whether the Matter Decompressor megastructure is active.
     */
    private static boolean isMatterDecompressorActive(CelestialForgingAnvilBlockEntity blockEntity) {
        if (blockEntity.getActiveMegastructureIndex() < 0) return false;
        var option = blockEntity.getActiveMegastructureOption();
        if (option == null) return false;
        return "matter_decompressor".equals(option.megastructure());
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
        poseStack.mulPose(Axis.YP.rotationDegrees(bodyRot * CelestialBodyData.getVisualRotationSpeed(star.rotationSpeed())));

        if (renderR4) {
            renderRingCutout(R4_DYSON_SPHERE, poseStack, bufferSource, packedOverlay, modelRenderer);
        }
        if (renderR5) {
            renderRingCutout(R5_DYSON_SPHERE, poseStack, bufferSource, packedOverlay, modelRenderer);
        }
        poseStack.popPose();
    }

    /**
     * Render Magnetar Coil rings with two models at the R4 position:
     * <ul>
     *   <li>{@code ring_4_coil_ring} — mechanical rotation (same bone hierarchy as original R4 inner ring)</li>
     *   <li>{@code ring_4_coil_fix} — star-synchronous rotation (like Dyson Sphere, stays still relative to star)</li>
     * </ul>
     */
    private void renderMagnetarCoilRings(
        float centerY,
        float rot,
        float bodyRot,
        StarData star,
        float scale,
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int packedOverlay,
        ModelBlockRenderer modelRenderer
    ) {
        if (scale < 0.001f) return;

        // === Ring model (mechanical rotation, same as original R4 inner ring) ===
        poseStack.pushPose();
        poseStack.translate(0.5, centerY, 0.5);
        poseStack.scale(6, 6, 6);
        // Apply the full bone hierarchy to position at the "in" bone level
        poseStack.mulPose(Axis.YP.rotationDegrees(-rot));
        poseStack.mulPose(Axis.XP.rotationDegrees(14.5108f));
        poseStack.mulPose(Axis.YP.rotationDegrees(-3.8411f));
        poseStack.mulPose(Axis.ZP.rotationDegrees(14.5109f));
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0f + rot));
        poseStack.mulPose(Axis.ZP.rotationDegrees(rot));
        renderRingCutout(R4_COIL_RING, poseStack, bufferSource, packedOverlay, modelRenderer);
        poseStack.popPose();

        // === Fix model (completely static, no rotation) ===
        poseStack.pushPose();
        poseStack.translate(0.5, centerY, 0.5);
        poseStack.scale(6, 6, 6);
        renderRingCutout(R4_COIL_FIX, poseStack, bufferSource, packedOverlay, modelRenderer);
        poseStack.popPose();
    }

    /**
     * Render Penrose Sphere rings with two models at the R4 position:
     * <ul>
     *   <li>{@code ring_4_penrose_sphere_laser} / {@code ring_4_penrose_sphere_laser_off} —
     *   mechanical rotation (same bone hierarchy as original R4 inner ring)</li>
     *   <li>{@code ring_4_penrose_sphere_fix} — star-synchronous rotation (like Dyson Sphere, stays still relative to the black hole)</li>
     * </ul>
     */
    private void renderPenroseSphereRings(
        float centerY,
        float rot,
        float bodyRot,
        StarData star,
        float scale,
        boolean laserActive,
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int packedOverlay,
        ModelBlockRenderer modelRenderer
    ) {
        if (scale < 0.001f) return;

        // === Laser/Off model (star-synchronous, reverse Y rotation) ===
        poseStack.pushPose();
        poseStack.translate(0.5, centerY, 0.5);
        poseStack.scale(6, 6, 6);
        // Same axial tilt as black hole, but rotate in opposite direction
        poseStack.mulPose(Axis.XP.rotationDegrees(star.axialTilt()));
        poseStack.mulPose(Axis.YP.rotationDegrees(-bodyRot * CelestialBodyData.getVisualRotationSpeed(star.rotationSpeed())));
        ModelResourceLocation laserModel = laserActive ? R4_PENROSE_SPHERE_LASER : R4_PENROSE_SPHERE_LASER_OFF;
        renderRingCutout(laserModel, poseStack, bufferSource, packedOverlay, modelRenderer);
        poseStack.popPose();

        // === Fix model (star-synchronous rotation, same direction as black hole) ===
        poseStack.pushPose();
        poseStack.translate(0.5, centerY, 0.5);
        poseStack.scale(6, 6, 6);
        // Apply star's axial tilt and Y rotation (same direction as black hole)
        poseStack.mulPose(Axis.XP.rotationDegrees(star.axialTilt()));
        poseStack.mulPose(Axis.YP.rotationDegrees(bodyRot * CelestialBodyData.getVisualRotationSpeed(star.rotationSpeed())));
        renderRingCutout(R4_PENROSE_SPHERE_FIX, poseStack, bufferSource, packedOverlay, modelRenderer);
        poseStack.popPose();
    }

    /**
     * Render Matter Decompressor rings with two models at the R4 position:
     * <ul>
     *   <li>{@code ring_4_matter_decompressor_ring} — mechanical rotation (same bone hierarchy as original R4 inner ring)</li>
     *   <li>{@code ring_4_matter_decompressor_fix} —
     *   star-synchronous rotation (like Dyson Sphere, stays still relative to the stellar remnant)</li>
     * </ul>
     */
    private void renderMatterDecompressorRings(
        float centerY,
        float rot,
        float bodyRot,
        StarData star,
        float scale,
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int packedOverlay,
        ModelBlockRenderer modelRenderer
    ) {
        if (scale < 0.001f) return;

        // === Ring model (mechanical rotation, same as original R4 inner ring) ===
        poseStack.pushPose();
        poseStack.translate(0.5, centerY, 0.5);
        poseStack.scale(6, 6, 6);
        // Apply the full bone hierarchy to position at the "in" bone level
        poseStack.mulPose(Axis.YP.rotationDegrees(-rot));
        poseStack.mulPose(Axis.XP.rotationDegrees(14.5108f));
        poseStack.mulPose(Axis.YP.rotationDegrees(-3.8411f));
        poseStack.mulPose(Axis.ZP.rotationDegrees(14.5109f));
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0f + rot));
        poseStack.mulPose(Axis.ZP.rotationDegrees(rot));
        renderRingCutout(R4_MATTER_DECOMPRESSOR_RING, poseStack, bufferSource, packedOverlay, modelRenderer);
        poseStack.popPose();

        // === Fix model (star-synchronous rotation, same direction as stellar remnant) ===
        poseStack.pushPose();
        poseStack.translate(0.5, centerY, 0.5);
        poseStack.scale(6, 6, 6);
        // Apply star's axial tilt and Y rotation (same direction as the body)
        poseStack.mulPose(Axis.XP.rotationDegrees(star.axialTilt()));
        poseStack.mulPose(Axis.YP.rotationDegrees(bodyRot * CelestialBodyData.getVisualRotationSpeed(star.rotationSpeed())));
        renderRingCutout(R4_MATTER_DECOMPRESSOR_FIX, poseStack, bufferSource, packedOverlay, modelRenderer);
        poseStack.popPose();
    }

    /**
     * Determine whether a mechanical ring should be visible for the given body data.
     */
    private static boolean isRingVisible(int ring, @Nullable CelestialBodyData bodyData, boolean isAmplify) {
        if (isAmplify) {
            return switch (ring) {
                case 4 -> bodyData == null || bodyData.size() < 48;
                case 5 -> true;
                case 6 -> bodyData == null || bodyData.size() >= 48;
                default -> false;
            };
        } else {
            if (bodyData == null) return ring >= 1 && ring <= 3;
            return switch (ring) {
                case 1 -> !(bodyData instanceof GiantPlanetData);
                case 2 -> true;
                case 3 -> !(bodyData instanceof RockyPlanetData) && !(bodyData instanceof SpecialCelestialBodyData);
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
        // null prevBody: CFA rings exist before the body — all were visible.
        // Rings that no longer apply fade out; rings that remain stay visible.
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

    private void renderBakedModelCutout(
        ModelResourceLocation modelId,
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int packedOverlay
    ) {
        renderBakedModel(modelId, poseStack, bufferSource, packedOverlay, RenderType.cutout());
    }

    /**
     * Render a baked model with a specified render type.
     */
    @SuppressWarnings("checkstyle:Indentation")
    private void renderBakedModel(
        ModelResourceLocation modelId,
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int packedOverlay,
        RenderType renderType
    ) {
        BakedModel model = Minecraft.getInstance().getModelManager().getModel(modelId);
        if (model == Minecraft.getInstance().getModelManager().getMissingModel()) return;
        Minecraft.getInstance()
            .getBlockRenderer()
            .getModelRenderer()
            .renderModel(
                poseStack.last(),
                bufferSource.getBuffer(renderType),
                null,
                model,
                1.0f,
                1.0f,
                1.0f,
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
        if (bodyData instanceof SpecialCelestialBodyData s && s.isErrorPlanet()) {
            baseScale *= 0.25f;
        }
        float scale = baseScale * animProgress;
        if (scale < 0.001f) {
            poseStack.popPose();
            return;
        }

        poseStack.scale(scale, scale, scale);
        poseStack.mulPose(Axis.XP.rotationDegrees(bodyData.axialTilt()));
        poseStack.mulPose(Axis.YP.rotationDegrees(bodyRotation * CelestialBodyData.getVisualRotationSpeed(bodyData.rotationSpeed())));
        poseStack.translate(-0.5, -0.5, -0.5);

        // Complex custom models (shattered, hollow, flesh, intelligence, error)
        if (bodyData instanceof SpecialCelestialBodyData s && s.needsCustomModel()) {
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
            renderStarModel(star, bodyRotation, poseStack, bufferSource, packedOverlay, seed);
        } else {
            renderPlanetBody(bodyData, poseStack, bufferSource, packedOverlay, seed);
        }
        poseStack.popPose();
    }

    private static final ModelResourceLocation STAR_MODEL = ModelResourceLocation.standalone(AnvilCraft.of("block/celestial_body/star"));

    private static ModelResourceLocation getStarModel(StarData star) {
        if (star.bodyClass() == CelestialBodyClass.NEUTRON_STAR) return NEUTRON_STAR_MODEL;
        if (star.bodyClass() == CelestialBodyClass.BLACK_HOLE) return BLACK_HOLE_MODEL;
        return STAR_MODEL;
    }

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
        BakedModel model = Minecraft.getInstance().getModelManager().getModel(special.getModelLocation());
        if (model == Minecraft.getInstance().getModelManager().getMissingModel()) return;

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.cutout());
        Minecraft.getInstance()
            .getBlockRenderer()
            .getModelRenderer()
            .renderModel(poseStack.last(), consumer, null, model, 1.0f, 1.0f, 1.0f, LightTexture.FULL_BRIGHT, packedOverlay);
    }

    /**
     * Render a star: animated base model + color-tint overlay + halo.
     * The block model provides animation (via .mcmeta), the translucent
     * overlay cube provides the star-specific color from energy anvil count.
     * Special-cases neutron stars and black holes which use dedicated models.
     */
    private void renderStarModel(
        StarData star,
        float bodyRotation,
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int packedOverlay,
        long seed
    ) {
        // Stellar remnants use dedicated models without color overlay or halo
        if (star.bodyClass() == CelestialBodyClass.BLACK_HOLE) {
            renderBakedModel(getStarModel(star), poseStack, bufferSource, packedOverlay, RenderType.translucent());
            return;
        }
        if (star.bodyClass() == CelestialBodyClass.NEUTRON_STAR) {
            renderBakedModelCutout(getStarModel(star), poseStack, bufferSource, packedOverlay);

            // Render relativistic jet along the magnetic axis — only for Super Fast pulsars.
            // Rotation speed 5+ = Super Fast (≥100× visual multiplier), which produces
            // the extreme magnetic field needed for observable relativistic jets.
            if (star.rotationSpeed() >= 5) {
                float visualSpeed = CelestialBodyData.getVisualRotationSpeed(star.rotationSpeed());
                float extraJetRotation = bodyRotation * visualSpeed * 0.5f;
                // Star magneticFieldStrength is 4 (normal) or 5 (magnetar)
                float magneticTilt = star.magneticFieldStrength() >= 5 ? 15f : 10f;
                poseStack.pushPose();
                poseStack.translate(0.5, 0.5, 0.5);
                poseStack.mulPose(Axis.YP.rotationDegrees(extraJetRotation));
                poseStack.mulPose(Axis.XP.rotationDegrees(magneticTilt));
                poseStack.translate(-0.5, -0.5, -0.5);
                renderBakedModel(NEUTRON_STAR_JET_MODEL, poseStack, bufferSource, packedOverlay, RenderType.translucent());
                poseStack.popPose();
            }
            return;
        }

        // Animated grayscale star model (block atlas, supports .mcmeta)
        BakedModel model = Minecraft.getInstance().getModelManager().getModel(getStarModel(star));
        if (model != Minecraft.getInstance().getModelManager().getMissingModel()) {
            VertexConsumer consumer = bufferSource.getBuffer(RenderType.cutout());
            Minecraft.getInstance()
                .getBlockRenderer()
                .getModelRenderer()
                .renderModel(poseStack.last(), consumer, null, model, 1.0f, 1.0f, 1.0f, LightTexture.FULL_BRIGHT, packedOverlay);
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
        poseStack.mulPose(Axis.YP.rotationDegrees(bodyRotation * CelestialBodyData.getVisualRotationSpeed(bodyData.rotationSpeed())));
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
                    poseStack,
                    bufferSource,
                    rgb[0],
                    rgb[1],
                    rgb[2],
                    alpha,
                    LightTexture.FULL_BRIGHT,
                    packedOverlay,
                    seed
                );
                poseStack.popPose();
            }
        }
    }

    /**
     * Renders a cube with multiplicative blending ({@code DST_COLOR * SRC_COLOR}),
     * used for star color overlay to achieve accurate palette-like coloring.
     */
    private void renderColorOverlay(PoseStack poseStack, MultiBufferSource bufferSource, float r, float g, float b, int packedOverlay) {
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
     */
    private float getBodyScale(CelestialBodyData data) {
        // Stellar remnants use fixed scales independent of the stored size
        if (data instanceof StarData star) {
            if (star.bodyClass() == CelestialBodyClass.BLACK_HOLE) {
                return 1.5f;
            }
            if (star.bodyClass() == CelestialBodyClass.NEUTRON_STAR) {
                return 0.8f;
            }
        }
        int size = data.size();
        if (size <= 20) {
            return 1.5f * (0.2f + (size - 1) * 0.8f / 19f);
        } else {
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
    public boolean shouldRenderOffScreen(CelestialForgingAnvilBlockEntity blockEntity) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public boolean shouldRender(CelestialForgingAnvilBlockEntity blockEntity, Vec3 cameraPos) {
        return Vec3.atCenterOf(blockEntity.getBlockPos())
            .multiply(1.0, 0.0, 1.0)
            .closerThan(cameraPos.multiply(1.0, 0.0, 1.0), this.getViewDistance());
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
