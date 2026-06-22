package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.cfa.CelestialForgingAnvilBlock;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyData;
import dev.dubhe.anvilcraft.block.entity.celestial.StarData;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.CFARenderState;
import dev.dubhe.anvilcraft.client.support.FeatureRendererSupport;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import org.jspecify.annotations.Nullable;

public class CFARenderer implements BlockEntityRenderer<CelestialForgingAnvilBlockEntity, CFARenderState> {
    // === Ring models ===
    public static final StandaloneModelKey<BlockStateModel> RING1 = key("CFA Ring 1");
    public static final StandaloneModelKey<BlockStateModel> RING2 = key("CFA Ring 2");
    public static final StandaloneModelKey<BlockStateModel> RING3 = key("CFA Ring 3");
    public static final StandaloneModelKey<BlockStateModel> RING4 = key("CFA Ring 4");
    public static final StandaloneModelKey<BlockStateModel> RING5 = key("CFA Ring 5");
    public static final StandaloneModelKey<BlockStateModel> RING6 = key("CFA Ring 6");

    // === Megastructure models — excavator ===
    public static final StandaloneModelKey<BlockStateModel> R1_EXCAVATOR = key("CFA Ring 1 Excavator");
    public static final StandaloneModelKey<BlockStateModel> R1_EXCAVATOR_OFF = key("CFA Ring 1 Excavator Off");

    // === Megastructure models — extractor ===
    public static final StandaloneModelKey<BlockStateModel> R1_EXTRACTOR = key("CFA Ring 1 Extractor");
    public static final StandaloneModelKey<BlockStateModel> R2_EXTRACTOR = key("CFA Ring 2 Extractor");

    // === Megastructure models — eco station ===
    public static final StandaloneModelKey<BlockStateModel> R1_ECO_STATION = key("CFA Ring 1 Eco Station");

    // === Megastructure models — temple ===
    public static final StandaloneModelKey<BlockStateModel> R1_TEMPLE = key("CFA Ring 1 Temple");

    // === Megastructure models — collider ===
    public static final StandaloneModelKey<BlockStateModel> R4_COLLIDER = key("CFA Ring 4 Collider");

    // === Megastructure models — dyson sphere ===
    public static final StandaloneModelKey<BlockStateModel> R4_DYSON_SPHERE = key("CFA Ring 4 Dyson Sphere");
    public static final StandaloneModelKey<BlockStateModel> R5_DYSON_SPHERE = key("CFA Ring 5 Dyson Sphere");

    // === Megastructure models — magnetar coil ===
    public static final StandaloneModelKey<BlockStateModel> R4_COIL_FIX = key("CFA Ring 4 Coil Fix");
    public static final StandaloneModelKey<BlockStateModel> R4_COIL_RING = key("CFA Ring 4 Coil Ring");

    // === Megastructure models — penrose sphere ===
    public static final StandaloneModelKey<BlockStateModel> R4_PENROSE_SPHERE_FIX = key("CFA Ring 4 Penrose Fix");
    public static final StandaloneModelKey<BlockStateModel> R4_PENROSE_SPHERE_LASER = key("CFA Ring 4 Penrose Laser");
    public static final StandaloneModelKey<BlockStateModel> R4_PENROSE_SPHERE_LASER_OFF = key("CFA Ring 4 Penrose Laser Off");

    // === Megastructure models — matter decompressor ===
    public static final StandaloneModelKey<BlockStateModel> R4_MATTER_DECOMPRESSOR_FIX = key("CFA Ring 4 Decompressor Fix");
    public static final StandaloneModelKey<BlockStateModel> R4_MATTER_DECOMPRESSOR_RING = key("CFA Ring 4 Decompressor Ring");

    // === Megastructure models — wormhole stabilizer ===
    public static final StandaloneModelKey<BlockStateModel> R4_WORMHOLE_STABILIZER = key("CFA Ring 4 Wormhole Stabilizer");

    // === Megastructure models — accelerator ===
    public static final StandaloneModelKey<BlockStateModel> R5_ACCELERATOR = key("CFA Ring 5 Accelerator");
    public static final StandaloneModelKey<BlockStateModel> R6_ACCELERATOR = key("CFA Ring 6 Accelerator");

    // === Celestial body models ===
    public static final StandaloneModelKey<BlockStateModel> BODY_NEUTRON_STAR = key("CFA Body Neutron Star");
    public static final StandaloneModelKey<BlockStateModel> BODY_NEUTRON_STAR_JET = key("CFA Body Neutron Star Jet");
    public static final StandaloneModelKey<BlockStateModel> BODY_BLACK_HOLE = key("CFA Body Black Hole");

    private static StandaloneModelKey<BlockStateModel> key(String desc) {
        return new StandaloneModelKey<>(() -> "AnvilCraft: " + desc + " Model");
    }

    public CFARenderer(BlockEntityRendererProvider.Context ignored) {
    }

    @Override
    public CFARenderState createRenderState() {
        return new CFARenderState();
    }

    @Override
    public void extractRenderState(
        CelestialForgingAnvilBlockEntity be,
        CFARenderState state,
        float partialTicks,
        Vec3 cameraPosition,
        ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, cameraPosition, breakProgress);

        float rot = be.getRotation() + (be.getRotation() - be.getPreRotation()) * partialTicks;
        state.setRotation(rot);
        boolean amplified = be.isAmplify();
        state.setAmplified(amplified);
        state.setOffsetY(amplified ? 6.5 : 4.5);

        CelestialBodyData bodyData = be.getCelestialBodyData();

        // === Megastructure detection ===
        String megastructure = null;
        if (be.getActiveMegastructureOption() != null) {
            megastructure = be.getActiveMegastructureOption().megastructure();
        }
        int activeIndex = be.getActiveMegastructureIndex();

        // === Ring model selection ===
        // Ring 1 (innermost, non-amplified): depends on megastructure
        if (!amplified) {
            StandaloneModelKey<BlockStateModel> r1Key = RING1;
            if (megastructure != null) {
                r1Key = switch (megastructure) {
                    case "planet_excavator" -> be.isExcavatorLaserActive() ? R1_EXCAVATOR : R1_EXCAVATOR_OFF;
                    case "planet_extractor" -> R1_EXTRACTOR;
                    case "eco_station" -> R1_ECO_STATION;
                    case "temple" -> R1_TEMPLE;
                    default -> RING1;
                };
            }
            state.setSmall(FeatureRendererSupport.initialize(r1Key, be));
        }

        // Ring 2 (middle, non-amplified)
        if (!amplified) {
            StandaloneModelKey<BlockStateModel> r2Key = RING2;
            if ("giant_extractor".equals(megastructure)) r2Key = R2_EXTRACTOR;
            state.setBig(FeatureRendererSupport.initialize(r2Key, be));
        }

        // Ring 3 (outmost, non-amplified) — only when no megastructure on ring 2
        if (!amplified && activeIndex < 0) {
            state.setRing4Model(FeatureRendererSupport.initialize(RING3, be));
            state.setHasRing4(true);
        }

        // Amplified rings
        if (amplified) {
            // Ring 4 (innermost, amplified)
            StandaloneModelKey<BlockStateModel> r4Key = RING4;
            if (megastructure != null) {
                r4Key = switch (megastructure) {
                    case "stellar_ring_collider" -> R4_COLLIDER;
                    case "dyson_sphere_small" -> R4_DYSON_SPHERE;
                    case "magnetar_coil" -> R4_COIL_FIX;
                    case "penrose_sphere" -> be.isPenroseSphereLaserActive()
                        ? R4_PENROSE_SPHERE_LASER : R4_PENROSE_SPHERE_LASER_OFF;
                    case "matter_decompressor" -> R4_MATTER_DECOMPRESSOR_FIX;
                    case "wormhole_stabilizer" -> R4_WORMHOLE_STABILIZER;
                    default -> RING4;
                };
            }
            state.setRing4Model(FeatureRendererSupport.initialize(r4Key, be));
            state.setHasRing4(true);

            // Ring 5 (middle, amplified)
            StandaloneModelKey<BlockStateModel> r5Key = RING5;
            if ("stellar_evolution_accelerator".equals(megastructure) && be.isAcceleratorActive()) {
                r5Key = R5_ACCELERATOR;
            } else if ("dyson_sphere_large".equals(megastructure)) {
                r5Key = R5_DYSON_SPHERE;
            } else if ("dyson_sphere_small".equals(megastructure)) {
                state.setHasRing5(false); // small dyson sphere has its own ring
            }
            if (state.isHasRing5() || megastructure == null || "dyson_sphere_large".equals(megastructure)) {
                state.setRing5Model(FeatureRendererSupport.initialize(r5Key, be));
                state.setHasRing5(true);
            }

            // Ring 6 (outmost, amplified) — only if no megastructure there
            if ("stellar_evolution_accelerator".equals(megastructure) && be.isAcceleratorActive()) {
                state.setRing6Model(FeatureRendererSupport.initialize(R6_ACCELERATOR, be));
                state.setHasRing6(true);
            } else if (activeIndex < 0) {
                state.setRing6Model(FeatureRendererSupport.initialize(RING6, be));
                state.setHasRing6(true);
            }
        }

        // === Celestial body state ===
        if (bodyData != null) {
            state.setHasBody(true);
            state.setBodyRotation(be.getBodyRotation() + partialTicks);
            state.setBodyAxialTilt(bodyData.axialTilt());
            state.setBodyScale(bodyData.size() / 32.0f);
            if (bodyData instanceof StarData star) {
                state.setBodyColorR(star.colorR());
                state.setBodyColorG(star.colorG());
                state.setBodyColorB(star.colorB());
            }
            state.setAnimationProgress(be.getAnimationProgress(partialTicks));
            state.setAnimationRotationBoost(be.getAnimationRotationBoost(partialTicks));
        } else {
            state.setHasBody(false);
        }

        // === Excavator/Penrose laser state ===
        state.setExcavatorLaserActive(be.isExcavatorLaserActive());
        state.setPenroseSphereLaserActive(be.isPenroseSphereLaserActive());

        // === Accelerator state ===
        state.setAcceleratorStage(be.getAcceleratorStage());
        state.setCollapseAnimTicks(be.getCollapseAnimTicks());
        state.setSupernovaFlashTicks(be.getSupernovaFlashTicks());
    }

    @Override
    public void submit(CFARenderState state, PoseStack pose, SubmitNodeCollector collector,
                       CameraRenderState camera) {
        pose.pushPose();
        pose.translate(0.5, state.getOffsetY(), 0.5);
        float rot = state.getRotation();

        // === Outermost ring (Ring 3 non-amplified, Ring 6 amplified) ===
        float outermostRot = state.isAmplified() ? -rot : -rot;
        pose.mulPose(Axis.YP.rotationDegrees(outermostRot));
        pose.mulPose(Axis.XP.rotationDegrees(14.5f));
        pose.mulPose(Axis.YP.rotationDegrees(-3.84f));
        pose.mulPose(Axis.ZP.rotationDegrees(14.5f));

        if (state.isAmplified()) {
            if (state.isHasRing6() && state.getRing6Model() != null) {
                submitModel(state.getRing6Model(), pose, collector, state);
            }
        } else {
            if (state.isHasRing4() && state.getRing4Model() != null) {
                submitModel(state.getRing4Model(), pose, collector, state);
            }
        }

        // === Middle ring ===
        pose.mulPose(Axis.XP.rotationDegrees(90.0f + rot));
        if (state.isAmplified()) {
            if (state.isHasRing5() && state.getRing5Model() != null) {
                submitModel(state.getRing5Model(), pose, collector, state);
            }
        } else {
            if (state.getBig() != null) {
                state.getBig().submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            }
        }

        // === Innermost ring ===
        pose.mulPose(Axis.ZP.rotationDegrees(rot));
        if (state.isAmplified()) {
            if (state.getRing4Model() != null) {
                submitModel(state.getRing4Model(), pose, collector, state);
            }
        } else {
            if (state.getSmall() != null) {
                state.getSmall().submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            }
        }

        // === Celestial body placeholder ===
        if (state.isHasBody() && state.getBodyModel() != null) {
            pose.pushPose();
            pose.scale(state.getBodyScale(), state.getBodyScale(), state.getBodyScale());
            pose.mulPose(Axis.YP.rotationDegrees(state.getBodyRotation()));
            submitModel(state.getBodyModel(), pose, collector, state);
            pose.popPose();
        }

        pose.popPose();
    }

    private static void submitModel(net.minecraft.client.renderer.block.BlockModelRenderState model,
                                    PoseStack pose, SubmitNodeCollector collector, CFARenderState state) {
        model.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
    }

    @Override
    public AABB getRenderBoundingBox(CelestialForgingAnvilBlockEntity blockEntity) {
        BlockState bs = blockEntity.getBlockState();
        if (!blockEntity.isAmplify()) {
            AABB aabb = new AABB(
                blockEntity.getBlockPos().offset(bs.getValue(CelestialForgingAnvilBlock.HALF).getOffset())
            ).inflate(1, 0, 1);
            return aabb.setMaxY(aabb.maxY + 5);
        }
        AABB aabb = new AABB(
            blockEntity.getBlockPos().offset(bs.getValue(CelestialForgingAnvilBlock.HALF).getOffset())
        ).inflate(3, 0, 3);
        return aabb.setMaxY(aabb.maxY + 7);
    }
}
