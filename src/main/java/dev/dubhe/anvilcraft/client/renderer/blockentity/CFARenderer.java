package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
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
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.CFARenderState;
import dev.dubhe.anvilcraft.client.support.FeatureRendererSupport;
import net.minecraft.client.model.object.skull.SkullModelBase;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 锻星砧的世界内渲染器，将 1.21 的即时渲染实现迁移到 26.1 的提取与提交架构。
 * 负责束星环骨骼层级、红石驱动的平滑缩放、天体自转、动态行星贴图、恒星颜色与光晕、
 * 大气层、天体环、托举光束以及超新星闪光和放射光束。
 */
@SuppressWarnings({"checkstyle:VariableDeclarationUsageDistance", "checkstyle:OverloadMethodsDeclarationOrder"})
public class CFARenderer implements BlockEntityRenderer<CelestialForgingAnvilBlockEntity, CFARenderState> {
    private final @Nullable SkullModelBase playerHeadModel;
    // ==================== 束星环模型 ====================
    public static final StandaloneModelKey<BlockStateModel> RING1 = key("CFA Ring 1");
    public static final StandaloneModelKey<BlockStateModel> RING2 = key("CFA Ring 2");
    public static final StandaloneModelKey<BlockStateModel> RING3 = key("CFA Ring 3");
    public static final StandaloneModelKey<BlockStateModel> RING4 = key("CFA Ring 4");
    public static final StandaloneModelKey<BlockStateModel> RING5 = key("CFA Ring 5");
    public static final StandaloneModelKey<BlockStateModel> RING6 = key("CFA Ring 6");

    // ==================== 巨构模型 ====================
    public static final StandaloneModelKey<BlockStateModel> R1_EXCAVATOR = key("CFA Ring 1 Excavator");
    public static final StandaloneModelKey<BlockStateModel> R1_EXCAVATOR_OFF = key("CFA Ring 1 Excavator Off");
    public static final StandaloneModelKey<BlockStateModel> R1_EXTRACTOR = key("CFA Ring 1 Extractor");
    public static final StandaloneModelKey<BlockStateModel> R2_EXTRACTOR = key("CFA Ring 2 Extractor");
    public static final StandaloneModelKey<BlockStateModel> R1_ECO_STATION = key("CFA Ring 1 Eco Station");
    public static final StandaloneModelKey<BlockStateModel> R1_TEMPLE = key("CFA Ring 1 Temple");
    public static final StandaloneModelKey<BlockStateModel> R4_COLLIDER = key("CFA Ring 4 Collider");
    public static final StandaloneModelKey<BlockStateModel> R4_DYSON_SPHERE = key("CFA Ring 4 Dyson Sphere");
    public static final StandaloneModelKey<BlockStateModel> R5_DYSON_SPHERE = key("CFA Ring 5 Dyson Sphere");
    public static final StandaloneModelKey<BlockStateModel> R4_COIL = key("CFA Ring 4 Coil");
    public static final StandaloneModelKey<BlockStateModel> R4_COIL_FIX = key("CFA Ring 4 Coil Fix");
    public static final StandaloneModelKey<BlockStateModel> R4_COIL_RING = key("CFA Ring 4 Coil Ring");
    public static final StandaloneModelKey<BlockStateModel> R4_PENROSE_SPHERE = key("CFA Ring 4 Penrose Sphere");
    public static final StandaloneModelKey<BlockStateModel> R4_PENROSE_SPHERE_FIX = key("CFA Ring 4 Penrose Fix");
    public static final StandaloneModelKey<BlockStateModel> R4_PENROSE_SPHERE_LASER = key("CFA Ring 4 Penrose Laser");
    public static final StandaloneModelKey<BlockStateModel> R4_PENROSE_SPHERE_LASER_OFF = key(
        "CFA Ring 4 Penrose Laser Off");
    public static final StandaloneModelKey<BlockStateModel> R4_MATTER_DECOMPRESSOR = key(
        "CFA Ring 4 Matter Decompressor");
    public static final StandaloneModelKey<BlockStateModel> R4_MATTER_DECOMPRESSOR_FIX = key(
        "CFA Ring 4 Decompressor Fix");
    public static final StandaloneModelKey<BlockStateModel> R4_MATTER_DECOMPRESSOR_RING = key(
        "CFA Ring 4 Decompressor Ring");
    public static final StandaloneModelKey<BlockStateModel> R4_WORMHOLE_STABILIZER = key(
        "CFA Ring 4 Wormhole Stabilizer");
    public static final StandaloneModelKey<BlockStateModel> R5_ACCELERATOR = key("CFA Ring 5 Accelerator");
    public static final StandaloneModelKey<BlockStateModel> R6_ACCELERATOR = key("CFA Ring 6 Accelerator");

    // ==================== 天体模型 ====================
    public static final StandaloneModelKey<BlockStateModel> BODY_STAR = key("CFA Body Star");
    public static final StandaloneModelKey<BlockStateModel> BODY_NEUTRON_STAR = key("CFA Body Neutron Star");
    public static final StandaloneModelKey<BlockStateModel> BODY_NEUTRON_STAR_JET = key("CFA Body Neutron Star Jet");
    public static final StandaloneModelKey<BlockStateModel> BODY_BLACK_HOLE = key("CFA Body Black Hole");

    // 使用独立复杂模型的特殊天体。
    public static final StandaloneModelKey<BlockStateModel> BODY_PLANET_ARID = key("CFA Body Planet Arid");
    public static final StandaloneModelKey<BlockStateModel> BODY_PLANET_WET = key("CFA Body Planet Wet");
    public static final StandaloneModelKey<BlockStateModel> BODY_PLANET_BOGGY = key("CFA Body Planet Boggy");
    public static final StandaloneModelKey<BlockStateModel> BODY_PLANET_OCEANIC = key("CFA Body Planet Oceanic");
    public static final StandaloneModelKey<BlockStateModel> BODY_PLANET_ATMOSPHERELESS = key(
        "CFA Body Planet Atmosphereless");
    public static final StandaloneModelKey<BlockStateModel> BODY_PLANET_GIANT = key("CFA Body Planet Giant");
    public static final StandaloneModelKey<BlockStateModel> BODY_PLANET_OVERWORLD = key("CFA Body Planet Overworld");
    public static final StandaloneModelKey<BlockStateModel> BODY_PLANET_FLESH = key("CFA Body Planet Flesh");
    public static final StandaloneModelKey<BlockStateModel> BODY_PLANET_INTELLIGENCE = key(
        "CFA Body Planet Intelligence");
    public static final StandaloneModelKey<BlockStateModel> BODY_PLANET_SHATTERED = key("CFA Body Planet Shattered");
    public static final StandaloneModelKey<BlockStateModel> BODY_PLANET_HOLLOW = key("CFA Body Planet Hollow");
    public static final StandaloneModelKey<BlockStateModel> BODY_PLANET_ERROR = key("CFA Body Planet Error");

    private static StandaloneModelKey<BlockStateModel> key(String desc) {
        return new StandaloneModelKey<>(() -> "AnvilCraft: " + desc + " Model");
    }

    public CFARenderer(BlockEntityRendererProvider.Context context) {
        this.playerHeadModel = SkullBlockRenderer.createModel(context.entityModelSet(), SkullBlock.Types.PLAYER);
    }

    @Override
    public CFARenderState createRenderState() {
        return new CFARenderState();
    }

    // 从 1.21 同步的渲染常量。
    private static final float BEAM_BASE_Y = 1.5f;
    private static final float BEAM_INNER_HALF = 0.08f;
    private static final int BEAM_GLOW_LAYERS = 4;
    private static final float BEAM_GLOW_HALF_STEP = 0.06f;
    private static final Map<BlockPos, TractorBeamData> DEFERRED_TRACTOR_BEAMS = new LinkedHashMap<>();
    private static final float SUPERNOVA_MAX_RADIUS = 8.0f;
    private static final int SUPERNOVA_RAY_COUNT = 24;
    private static final float SUPERNOVA_RAY_LENGTH = 12.0f;

    private record TractorBeamData(BlockPos pos, float beamHeight, float animationProgress) {
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
        boolean isAmplify = be.isAmplify();
        state.setAmplified(isAmplify);

        CelestialBodyData bodyData = be.getCelestialBodyData();
        state.setBodyData(bodyData);

        // 红石信号为 0 时使用基础值，信号增强时按 26.1 当前比例放大到完整动态尺寸。
        int redstoneSignal = be.getRedstoneSignal();
        float redstoneFactor = redstoneSignal / 5.0f;
        state.setRedstoneFactor(redstoneFactor);

        float fullRingScale = CelestialBodyData.ringSystemScale(bodyData, isAmplify);
        float fullCenterY = CelestialBodyData.dynamicCenterY(bodyData, isAmplify);
        float baseRingScale = 6.0f;
        float baseCenterY = isAmplify ? 6.5f : 4.5f;
        float ringScale = baseRingScale + (fullRingScale - baseRingScale) * redstoneFactor;
        float centerY = baseCenterY + (fullCenterY - baseCenterY) * redstoneFactor;
        if (isAmplify) {
            centerY += 19.0f * (redstoneSignal / 15.0f);
        }

        float bodyScaleMultiplier = 2.0f;
        if (bodyData != null) {
            float rawBodyScale = bodyData.bodyScale();
            float fullBodyScale = rawBodyScale * CelestialBodyData.BODY_SCALE_FACTOR;
            bodyScaleMultiplier = rawBodyScale + (fullBodyScale - rawBodyScale) * redstoneFactor;
        }

        // 在方块实体上执行与帧率无关的客户端平滑插值。
        float beamHeightTarget = 2.0f + 1.5f * redstoneSignal;
        be.updateRenderSmoothing(ringScale, centerY, bodyScaleMultiplier, beamHeightTarget);
        state.setRingScale(be.getSmoothRingScale());
        state.setCenterY(be.getSmoothCenterY());
        state.setBodyScaleMultiplier(be.getSmoothBodyScale());
        state.setBeamHeight(be.getSmoothBeamHeight());

        // 基础旋转动画。
        float animProgress = be.getAnimationProgress(partialTicks);
        state.setAnimationProgress(animProgress);
        state.setAnimating(be.getAnimationTicks() > 0);
        state.setAnimationForward(be.isAnimationForward());
        CelestialBodyData prevBody = be.getAnimationPreviousBodyData();

        float rotationBoost = be.getAnimationRotationBoost(partialTicks);
        state.setBodyRotation((be.getBodyRotation() + partialTicks) * rotationBoost);

        // 巨构渲染状态。
        String megastructure = be.getActiveMegastructureOption() != null
            ? be.getActiveMegastructureOption().megastructure() : null;
        int activeIndex = be.getActiveMegastructureIndex();
        state.setAcceleratorActive(be.isAcceleratorActive());
        state.setPenroseLaserActive(be.isPenroseSphereLaserActive());
        state.setDysonSphereR4("dyson_sphere_small".equals(megastructure));
        state.setDysonSphereR5("dyson_sphere_large".equals(megastructure));
        state.setMagnetarCoil("magnetar_coil".equals(megastructure));
        state.setPenroseSphere("penrose_sphere".equals(megastructure));
        state.setMatterDecompressor("matter_decompressor".equals(megastructure));

        this.extractRings(be, state, megastructure, activeIndex, bodyData, prevBody);
        this.extractMegastructureRings(be, state, bodyData);
        this.extractBody(be, state, partialTicks);
        this.extractSupernova(be, state, partialTicks);
    }

    /// 解析三层机械束星环模型及其可见性，供淡入淡出过渡使用。
    private void extractRings(
        CelestialForgingAnvilBlockEntity be,
        CFARenderState state,
        @Nullable String megastructure,
        int activeIndex,
        @Nullable CelestialBodyData bodyData,
        @Nullable CelestialBodyData prevBody
    ) {
        boolean isAmplify = state.isAmplified();
        // 外环：普通状态使用 R3，增幅状态使用 R6。
        StandaloneModelKey<BlockStateModel> outerKey = isAmplify ? this.getRing6Model(be) : RING3;
        // 中环：普通状态使用 R2，增幅状态使用 R5。
        StandaloneModelKey<BlockStateModel> middleKey = isAmplify ? this.getRing5Model(be) : this.getRing2Model(be);
        // 内环：普通状态使用 R1，增幅状态使用 R4。
        StandaloneModelKey<BlockStateModel> innerKey = isAmplify ? this.getRing4Model(be) : this.getRing1Model(be);

        int outerIndex = isAmplify ? 6 : 3;
        int middleIndex = isAmplify ? 5 : 2;
        int innerIndex = isAmplify ? 4 : 1;

        state.setOuterRingModel(FeatureRendererSupport.initialize(outerKey, be));
        state.setMiddleRingModel(FeatureRendererSupport.initialize(middleKey, be));
        state.setInnerRingModel(FeatureRendererSupport.initialize(innerKey, be));
        state.setHasOuterRing(true);
        state.setHasMiddleRing(true);
        state.setHasInnerRing(true);

        state.setOuterVisibleNow(isRingVisible(outerIndex, bodyData, isAmplify));
        state.setOuterWasVisible(prevBody == null || isRingVisible(outerIndex, prevBody, isAmplify));
        state.setMiddleVisibleNow(isRingVisible(middleIndex, bodyData, isAmplify));
        state.setMiddleWasVisible(prevBody == null || isRingVisible(middleIndex, prevBody, isAmplify));
        state.setInnerVisibleNow(isRingVisible(innerIndex, bodyData, isAmplify));
        state.setInnerWasVisible(prevBody == null || isRingVisible(innerIndex, prevBody, isAmplify));

        // 特殊巨构使用随恒星同步的额外渲染层替代机械环时，隐藏骨骼层级中的对应环。
        if (isAmplify) {
            boolean anyDyson = state.isDysonSphereR4() || state.isDysonSphereR5();
            boolean isSmallStar = bodyData != null && bodyData.size() < 48;
            // 戴森球隐藏外环；彭罗斯球仅在加速器未工作时隐藏外环。
            boolean hideOuterForPenrose = state.isPenroseSphere() && !state.isAcceleratorActive();
            if (anyDyson || hideOuterForPenrose) {
                state.setHasOuterRing(false);
            }
            // 戴森球隐藏中环；小型恒星的彭罗斯球仅在加速器未工作时隐藏中环。
            boolean hideMiddleForPenrose = state.isPenroseSphere() && isSmallStar && !state.isAcceleratorActive();
            if (anyDyson || hideMiddleForPenrose) {
                state.setHasMiddleRing(false);
            }
            // 小型戴森球、磁星线圈、彭罗斯球和物质解压器使用恒星同步层替代 R4。
            if (state.isDysonSphereR4() || state.isMagnetarCoil()
                || state.isPenroseSphere() || state.isMatterDecompressor()) {
                state.setHasInnerRing(false);
            }
        }
    }

    /// 判断指定天体数据下的机械束星环是否可见。
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

    private StandaloneModelKey<BlockStateModel> getRing1Model(CelestialForgingAnvilBlockEntity be) {
        if (be.getActiveMegastructureIndex() >= 0 && be.getActiveMegastructureOption() != null) {
            String m = be.getActiveMegastructureOption().megastructure();
            switch (m) {
                case "planet_excavator" -> {
                    return be.isExcavatorLaserActive() ? R1_EXCAVATOR : R1_EXCAVATOR_OFF;
                }
                case "planet_exctractor" -> {
                    return R1_EXTRACTOR;
                }
                case "eco_station" -> {
                    return R1_ECO_STATION;
                }
                case "temple" -> {
                    return R1_TEMPLE;
                }
                default -> {
                }
            }
        }
        return RING1;
    }

    private StandaloneModelKey<BlockStateModel> getRing2Model(CelestialForgingAnvilBlockEntity be) {
        if (be.getActiveMegastructureIndex() >= 0 && be.getActiveMegastructureOption() != null
            && "giant_planet_exctractor".equals(be.getActiveMegastructureOption().megastructure())) {
            return R2_EXTRACTOR;
        }
        return RING2;
    }

    private StandaloneModelKey<BlockStateModel> getRing4Model(CelestialForgingAnvilBlockEntity be) {
        if (be.getActiveMegastructureIndex() >= 0 && be.getActiveMegastructureOption() != null) {
            String m = be.getActiveMegastructureOption().megastructure();
            switch (m) {
                case "stellar_ring_collider" -> {
                    return R4_COLLIDER;
                }
                case "dyson_sphere_small" -> {
                    return R4_DYSON_SPHERE;
                }
                case "wormhole_stabilizer" -> {
                    return R4_WORMHOLE_STABILIZER;
                }
                default -> {
                }
            }
        }
        return RING4;
    }

    private StandaloneModelKey<BlockStateModel> getRing5Model(CelestialForgingAnvilBlockEntity be) {
        if (be.getActiveMegastructureIndex() >= 0 && be.getActiveMegastructureOption() != null
            && "dyson_sphere_large".equals(be.getActiveMegastructureOption().megastructure())) {
            return R5_DYSON_SPHERE;
        }
        if (be.isAcceleratorActive() && be.getCelestialBodyData() instanceof StarData star && star.size() < 48) {
            return R5_ACCELERATOR;
        }
        return RING5;
    }

    private StandaloneModelKey<BlockStateModel> getRing6Model(CelestialForgingAnvilBlockEntity be) {
        if (be.isAcceleratorActive() && be.getCelestialBodyData() instanceof StarData star && star.size() >= 48) {
            return R6_ACCELERATOR;
        }
        return RING6;
    }

    /// 初始化戴森球、彭罗斯球、磁星线圈和物质解压器的恒星同步附加模型。
    private void extractMegastructureRings(
        CelestialForgingAnvilBlockEntity be,
        CFARenderState state,
        @Nullable CelestialBodyData bodyData
    ) {
        if (!state.isAmplified() || !(bodyData instanceof StarData)) return;
        if (state.isDysonSphereR4() || state.isDysonSphereR5()) {
            if (state.isDysonSphereR4()) {
                state.setR4DysonModel(FeatureRendererSupport.initialize(R4_DYSON_SPHERE, be));
            }
            if (state.isDysonSphereR5()) {
                state.setR5DysonModel(FeatureRendererSupport.initialize(R5_DYSON_SPHERE, be));
            }
            boolean isSmallStar = bodyData.size() < 48;
            state.setDysonSmallStar(isSmallStar);
            if (state.isDysonSphereR4() && isSmallStar) {
                state.setDysonOuterRingModel(FeatureRendererSupport.initialize(this.getRing5Model(be), be));
            } else if (state.isDysonSphereR5() && !isSmallStar) {
                state.setDysonOuterRingModel(FeatureRendererSupport.initialize(this.getRing6Model(be), be));
            }
        }
        if (state.isPenroseSphere()) {
            state.setPenroseFixModel(FeatureRendererSupport.initialize(R4_PENROSE_SPHERE_FIX, be));
            state.setPenroseLaserModel(FeatureRendererSupport.initialize(
                state.isPenroseLaserActive() ? R4_PENROSE_SPHERE_LASER : R4_PENROSE_SPHERE_LASER_OFF, be));
        }
        if (state.isMagnetarCoil()) {
            state.setCoilFixModel(FeatureRendererSupport.initialize(R4_COIL_FIX, be));
            state.setCoilRingModel(FeatureRendererSupport.initialize(R4_COIL_RING, be));
        }
        if (state.isMatterDecompressor()) {
            state.setDecompressorFixModel(FeatureRendererSupport.initialize(R4_MATTER_DECOMPRESSOR_FIX, be));
            state.setDecompressorRingModel(FeatureRendererSupport.initialize(R4_MATTER_DECOMPRESSOR_RING, be));
        }
    }

    /// 为提交阶段解析天体模型和动态贴图。
    private void extractBody(CelestialForgingAnvilBlockEntity be, CFARenderState state, float partialTicks) {
        CelestialBodyData effectiveBodyData = be.getEffectiveBodyDataForRendering();
        state.setEffectiveBodyData(effectiveBodyData);
        boolean canRender = effectiveBodyData != null
            && (!(effectiveBodyData instanceof StarData) || be.isAmplifierPresent());
        state.setCanRenderBody(canRender);
        if (!canRender) return;

        // 使用动态烘焙贴图的天体环。
        if (effectiveBodyData.ringType() != RingType.NONE) {
            state.setBodyRingTexture(CelestialBodyTextureBakery.getOrBakeRing(effectiveBodyData));
        }

        switch (effectiveBodyData) {
            case SpecialCelestialBodyData special when special.isPlayerHead() -> {
                if (special.playerHeadProfile() != null) {
                    ResolvableProfile profile = ResolvableProfile.CODEC
                        .parse(NbtOps.INSTANCE, special.playerHeadProfile())
                        .result()
                        .orElse(null);
                    state.setPlayerHeadProfile(profile);
                }
            }
            // 粉碎、空洞、血肉、智慧和错误天体使用独立复杂模型。
            case SpecialCelestialBodyData special when special.needsCustomModel() ->
            state.setComplexBodyModel(FeatureRendererSupport.initialize(selectComplexBodyModel(special), be));
            case StarData star -> {
                boolean translucent = star.bodyClass() == CelestialBodyClass.BLACK_HOLE;
                state.setBodyModel(FeatureRendererSupport.initialize(getStarModelKey(star), be, translucent));
                if (star.bodyClass() == CelestialBodyClass.NEUTRON_STAR && star.rotationSpeed() >= 5) {
                    state.setNeutronJetModel(FeatureRendererSupport.initialize(BODY_NEUTRON_STAR_JET, be, true));
                }
            }
            // 普通行星使用色板动态烘焙贴图。
            default -> state.setBodyTexture(CelestialBodyTextureBakery.getOrBakeBody(effectiveBodyData));
        }
    }

    /// 捕获超新星闪光的进度、帧序号和几何参数。
    private void extractSupernova(CelestialForgingAnvilBlockEntity be, CFARenderState state, float partialTicks) {
        int ticks = be.getSupernovaFlashTicks();
        state.setSupernovaFlashTicks(ticks);
        if (ticks <= 0) return;
        int total = CelestialForgingAnvilBlockEntity.SUPERNOVA_FLASH_TICKS;
        float elapsed = (total - ticks + partialTicks);
        float t = Math.clamp(elapsed / total, 0.0f, 1.0f);
        state.setSupernovaProgress(t);
        state.setSupernovaSeed(be.getBlockPos().asLong() ^ 0x5DEECE66DL);

        float scale = be.getSupernovaScale();
        if (scale <= 0f) scale = 1.0f;
        state.setSupernovaScale(scale);
        state.setSupernovaLocalCenterY(be.getSupernovaCenterY() - be.getBlockPos().getY());

        int frame = Math.clamp((int) (t * 8.0f), 0, 7);
        state.setSupernovaFrameTexture(dev.dubhe.anvilcraft.AnvilCraft.of("textures/particle/supernova_" + frame + ".png"));
        float expand = (float) Math.sqrt(t);
        state.setSupernovaFlashRadius(SUPERNOVA_MAX_RADIUS * expand * scale);
        state.setSupernovaFlashAlpha(t > 0.75f ? (1.0f - (t - 0.75f) / 0.25f) : 1.0f);
    }

    private static StandaloneModelKey<BlockStateModel> getStarModelKey(StarData star) {
        if (star.bodyClass() == CelestialBodyClass.NEUTRON_STAR) {
            return BODY_NEUTRON_STAR;
        }
        if (star.bodyClass() == CelestialBodyClass.BLACK_HOLE) {
            return BODY_BLACK_HOLE;
        }
        return BODY_STAR;
    }

    private static StandaloneModelKey<BlockStateModel> selectComplexBodyModel(SpecialCelestialBodyData s) {
        return switch (s.textureName()) {
            case "planet_flesh" -> BODY_PLANET_FLESH;
            case "planet_intelligence" -> BODY_PLANET_INTELLIGENCE;
            case "planet_shattered" -> BODY_PLANET_SHATTERED;
            case "planet_hollow" -> BODY_PLANET_HOLLOW;
            case "planet_error" -> BODY_PLANET_ERROR;
            case "planet_overworld" -> BODY_PLANET_OVERWORLD;
            default -> BODY_PLANET_ERROR;
        };
    }

    @Override
    public void submit(CFARenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        float rot = state.getRotation();
        float ringScale = state.getRingScale();
        float centerY = state.getCenterY();

        pose.pushPose();
        pose.translate(0.5, centerY, 0.5);
        pose.scale(ringScale, ringScale, ringScale);

        // 骨骼层级：最外层绕 Y 轴，外层施加固定倾角，中层绕 X 轴，内层绕 Z 轴。
        pose.mulPose(Axis.YP.rotationDegrees(-rot));
        pose.mulPose(Axis.XP.rotationDegrees(14.5108f));
        pose.mulPose(Axis.YP.rotationDegrees(-3.8411f));
        pose.mulPose(Axis.ZP.rotationDegrees(14.5109f));

        // 最外环是外层骨骼的子节点。
        this.submitRingMaybe(
            state.getOuterRingModel(),
            state.isHasOuterRing(),
            state.isOuterVisibleNow(),
            state.isOuterWasVisible(),
            state,
            pose,
            collector
        );

        // 中层骨骼绕 X 轴旋转。
        pose.mulPose(Axis.XP.rotationDegrees(90.0f + rot));
        this.submitRingMaybe(
            state.getMiddleRingModel(),
            state.isHasMiddleRing(),
            state.isMiddleVisibleNow(),
            state.isMiddleWasVisible(),
            state,
            pose,
            collector
        );

        // 内层骨骼绕 Z 轴旋转。
        pose.mulPose(Axis.ZP.rotationDegrees(rot));
        this.submitRingMaybe(
            state.getInnerRingModel(),
            state.isHasInnerRing(),
            state.isInnerVisibleNow(),
            state.isInnerWasVisible(),
            state,
            pose,
            collector
        );
        pose.popPose();

        // 提交随恒星同步的巨构环渲染层。
        this.submitMegastructureRings(state, pose, collector);

        // 提交天体及其天体环。
        if (state.isCanRenderBody() && state.getEffectiveBodyData() != null) {
            this.submitCelestialBody(state, pose, collector);
            this.submitCelestialRing(state, pose, collector);
        }

        // 仅记录本帧托举光束，在 AFTER_WEATHER 阶段绘制，确保光束位于云层上方。
        if (state.isCanRenderBody() && state.getBeamHeight() > 0.01f && state.getAnimationProgress() > 0.01f) {
            DEFERRED_TRACTOR_BEAMS.put(
                state.blockPos,
                new TractorBeamData(state.blockPos, state.getBeamHeight(), state.getAnimationProgress())
            );
        }

        // 超新星闪光独立于当前天体，即使天体已变为残骸也继续播放。
        if (state.getSupernovaFlashTicks() > 0) {
            this.submitSupernovaFlash(state, pose, collector);
        }
    }

    /// 渲染机械束星环，并在可见性变化时执行淡入淡出。
    private void submitRingMaybe(
        @Nullable BlockModelRenderState model,
        boolean present,
        boolean visibleNow,
        boolean wasVisible,
        CFARenderState state,
        PoseStack pose,
        SubmitNodeCollector collector
    ) {
        if (model == null || !present) return;
        if (!state.isAnimating()) {
            if (visibleNow) {
                submitFullBrightModel(model, false, pose, collector);
            }
            return;
        }
        if (visibleNow && wasVisible) {
            submitFullBrightModel(model, false, pose, collector);
        } else if (visibleNow) {
            float scale = state.isAnimationForward() ? state.getAnimationProgress() : (1.0f - state.getAnimationProgress());
            if (scale > 0.01f) this.submitRingScaled(model, scale, pose, collector, state);
        } else if (wasVisible) {
            float scale = state.isAnimationForward() ? (1.0f - state.getAnimationProgress()) : state.getAnimationProgress();
            if (scale > 0.01f) this.submitRingScaled(model, scale, pose, collector, state);
        }
    }

    private void submitRingScaled(
        BlockModelRenderState model,
        float scale,
        PoseStack pose,
        SubmitNodeCollector collector,
        CFARenderState state
    ) {
        pose.pushPose();
        pose.scale(scale, scale, scale);
        submitFullBrightModel(model, false, pose, collector);
        pose.popPose();
    }

    private static void submitModel(
        BlockModelRenderState model,
        PoseStack pose,
        SubmitNodeCollector collector
    ) {
        model.submit(pose, collector, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
    }

    /**
     * 以全亮光照直接提交模型，避免默认方块模型提交重新引入环境光遮蔽。
     * 半透明模型仍使用带排序和混合的方块图集渲染层。
     */
    private static void submitFullBrightModel(
        BlockModelRenderState model,
        boolean translucent,
        PoseStack pose,
        SubmitNodeCollector collector
    ) {
        model.submitModel(
            translucent ? ModRenderTypes.TRANSLUCENT_BLOCK : ModRenderTypes.CUTOUT_BLOCK,
            pose,
            collector,
            LightCoordsUtil.FULL_BRIGHT,
            OverlayTexture.NO_OVERLAY,
            0
        );
    }

    /// 渲染戴森球、彭罗斯球、磁星线圈和物质解压器的恒星同步层。
    private void submitMegastructureRings(CFARenderState state, PoseStack pose, SubmitNodeCollector collector) {
        if (!state.isAmplified() || !(state.getBodyData() instanceof StarData star)) return;
        float centerY = state.getCenterY();
        float ringScale = state.getRingScale();
        float rot = state.getRotation();
        float bodyRot = state.getBodyRotation();
        float visSpeed = CelestialBodyData.getVisualRotationSpeed(star.rotationSpeed());
        float tilt = star.axialTilt();

        // 戴森球的环与恒星同步旋转，外环也使用恒星同步模型。
        if (state.isDysonSphereR4() || state.isDysonSphereR5()) {
            pushRing(pose, centerY, ringScale);
            pose.mulPose(Axis.XP.rotationDegrees(tilt));
            pose.mulPose(Axis.YP.rotationDegrees(bodyRot * visSpeed));
            if (state.getR4DysonModel() != null) {
                submitFullBrightModel(state.getR4DysonModel(), false, pose, collector);
            }
            if (state.getR5DysonModel() != null) {
                submitFullBrightModel(state.getR5DysonModel(), false, pose, collector);
            }
            pose.popPose();
            if (state.getDysonOuterRingModel() != null) {
                pushRing(pose, centerY, ringScale);
                pose.mulPose(Axis.XP.rotationDegrees(tilt));
                pose.mulPose(Axis.YP.rotationDegrees(bodyRot * visSpeed));
                submitFullBrightModel(state.getDysonOuterRingModel(), false, pose, collector);
                pose.popPose();
            }
        }

        // 彭罗斯球激光层反向同步，固定层正向同步。
        if (state.isPenroseSphere()) {
            if (state.getPenroseLaserModel() != null) {
                pushRing(pose, centerY, ringScale);
                pose.mulPose(Axis.XP.rotationDegrees(tilt));
                pose.mulPose(Axis.YP.rotationDegrees(-bodyRot * visSpeed));
                submitFullBrightModel(state.getPenroseLaserModel(), false, pose, collector);
                pose.popPose();
            }
            if (state.getPenroseFixModel() != null) {
                pushRing(pose, centerY, ringScale);
                pose.mulPose(Axis.XP.rotationDegrees(tilt));
                pose.mulPose(Axis.YP.rotationDegrees(bodyRot * visSpeed));
                submitFullBrightModel(state.getPenroseFixModel(), false, pose, collector);
                pose.popPose();
            }
        }

        // 磁星线圈由机械旋转环和静态固定层组成。
        if (state.isMagnetarCoil()) {
            if (state.getCoilRingModel() != null) {
                pushRing(pose, centerY, ringScale);
                applyMechanicalInnerBone(pose, rot);
                submitFullBrightModel(state.getCoilRingModel(), false, pose, collector);
                pose.popPose();
            }
            if (state.getCoilFixModel() != null) {
                pushRing(pose, centerY, ringScale);
                submitFullBrightModel(state.getCoilFixModel(), false, pose, collector);
                pose.popPose();
            }
        }

        // 物质解压器由机械旋转环和恒星同步固定层组成。
        if (state.isMatterDecompressor()) {
            if (state.getDecompressorRingModel() != null) {
                pushRing(pose, centerY, ringScale);
                applyMechanicalInnerBone(pose, rot);
                submitFullBrightModel(state.getDecompressorRingModel(), false, pose, collector);
                pose.popPose();
            }
            if (state.getDecompressorFixModel() != null) {
                pushRing(pose, centerY, ringScale);
                pose.mulPose(Axis.XP.rotationDegrees(tilt));
                pose.mulPose(Axis.YP.rotationDegrees(bodyRot * visSpeed));
                submitFullBrightModel(state.getDecompressorFixModel(), false, pose, collector);
                pose.popPose();
            }
        }
    }

    private static void pushRing(PoseStack pose, float centerY, float ringScale) {
        pose.pushPose();
        pose.translate(0.5, centerY, 0.5);
        pose.scale(ringScale, ringScale, ringScale);
    }

    /// 应用到内层骨骼为止的完整层级变换，用于机械内环旋转。
    private static void applyMechanicalInnerBone(PoseStack pose, float rot) {
        pose.mulPose(Axis.YP.rotationDegrees(-rot));
        pose.mulPose(Axis.XP.rotationDegrees(14.5108f));
        pose.mulPose(Axis.YP.rotationDegrees(-3.8411f));
        pose.mulPose(Axis.ZP.rotationDegrees(14.5109f));
        pose.mulPose(Axis.XP.rotationDegrees(90.0f + rot));
        pose.mulPose(Axis.ZP.rotationDegrees(rot));
    }

    // ==================== 天体渲染 ====================

    private void submitCelestialBody(CFARenderState state, PoseStack pose, SubmitNodeCollector collector) {
        CelestialBodyData bodyData = state.getEffectiveBodyData();
        if (bodyData == null) return;
        float centerY = state.getCenterY();
        float bodyRot = state.getBodyRotation();
        float animProgress = state.getAnimationProgress();

        float baseScale = state.getBodyScaleMultiplier();
        if (bodyData instanceof SpecialCelestialBodyData s && s.isErrorPlanet()) {
            baseScale *= 0.25f;
        }
        float scale = baseScale * animProgress;
        if (scale < 0.001f) return;

        pose.pushPose();
        pose.translate(0.5, centerY, 0.5);
        pose.scale(scale, scale, scale);
        pose.mulPose(Axis.XP.rotationDegrees(bodyData.axialTilt()));
        pose.mulPose(Axis.YP.rotationDegrees(bodyRot * CelestialBodyData.getVisualRotationSpeed(bodyData.rotationSpeed())));
        pose.translate(-0.5, -0.5, -0.5);

        long seed = state.blockPos.asLong();
        if (bodyData instanceof SpecialCelestialBodyData special && special.needsCustomModel()) {
            if (special.isPlayerHead()) {
                this.submitPlayerHead(state, pose, collector);
            } else {
                if (state.getComplexBodyModel() != null) {
                    submitModel(state.getComplexBodyModel(), pose, collector);
                }
                if (special.hasAtmosphere() && special.temperature() != null) {
                    this.submitAtmosphere(pose, collector, special.temperature(), 1.125f, seed);
                }
            }
        } else if (bodyData instanceof StarData star) {
            this.submitStar(state, star, pose, collector, seed);
        } else {
            this.submitPlanet(state, bodyData, pose, collector, seed);
        }
        pose.popPose();
    }

    private void submitPlayerHead(CFARenderState state, PoseStack pose, SubmitNodeCollector collector) {
        ResolvableProfile profile = state.getPlayerHeadProfile();
        if (profile == null || this.playerHeadModel == null) return;
        pose.pushPose();
        pose.translate(0.5f, 0.25f, 0.5f);
        pose.scale(-1.0f, -1.0f, 1.0f);
        SkullBlockRenderer.submitSkull(
            0.0f,
            pose,
            collector,
            LightCoordsUtil.FULL_BRIGHT,
            this.playerHeadModel,
            net.minecraft.client.Minecraft.getInstance().playerSkinRenderCache().getOrDefault(profile).renderType(),
            0,
            null
        );
        pose.popPose();
    }

    private void submitStar(
        CFARenderState state,
        StarData star,
        PoseStack pose,
        SubmitNodeCollector collector,
        long seed
    ) {
        // 黑洞和中子星使用独立烘焙模型，不叠加普通恒星颜色或光晕。
        if (star.bodyClass() == CelestialBodyClass.BLACK_HOLE) {
            if (state.getBodyModel() != null) {
                submitFullBrightModel(state.getBodyModel(), true, pose, collector);
            }
            return;
        }
        if (star.bodyClass() == CelestialBodyClass.NEUTRON_STAR) {
            if (state.getBodyModel() != null) {
                submitFullBrightModel(state.getBodyModel(), false, pose, collector);
            }
            if (state.getNeutronJetModel() != null) {
                float visualSpeed = CelestialBodyData.getVisualRotationSpeed(star.rotationSpeed());
                float extraJetRotation = state.getBodyRotation() * visualSpeed * 0.5f;
                float magneticTilt = star.magneticFieldStrength() >= 5 ? 15f : 10f;
                pose.pushPose();
                pose.translate(0.5, 0.5, 0.5);
                pose.mulPose(Axis.YP.rotationDegrees(extraJetRotation));
                pose.mulPose(Axis.XP.rotationDegrees(magneticTilt));
                pose.translate(-0.5, -0.5, -0.5);
                submitFullBrightModel(state.getNeutronJetModel(), true, pose, collector);
                pose.popPose();
            }
            return;
        }

        // 主序星使用带动画的灰度烘焙模型。
        if (state.getBodyModel() != null) {
            submitFullBrightModel(state.getBodyModel(), false, pose, collector);
        }

        // 叠加乘法恒星颜色。
        float[] rgb = CelestialBodyRenderer.getStarColor(star);
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.scale(1.005f, 1.005f, 1.005f);
        pose.translate(-0.5, -0.5, -0.5);
        this.submitColorOverlay(pose, collector, rgb[0], rgb[1], rgb[2]);
        pose.popPose();

        // 绘制恒星光晕。
        int haloIterations = 10;
        for (int i = 0; i < haloIterations; i++) {
            float progress = (float) i / haloIterations;
            float haloScale = 1.0f + progress * 0.6f;
            float alpha = (1.2f - 1.125f * progress) / haloIterations;
            pose.pushPose();
            pose.translate(0.5, 0.5, 0.5);
            pose.scale(haloScale, haloScale, haloScale);
            pose.translate(-0.5, -0.5, -0.5);
            this.submitTranslucentCube(pose, collector, rgb[0], rgb[1], rgb[2], alpha);
            pose.popPose();
        }
    }

    private void submitPlanet(
        CFARenderState state,
        CelestialBodyData bodyData,
        PoseStack pose,
        SubmitNodeCollector collector,
        long seed
    ) {
        Identifier bodyTexture = state.getBodyTexture();
        if (bodyTexture != null) {
            collector.submitCustomGeometry(
                pose, ModRenderTypes.STAR_CUTOUT.apply(bodyTexture),
                (last, consumer) -> CelestialBodyRenderer.renderPlanetBody(
                    last,
                    consumer,
                    LightCoordsUtil.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY
                )
            );
        }

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
            this.submitAtmosphere(pose, collector, atmosTemp, 1.125f, seed);
        }

        // 褐矮星使用较弱的恒星式光晕。
        if (bodyData instanceof GiantPlanetData gp && gp.brownDwarf()) {
            float[] rgb = CelestialBodyRenderer.getAtmosphereColor(Temperature.SCORCHED);
            int haloIterations = 3;
            for (int i = 0; i < haloIterations; i++) {
                float progress = (float) i / haloIterations;
                float haloScale = 1.15f + progress * 0.25f;
                float alpha = (0.45f - 0.38f * progress) / haloIterations;
                pose.pushPose();
                pose.translate(0.5, 0.5, 0.5);
                pose.scale(haloScale, haloScale, haloScale);
                pose.translate(-0.5, -0.5, -0.5);
                this.submitTranslucentCube(pose, collector, rgb[0], rgb[1], rgb[2], alpha);
                pose.popPose();
            }
        }
    }

    private void submitCelestialRing(CFARenderState state, PoseStack pose, SubmitNodeCollector collector) {
        CelestialBodyData bodyData = state.getEffectiveBodyData();
        if (bodyData == null || bodyData.ringType() == RingType.NONE) return;
        Identifier ringTexture = state.getBodyRingTexture();
        if (ringTexture == null) return;

        pose.pushPose();
        pose.translate(0.5, state.getCenterY(), 0.5);
        float ringMultiplier = switch (bodyData) {
            case RockyPlanetData rp -> 1.35f;
            case GiantPlanetData gp -> 1.3f;
            default -> 1.4f;
        };
        float ringScale = state.getBodyScaleMultiplier() * ringMultiplier * state.getAnimationProgress();
        if (ringScale < 0.001f) {
            pose.popPose();
            return;
        }
        pose.scale(ringScale, ringScale, ringScale);
        pose.mulPose(Axis.XP.rotationDegrees(bodyData.axialTilt()));
        pose.mulPose(Axis.YP.rotationDegrees(
            state.getBodyRotation() * CelestialBodyData.getVisualRotationSpeed(bodyData.rotationSpeed())));
        pose.translate(-0.5, -0.5, -0.5);

        collector.submitCustomGeometry(
            pose, ModRenderTypes.CELESTIAL_RING.apply(ringTexture),
            (last, consumer) -> CelestialBodyRenderer.renderRing(
                last, consumer, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY)
        );
        pose.popPose();
    }

    private void submitColorOverlay(PoseStack pose, SubmitNodeCollector collector, float r, float g, float b) {
        collector.submitCustomGeometry(
            pose, ModRenderTypes.STAR_COLOR_OVERLAY,
            (last, consumer) -> CelestialBodyRenderer.renderColorCube(
                last, consumer, r, g, b, 1.0f, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY)
        );
    }

    private void submitTranslucentCube(
        PoseStack pose,
        SubmitNodeCollector collector,
        float r,
        float g,
        float b,
        float a
    ) {
        collector.submitCustomGeometry(
            pose,
            ModRenderTypes.CELESTIAL_ATMOSPHERE,
            (last, consumer) -> CelestialBodyRenderer.renderColorCube(
                last,
                consumer,
                r,
                g,
                b,
                a,
                LightCoordsUtil.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY
            )
        );
    }

    private void submitAtmosphere(
        PoseStack pose,
        SubmitNodeCollector collector,
        Temperature temp,
        float scale,
        long seed
    ) {
        float[] rgb = CelestialBodyRenderer.getAtmosphereColor(temp);
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.scale(scale, scale, scale);
        pose.translate(-0.5, -0.5, -0.5);
        collector.submitCustomGeometry(
            pose, ModRenderTypes.CELESTIAL_ATMOSPHERE,
            (last, consumer) -> CelestialBodyRenderer.renderAtmosphereCube(
                last,
                consumer,
                rgb,
                0.2f,
                LightCoordsUtil.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY
            )
        );
        pose.popPose();
    }

    /// 将当前姿态包装为单层姿态栈，以适配天体渲染工具的参数形式。
    private static PoseStack poseOf(PoseStack.Pose last) {
        PoseStack ps = new PoseStack();
        ps.last().set(last);
        return ps;
    }

    // ==================== 超新星闪光与放射光束 ====================

    private void submitSupernovaFlash(CFARenderState state, PoseStack pose, SubmitNodeCollector collector) {
        float radius = state.getSupernovaFlashRadius();
        float alpha = state.getSupernovaFlashAlpha();
        Identifier tex = state.getSupernovaFrameTexture();
        double localCenterY = state.getSupernovaLocalCenterY();

        if (tex != null && radius >= 0.01f && alpha > 0.01f) {
            pose.pushPose();
            pose.translate(0.5, localCenterY, 0.5);
            float r = radius;
            float a = alpha;
            collector.submitCustomGeometry(
                pose, ModRenderTypes.SUPERNOVA_FLASH.apply(tex),
                (last, consumer) -> emitFlatQuad(consumer, last, r, a, LightCoordsUtil.FULL_BRIGHT)
            );
            pose.popPose();
        }

        // 绘制类似末影龙死亡效果的向外放射光束。
        float t = state.getSupernovaProgress();
        float grow = (float) Math.sqrt(t);
        float scale = state.getSupernovaScale();
        float length = SUPERNOVA_RAY_LENGTH * grow * scale;
        float intensity = t > 0.6f ? (1.0f - (t - 0.6f) / 0.4f) : 1.0f;
        if (length < 0.01f || intensity <= 0.01f) return;

        pose.pushPose();
        pose.translate(0.5, localCenterY, 0.5);
        long seed = state.getSupernovaSeed();
        float baseWidth = 0.25f * scale;
        float rayLen = length;
        float rayIntensity = intensity;
        collector.submitCustomGeometry(
            pose, ModRenderTypes.STELLAR_BEAM, (last, consumer) -> {
                Matrix4f matrix = last.pose();
                RandomSource rand = RandomSource.create(seed);
                for (int i = 0; i < SUPERNOVA_RAY_COUNT; i++) {
                    float u = rand.nextFloat() * 2.0f - 1.0f;
                    float theta = rand.nextFloat() * (float) (Math.PI * 2.0);
                    float s = (float) Math.sqrt(1.0f - u * u);
                    float dx = s * (float) Math.cos(theta);
                    float dy = u;
                    float dz = s * (float) Math.sin(theta);
                    float len = rayLen * (0.7f + 0.6f * rand.nextFloat());
                    float rayI = rayIntensity * (0.5f + 0.5f * rand.nextFloat());
                    emitRay(
                        consumer, matrix, dx, dy, dz, len, baseWidth,
                        0.12f * rayI, 0.22f * rayI, 0.26f * rayI
                    );
                }
            }
        );
        pose.popPose();
    }

    /// 从原点沿指定方向绘制尖端收束的细四棱锥光束。
    private static void emitRay(
        VertexConsumer consumer,
        Matrix4f matrix,
        float dx,
        float dy,
        float dz,
        float length,
        float halfWidth,
        float r,
        float g,
        float b
    ) {
        Vector3f dir = new Vector3f(dx, dy, dz).normalize();
        Vector3f up = Math.abs(dir.y) > 0.99f ? new Vector3f(1f, 0f, 0f) : new Vector3f(0f, 1f, 0f);
        Vector3f n1 = new Vector3f(dir).cross(up).normalize().mul(halfWidth);
        Vector3f n2 = new Vector3f(dir).cross(n1).normalize().mul(halfWidth);
        Vector3f tip = new Vector3f(dir).mul(length);

        float[][] base = {
            {-n1.x - n2.x, -n1.y - n2.y, -n1.z - n2.z},
            {n1.x - n2.x, n1.y - n2.y, n1.z - n2.z},
            {n1.x + n2.x, n1.y + n2.y, n1.z + n2.z},
            {-n1.x + n2.x, -n1.y + n2.y, -n1.z + n2.z}
        };
        for (int i = 0; i < 4; i++) {
            float[] c0 = base[i];
            float[] c1 = base[(i + 1) % 4];
            beamVertex(consumer, matrix, c0[0], c0[1], c0[2], r, g, b, 1.0f);
            beamVertex(consumer, matrix, c1[0], c1[1], c1[2], r, g, b, 1.0f);
            beamVertex(consumer, matrix, tip.x, tip.y, tip.z, 0f, 0f, 0f, 1.0f);
            beamVertex(consumer, matrix, tip.x, tip.y, tip.z, 0f, 0f, 0f, 1.0f);
        }
    }

    private static void beamVertex(
        VertexConsumer consumer,
        Matrix4f matrix,
        float x,
        float y,
        float z,
        float r,
        float g,
        float b,
        float a
    ) {
        consumer.addVertex(matrix, x, y, z).setColor(r, g, b, a);
    }

    /// 在 XZ 平面绘制以原点为中心、边长为两倍半径且法线向上的水平四边形。
    private static void emitFlatQuad(
        VertexConsumer vc,
        PoseStack.Pose pose,
        float r,
        float alpha,
        int light
    ) {
        int overlay = OverlayTexture.NO_OVERLAY;
        vc.addVertex(pose, -r, 0f, -r).setColor(1.0f, 1.0f, 1.0f, alpha).setUv(0f, 0f)
            .setOverlay(overlay).setLight(light).setNormal(pose, 0f, 1f, 0f);
        vc.addVertex(pose, -r, 0f, r).setColor(1.0f, 1.0f, 1.0f, alpha).setUv(0f, 1f)
            .setOverlay(overlay).setLight(light).setNormal(pose, 0f, 1f, 0f);
        vc.addVertex(pose, r, 0f, r).setColor(1.0f, 1.0f, 1.0f, alpha).setUv(1f, 1f)
            .setOverlay(overlay).setLight(light).setNormal(pose, 0f, 1f, 0f);
        vc.addVertex(pose, r, 0f, -r).setColor(1.0f, 1.0f, 1.0f, alpha).setUv(1f, 0f)
            .setOverlay(overlay).setLight(light).setNormal(pose, 0f, 1f, 0f);
    }

    // ==================== 托举光束 ====================

    /**
     * 在天气与云层完成后渲染本帧收集的锻星砧托举光束。
     */
    public static void renderDeferredTractorBeams(
        PoseStack pose,
        MultiBufferSource.BufferSource bufferSource,
        Vec3 cameraPosition
    ) {
        if (DEFERRED_TRACTOR_BEAMS.isEmpty()) return;
        VertexConsumer consumer = bufferSource.getBuffer(ModRenderTypes.STELLAR_BEAM);
        for (TractorBeamData data : DEFERRED_TRACTOR_BEAMS.values()) {
            pose.pushPose();
            pose.translate(
                data.pos().getX() - cameraPosition.x(),
                data.pos().getY() - cameraPosition.y(),
                data.pos().getZ() - cameraPosition.z()
            );
            emitTractorBeam(consumer, pose.last().pose(), data.beamHeight(), data.animationProgress());
            pose.popPose();
        }
        bufferSource.endBatch(ModRenderTypes.STELLAR_BEAM);
        DEFERRED_TRACTOR_BEAMS.clear();
    }

    private static void emitTractorBeam(
        VertexConsumer consumer,
        Matrix4f matrix,
        float beamHeight,
        float animProgress
    ) {
        float apexY = BEAM_BASE_Y + beamHeight;
        for (int layer = BEAM_GLOW_LAYERS; layer >= 1; layer--) {
            float half = BEAM_INNER_HALF + BEAM_GLOW_HALF_STEP * layer;
            float falloff = 1.0f / (layer + 1);
            falloff *= falloff * 2.0f;
            float r = 0.045f * falloff * animProgress;
            float g = 0.10f * falloff * animProgress;
            float b = 0.13f * falloff * animProgress;
            emitBeamPyramid(consumer, matrix, half, apexY, r, g, b, 0.12f);
        }
        emitBeamPyramid(
            consumer, matrix, BEAM_INNER_HALF, apexY,
            0.11f * animProgress, 0.20f * animProgress, 0.23f * animProgress, 0.22f
        );
    }

    /// 发射一个以 (0.5,0.5) 为水平中心的四棱锥：方形底面在 BEAM_BASE_Y、半宽 halfWidth，锥尖在 (0.5, apexY, 0.5)。
    private static void emitBeamPyramid(
        VertexConsumer vc,
        Matrix4f matrix,
        float halfWidth,
        float apexY,
        float r,
        float g,
        float b,
        float apexFade
    ) {
        float cx = 0.5f;
        float cz = 0.5f;
        float x0 = cx - halfWidth;
        float x1 = cx + halfWidth;
        float z0 = cz - halfWidth;
        float z1 = cz + halfWidth;
        float[][] corners = {{x0, z0}, {x1, z0}, {x1, z1}, {x0, z1}};
        float ar = r * apexFade;
        float ag = g * apexFade;
        float ab = b * apexFade;
        for (int i = 0; i < 4; i++) {
            float[] c0 = corners[i];
            float[] c1 = corners[(i + 1) % 4];
            beamVertex(vc, matrix, c0[0], BEAM_BASE_Y, c0[1], r, g, b, 1.0f);
            beamVertex(vc, matrix, c1[0], BEAM_BASE_Y, c1[1], r, g, b, 1.0f);
            beamVertex(vc, matrix, cx, apexY, cz, ar, ag, ab, 1.0f);
            beamVertex(vc, matrix, cx, apexY, cz, ar, ag, ab, 1.0f);
        }
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 512;
    }

    @Override
    public AABB getRenderBoundingBox(CelestialForgingAnvilBlockEntity be) {
        BlockState state = be.getBlockState();
        CelestialBodyData body = be.getCelestialBodyData();
        float centerY = CelestialBodyData.dynamicCenterY(body, be.isAmplify());
        if (be.isAmplify()) {
            centerY += 19.0f * (be.getRedstoneSignal() / 15.0f);
        }
        float bs = body != null ? body.bodyScale() * CelestialBodyData.BODY_SCALE_FACTOR : 6.0f;
        // 红石信号最大 3× 缩放后的天体和星环可能远大于 1×，渲染包围盒需留足余量。
        float bsMax = bs * 3.0f;
        float ringMax = CelestialBodyData.ringSystemScale(body, be.isAmplify()) * 3.0f;
        float maxHorizontal = Math.max(bsMax, ringMax) * 1.5f;
        float maxHeight = Math.max(centerY + bsMax * 1.5f, be.isAmplify() ? 73.0f : 36.0f);
        if (be.getSupernovaFlashTicks() > 0) {
            float explosionScale = Math.max(1.0f, be.getSupernovaScale());
            float reach = Math.max(SUPERNOVA_MAX_RADIUS, SUPERNOVA_RAY_LENGTH)
                * explosionScale * 1.5f + 2;
            double cy = be.getSupernovaCenterY();
            return new AABB(
                be.getBlockPos().getX() + 0.5, cy, be.getBlockPos().getZ() + 0.5,
                be.getBlockPos().getX() + 0.5, cy, be.getBlockPos().getZ() + 0.5
            ).inflate(reach);
        }
        AABB aabb = new AABB(be.getBlockPos().offset(state.getValue(CelestialForgingAnvilBlock.HALF).getOffset()))
            .inflate(maxHorizontal, 0, maxHorizontal);
        return aabb.setMaxY(aabb.maxY + maxHeight);
    }
}
