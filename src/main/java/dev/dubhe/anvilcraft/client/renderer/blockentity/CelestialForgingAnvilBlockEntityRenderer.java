package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.cfa.CelestialForgingAnvilBlock;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyClass;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyData;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialRefactorOption;
import dev.dubhe.anvilcraft.block.entity.celestial.GiantPlanetData;
import dev.dubhe.anvilcraft.block.entity.celestial.RingType;
import dev.dubhe.anvilcraft.block.entity.celestial.RockyPlanetData;
import dev.dubhe.anvilcraft.block.entity.celestial.SpecialCelestialBodyData;
import dev.dubhe.anvilcraft.block.entity.celestial.StarData;
import dev.dubhe.anvilcraft.block.entity.celestial.StellarEventProfile;
import dev.dubhe.anvilcraft.block.entity.celestial.StellarEvolutionState;
import dev.dubhe.anvilcraft.block.entity.celestial.StellarTrack;
import dev.dubhe.anvilcraft.block.entity.celestial.StellarTrackLibrary;
import dev.dubhe.anvilcraft.block.entity.celestial.StellarVisualState;
import dev.dubhe.anvilcraft.block.entity.celestial.Temperature;
import dev.dubhe.anvilcraft.client.init.ModRenderTypes;
import dev.dubhe.anvilcraft.client.renderer.blockentity.celestial.CelestialBodyRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.celestial.CelestialBodyTextureBakery;
import dev.dubhe.anvilcraft.init.ModMegastructures;
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
import net.minecraft.util.ColorRGBA;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Vector3f;

import javax.annotation.Nullable;

@SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
public class CelestialForgingAnvilBlockEntityRenderer implements BlockEntityRenderer<CelestialForgingAnvilBlockEntity> {
    public static final ModelResourceLocation R1 = ModelResourceLocation.standalone(AnvilCraft.of("block/celestial_forging_anvil_ring_1"));
    public static final ModelResourceLocation R2 = ModelResourceLocation.standalone(AnvilCraft.of("block/celestial_forging_anvil_ring_2"));
    public static final ModelResourceLocation R3 = ModelResourceLocation.standalone(AnvilCraft.of("block/celestial_forging_anvil_ring_3"));
    public static final ModelResourceLocation R4 = ModelResourceLocation.standalone(AnvilCraft.of("block/celestial_forging_anvil_ring_4"));
    public static final ModelResourceLocation R5 = ModelResourceLocation.standalone(AnvilCraft.of("block/celestial_forging_anvil_ring_5"));
    public static final ModelResourceLocation R6 = ModelResourceLocation.standalone(AnvilCraft.of("block/celestial_forging_anvil_ring_6"));

    /// 抽取器巨构建材模型
    public static final ModelResourceLocation R1_EXCAVATOR = ModelResourceLocation.standalone(AnvilCraft.of(
        "block/celestial_forging_anvil_ring_1_excavator"));
    public static final ModelResourceLocation R1_EXCAVATOR_OFF = ModelResourceLocation.standalone(AnvilCraft.of(
        "block/celestial_forging_anvil_ring_1_excavator_off"));

    /// 提取器巨构建材模型
    public static final ModelResourceLocation R1_EXCTRACTOR = ModelResourceLocation.standalone(AnvilCraft.of(
        "block/celestial_forging_anvil_ring_1_exctractor"));

    /// 生态站巨构建材模型
    public static final ModelResourceLocation R1_ECO_STATION = ModelResourceLocation.standalone(AnvilCraft.of(
        "block/celestial_forging_anvil_ring_1_eco_station"));

    /// 神殿巨构建材模型
    public static final ModelResourceLocation R1_TEMPLE = ModelResourceLocation.standalone(AnvilCraft.of(
        "block/celestial_forging_anvil_ring_1_temple"));

    /// 巨型提取器巨构建材模型（替换第二环）
    public static final ModelResourceLocation R2_EXCTRACTOR = ModelResourceLocation.standalone(AnvilCraft.of(
        "block/celestial_forging_anvil_ring_2_exctractor"));

    /// 恒星环对撞机巨构建材模型（替换第四环）
    public static final ModelResourceLocation R4_COLLIDER = ModelResourceLocation.standalone(AnvilCraft.of(
        "block/celestial_forging_anvil_ring_4_collider"));

    /// 戴森球巨构建材模型（替换第四环/第五环）
    public static final ModelResourceLocation R2_DYSON_SPHERE = ModelResourceLocation.standalone(AnvilCraft.of(
        "block/celestial_forging_anvil_ring_2_dyson_sphere"));
    public static final ModelResourceLocation R4_DYSON_SPHERE = ModelResourceLocation.standalone(AnvilCraft.of(
        "block/celestial_forging_anvil_ring_4_dyson_sphere"));
    public static final ModelResourceLocation R5_DYSON_SPHERE = ModelResourceLocation.standalone(AnvilCraft.of(
        "block/celestial_forging_anvil_ring_5_dyson_sphere"));

    /// 磁星线圈巨构建材模型（替换第四环，双模型）
    public static final ModelResourceLocation R4_COIL_FIX = ModelResourceLocation.standalone(AnvilCraft.of(
        "block/celestial_forging_anvil_ring_4_coil_fix"));
    public static final ModelResourceLocation R4_COIL_RING = ModelResourceLocation.standalone(AnvilCraft.of(
        "block/celestial_forging_anvil_ring_4_coil_ring"));

    /// 彭罗斯球巨构建材模型（替换第四环，双模型）
    public static final ModelResourceLocation R4_PENROSE_SPHERE_FIX = ModelResourceLocation.standalone(AnvilCraft.of(
        "block/celestial_forging_anvil_ring_4_penrose_sphere_fix"));
    public static final ModelResourceLocation R4_PENROSE_SPHERE_LASER = ModelResourceLocation.standalone(AnvilCraft.of(
        "block/celestial_forging_anvil_ring_4_penrose_sphere_laser"));
    public static final ModelResourceLocation R4_PENROSE_SPHERE_LASER_OFF = ModelResourceLocation.standalone(AnvilCraft.of(
        "block/celestial_forging_anvil_ring_4_penrose_sphere_laser_off"));

    /// 物质解压器巨构建材模型（替换第四环，双模型）
    public static final ModelResourceLocation R4_MATTER_DECOMPRESSOR_FIX = ModelResourceLocation.standalone(AnvilCraft.of(
        "block/celestial_forging_anvil_ring_4_matter_decompressor_fix"));
    public static final ModelResourceLocation R4_MATTER_DECOMPRESSOR_RING = ModelResourceLocation.standalone(AnvilCraft.of(
        "block/celestial_forging_anvil_ring_4_matter_decompressor_ring"));

    /// 虫洞稳定器巨构建材模型（替换第四环）
    public static final ModelResourceLocation R4_WORMHOLE_STABILIZER = ModelResourceLocation.standalone(AnvilCraft.of(
        "block/celestial_forging_anvil_ring_4_wormhole_stabilizer"));

    /// 恒星演化加速器模型
    public static final ModelResourceLocation R5_STELLAR_EVOLUTION_ACCELERATOR = ModelResourceLocation.standalone(AnvilCraft.of(
        "block/celestial_forging_anvil_ring_5_stellar_evolution_accelerator"));
    public static final ModelResourceLocation R6_STELLAR_EVOLUTION_ACCELERATOR = ModelResourceLocation.standalone(AnvilCraft.of(
        "block/celestial_forging_anvil_ring_6_stellar_evolution_accelerator"));

    /// 恒星残骸天体模型
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
    private static final float PLAYER_HEAD_SCALE = 16.0f;
    private static final float PLAYER_HEAD_HALF_HEIGHT = 0.25f;

    private final BlockRenderDispatcher blockRenderer;
    private final BlockState whiteConcrete = Blocks.WHITE_CONCRETE.defaultBlockState();

    public CelestialForgingAnvilBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = context.getBlockRenderDispatcher();
    }

    /// 环缩放和动态中心高度委托给 CelestialBodyData 统一计算（渲染与引力共用）。
    private static float ringSystemScale(@Nullable CelestialBodyData bodyData, boolean isAmplify) {
        return CelestialBodyData.ringSystemScale(bodyData, isAmplify);
    }

    private static float dynamicCenterY(@Nullable CelestialBodyData bodyData, boolean isAmplify) {
        return CelestialBodyData.dynamicCenterY(bodyData, isAmplify);
    }

    private static float bodyScale(CelestialBodyData data) {
        return data.bodyScale();
    }

    /** 使用视觉半径计算恒星环系统，确保环、中心和本体采用同一浮点快照。 */
    private static float stellarRingSystemScale(float visualBodyScale) {
        return CelestialBodyData.ringSystemScaleForVisualBodyScale(visualBodyScale);
    }

    private static float stellarCenterY(float ringScale, boolean isAmplify) {
        return CelestialBodyData.centerYForRingScale(ringScale, isAmplify);
    }

    /// 把原始视觉缩放按红石信号插值到完整渲染缩放。
    private static float redstoneScaled(float rawBodyScale, float redstoneFactor) {
        float fullBodyScale = rawBodyScale * CelestialBodyData.BODY_SCALE_FACTOR;
        return rawBodyScale + (fullBodyScale - rawBodyScale) * redstoneFactor;
    }

    private static float playerHeadCenterY(
        SpecialCelestialBodyData playerHead,
        boolean isAmplify,
        float bodyScaleMultiplier,
        float animationProgress
    ) {
        float initialHeadScale = bodyScale(playerHead) * PLAYER_HEAD_SCALE;
        float currentHeadScale = bodyScaleMultiplier * PLAYER_HEAD_SCALE;
        float baseCenterY = isAmplify ? 6.5f : 4.5f;
        return baseCenterY + (currentHeadScale - initialHeadScale) * animationProgress * PLAYER_HEAD_HALF_HEIGHT;
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
        CelestialBodyData bodyData = blockEntity.getCelestialBodyData();
        boolean isAmplify = blockEntity.isAmplify();
        float rotationBoost = blockEntity.getAnimationRotationBoost(partialTick);
        float bodyRotation = (blockEntity.getBodyRotation() + partialTick) * rotationBoost;
        int outerRing = isAmplify ? 6 : 3;
        int middleRing = isAmplify ? 5 : 2;
        int innerRing = isAmplify ? 4 : 1;
        float outerRotation = getMegastructureRotation(blockEntity, outerRing, rot, bodyRotation);
        float middleRotation = getMegastructureRotation(blockEntity, middleRing, rot, bodyRotation);
        float innerRotation = getMegastructureRotation(blockEntity, innerRing, rot, bodyRotation);

        @Nullable StellarEvolutionState evolutionState = blockEntity.getStellarEvolutionState();
        @Nullable StellarTrack evolutionTrack = blockEntity.getMegastructureManager().getAcceleratorHandler().getEvolutionTrack();
        /// 只有演化中的恒星使用浮点快照和程序化光球；静态恒星继续使用旧的动画光晕。
        @Nullable StellarVisualState stellarVisual = bodyData instanceof StarData && evolutionState != null
            ? blockEntity.getStellarVisualState(partialTick)
            : null;
        float visualRawBodyScale = bodyData == null ? 2.0f : bodyScale(bodyData);
        float structuralBodyScale = visualRawBodyScale;
        if (bodyData instanceof StarData && stellarVisual != null && evolutionState != null && evolutionTrack != null) {
            // 环和中心只读取结构半径；本体另行读取含脉动的半径。
            structuralBodyScale = blockEntity.getStellarStructuralBodyScale(partialTick);
            visualRawBodyScale = blockEntity.getStellarPulsatingBodyScale(partialTick);
        }

        /// 红石信号控制缩放倍率：0 级时使用固定原始值（天体=最小，环=6，高度=4.5/6.5），
        /// 15 级时达到完整动态缩放。中间级别线性插值。
        float redstoneFactor = blockEntity.getRedstoneSignal() / 15.0f;

        float fullRingScale = bodyData instanceof StarData && stellarVisual != null
            ? stellarRingSystemScale(structuralBodyScale)
            : ringSystemScale(bodyData, isAmplify);
        float fullCenterY = bodyData instanceof StarData && stellarVisual != null
            ? stellarCenterY(fullRingScale, isAmplify)
            : dynamicCenterY(bodyData, isAmplify);
        float baseRingScale = 6.0f;
        float baseCenterY = isAmplify ? 6.5f : 4.5f;
        float ringScale = baseRingScale + (fullRingScale - baseRingScale) * redstoneFactor;
        float centerY = baseCenterY + (fullCenterY - baseCenterY) * redstoneFactor;

        /// 天体缩放跟随红石信号：0 级为原始比例（bodyScale，最小~0.3 格 / 最大~3.9 格），
        /// 15 级为完整放大（getBodyScale = bodyScale × CelestialBodyData.BODY_SCALE_FACTOR）。
        /// 各尺寸天体的比例关系在所有红石级别下保持不变。
        float bodyScaleMultiplier = 2.0f; // 无天体时的默认值
        if (bodyData != null) {
            bodyScaleMultiplier = redstoneScaled(visualRawBodyScale, redstoneFactor);
        }

        /// 演化中的恒星半径跨越数量级，而红石基准值（环=6、高度=4.5/6.5）只适配发现时的
        /// 离散尺寸。膨胀期必须用实际渲染出来的本体反推环缩放和中心高度的下限，
        /// 否则恒星会长到束星环外并穿进锻星砧。结构包络已经是本阶段的最大半径（脉冲只
        /// 向内收缩），所以环在整个阶段保持同一尺寸，只需再留出事件核心膨胀的余量。
        @Nullable StellarEventProfile eventProfile = stellarVisual == null
            ? null
            : blockEntity.getStellarEventProfile();
        if (stellarVisual != null) {
            float eventReach = eventProfile == null ? 1.0f : Math.max(1.0f, eventProfile.maxCoreRadius());
            float peakBodyScale = redstoneScaled(structuralBodyScale, redstoneFactor) * eventReach;
            float ringFloor = CelestialBodyData.ringScaleForRenderedBodyScale(peakBodyScale);
            if (ringFloor > ringScale) ringScale = ringFloor;
            centerY = Math.max(centerY, stellarCenterY(ringScale, isAmplify));
        }

        /// 渲染端平滑：对环缩放、中心高度、天体缩放、光束高度做帧率无关的指数逼近，
        /// 让红石信号引起的尺寸/高度变化丝滑过渡（数百帧），而非每 tick 瞬间跳变。
        /// 光束高度 = 2 格 + 每级红石信号 0.5 格。
        float beamHeightTarget = 2.0f + 0.5f * blockEntity.getRedstoneSignal();
        blockEntity.updateRenderSmoothing(ringScale, centerY, bodyScaleMultiplier, beamHeightTarget);
        ringScale = blockEntity.getSmoothRingScale();
        centerY = blockEntity.getSmoothCenterY();
        bodyScaleMultiplier = blockEntity.getSmoothBodyScale();
        float beamHeight = blockEntity.getSmoothBeamHeight();

        poseStack.pushPose();
        poseStack.translate(0.5, centerY, 0.5);

        /// 追踪前一个天体与动画状态，用于圆环淡入淡出过渡
        CelestialBodyData prevBody = blockEntity.getAnimationPreviousBodyData();
        float animProgress = blockEntity.getAnimationProgress(partialTick);
        boolean isAnimating = blockEntity.getAnimationTicks() > 0;
        boolean animForward = blockEntity.isAnimationForward();

        /// 基于 Blockbench 动画构建圆环装配姿态，在各骨骼层级渲染圆环，
        /// 使每个圆环获得不同的累积旋转 —— 从而三环交叉永不平共面。
        /// 骨骼层级：outout（Y 旋转）→ out（静态倾斜）→ mid（X 旋转）→ in（Z 旋转）
        /// 圆环挂载：外环 → out，中环 → mid，内环 → in
        poseStack.scale(ringScale, ringScale, ringScale);

        /// outout + out：根骨骼 + 静态倾斜（外骨骼）
        poseStack.mulPose(Axis.YP.rotationDegrees(-outerRotation));        /// outout：Y 轴旋转
        poseStack.mulPose(Axis.XP.rotationDegrees(14.5108f));    /// out：静态倾斜 X
        poseStack.mulPose(Axis.YP.rotationDegrees(-3.8411f));    /// out：静态倾斜 Y
        poseStack.mulPose(Axis.ZP.rotationDegrees(14.5109f));    /// out：静态倾斜 Z

        /// === 最外层圆环 —— "out" 的子骨骼 ===
        if (blockEntity.isAmplify()) {
            boolean isDysonSphereR5 = isDysonSphereActive(blockEntity, 5);
            boolean anyDysonSphere = isDysonSphereActive(blockEntity, 4) || isDysonSphereR5;
            boolean hideOutermostForPenrose = isPenroseSphereActive(blockEntity)
                && !blockEntity.hasActiveAuxiliaryMegastructure();
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
        } else if (!isBrownDwarfDysonSphereActive(blockEntity)) {
            renderRingMaybe(
                getRing3Model(blockEntity),
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

        /// mid：X 旋转（中骨骼）
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0f + middleRotation));

        /// === 中间圆环 —— "mid" 的子骨骼 ===
        if (blockEntity.isAmplify()) {
            boolean anyDysonSphere = isDysonSphereActive(blockEntity, 4) || isDysonSphereActive(blockEntity, 5);
            /// 当彭罗斯球激活且第五环为最外层（小恒星）时隐藏第五环
            boolean isSmallStar = bodyData != null && bodyData.size() < 48;
            boolean hideMiddleForPenrose = isPenroseSphereActive(blockEntity)
                && isSmallStar
                && !blockEntity.hasActiveAuxiliaryMegastructure();
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
        } else if (!isBrownDwarfDysonSphereActive(blockEntity)) {
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

        /// in：Z 旋转（内骨骼）
        poseStack.mulPose(Axis.ZP.rotationDegrees(innerRotation));

        /// === 最内层圆环 —— "in" 的子骨骼 ===
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

        /// 渲染戴森球圆环，采用与恒星同步的旋转。
        /// 戴森球模型替换内环；外环（小恒星用 R5，大恒星用 R6）也在此
        /// 以与恒星同步旋转方式渲染，而非隐藏。
        if (blockEntity.isAmplify()) {
            boolean isDysonSphereR4 = isDysonSphereActive(blockEntity, 4);
            boolean isDysonSphereR5 = isDysonSphereActive(blockEntity, 5);
            if ((isDysonSphereR4 || isDysonSphereR5) && bodyData instanceof StarData star) {
                float bodyRot = bodyRotation;
                renderDysonSphereRings(
                    isDysonSphereR4,
                    isDysonSphereR5,
                    centerY,
                    bodyRot,
                    star,
                    animProgress,
                    ringScale,
                    poseStack,
                    multiBufferSource,
                    packedOverlay,
                    modelRenderer
                );
                /// 渲染外环，与恒星同步旋转（类似真实戴森球）
                boolean isSmallStar = bodyData.size() < 48;
                if (isDysonSphereR4 && isSmallStar) {
                    /// 小恒星：R5 为外环（可能被加速器模型替换）
                    poseStack.pushPose();
                    poseStack.translate(0.5, centerY, 0.5);
                    poseStack.scale(ringScale, ringScale, ringScale);
                    poseStack.mulPose(Axis.XP.rotationDegrees(star.axialTilt()));
                    poseStack.mulPose(
                        Axis.YP.rotationDegrees(bodyRot * CelestialBodyData.getVisualRotationSpeed(star.rotationSpeed())));
                    renderRingCutout(getRing5Model(blockEntity), poseStack, multiBufferSource, packedOverlay, modelRenderer);
                    poseStack.popPose();
                } else if (isDysonSphereR5 && !isSmallStar) {
                    /// 大恒星：R6 为外环（可能被加速器模型替换）
                    poseStack.pushPose();
                    poseStack.translate(0.5, centerY, 0.5);
                    poseStack.scale(ringScale, ringScale, ringScale);
                    poseStack.mulPose(Axis.XP.rotationDegrees(star.axialTilt()));
                    poseStack.mulPose(
                        Axis.YP.rotationDegrees(bodyRot * CelestialBodyData.getVisualRotationSpeed(star.rotationSpeed())));
                    renderRingCutout(getRing6Model(blockEntity), poseStack, multiBufferSource, packedOverlay, modelRenderer);
                    poseStack.popPose();
                }
            }
        }

        /// 褐矮星戴森球和外环与褐矮星同步旋转，替换非增幅模式的 R2/R3 机械圆环。
        if (!blockEntity.isAmplify()
            && isBrownDwarfDysonSphereActive(blockEntity)
            && bodyData != null) {
            renderBrownDwarfDysonSphereRings(
                centerY,
                bodyRotation,
                bodyData,
                animProgress,
                redstoneFactor,
                ringScale,
                poseStack,
                multiBufferSource,
                packedOverlay,
                modelRenderer,
                blockEntity
            );
        }

        /// 渲染彭罗斯球圆环：fix（恒星同步）+ laser/off（机械旋转）
        if (blockEntity.isAmplify() && isPenroseSphereActive(blockEntity) && bodyData instanceof StarData star) {
            float bodyRot = bodyRotation;
            renderPenroseSphereRings(centerY, rot, bodyRot, star, animProgress,
                blockEntity.isPenroseSphereLaserActive(), ringScale,
                poseStack, multiBufferSource, packedOverlay, modelRenderer);
        }

        /// 渲染磁星线圈圆环：fix（恒星同步）+ ring（机械旋转）
        if (blockEntity.isAmplify() && isMagnetarCoilActive(blockEntity) && bodyData instanceof StarData star) {
            float bodyRot = bodyRotation;
            renderMagnetarCoilRings(
                centerY,
                innerRotation,
                bodyRot,
                star,
                animProgress,
                ringScale,
                poseStack,
                multiBufferSource,
                packedOverlay,
                modelRenderer
            );
        }

        /// 渲染物质解压器圆环：fix（恒星同步，类似戴森球）+ ring（机械旋转）
        if (blockEntity.isAmplify() && isMatterDecompressorActive(blockEntity) && bodyData instanceof StarData star) {
            float bodyRot = bodyRotation;
            renderMatterDecompressorRings(
                centerY,
                innerRotation,
                bodyRot,
                star,
                animProgress,
                ringScale,
                poseStack,
                multiBufferSource,
                packedOverlay,
                modelRenderer
            );
        }

        /// 使用有效天体数据（考虑 celestialBodyData 已置为 null 的逆向动画情形）
        CelestialBodyData effectiveBodyData = blockEntity.getEffectiveBodyDataForRendering();
        boolean canRender = effectiveBodyData != null
            && (!(effectiveBodyData instanceof StarData star)
                || blockEntity.isAmplifierPresent()
                || star.specialRedDwarf());
        if (canRender) {
            renderTractorBeam(beamHeight, animProgress, poseStack, multiBufferSource);
            float eventProgress = blockEntity.getStellarEventProgress(partialTick);
            float bodyCenterY = centerY;
            if (effectiveBodyData instanceof SpecialCelestialBodyData special && special.isPlayerHead()) {
                bodyCenterY = playerHeadCenterY(special, isAmplify, bodyScaleMultiplier, animProgress);
            }
            renderCelestialBody(
                effectiveBodyData,
                bodyCenterY,
                bodyRotation,
                poseStack,
                multiBufferSource,
                packedOverlay,
                blockEntity.getBlockPos().asLong(),
                animProgress,
                bodyScaleMultiplier,
                stellarVisual,
                eventProfile,
                eventProgress
            );
            renderCelestialRing(
                effectiveBodyData,
                centerY,
                bodyRotation,
                poseStack,
                multiBufferSource,
                packedOverlay,
                animProgress,
                bodyScaleMultiplier
            );
        }

        /// 超新星爆发闪光：水平展开的动画 billboard，中心跟随天体中心、大小随红石缩放。
        /// 即使天体已被残骸替换也要播放，故置于 canRender 之外。
        if (blockEntity.getSupernovaFlashTicks() > 0) {
            renderSupernovaFlash(blockEntity, partialTick, poseStack, multiBufferSource);
        }

    }

    /// 获取第一环对应的模型，考虑巨构替换。
    private ModelResourceLocation getRing1Model(CelestialForgingAnvilBlockEntity blockEntity) {
        ModelResourceLocation active = getActiveRingModel(blockEntity, 1);
        if (active != null) return active;
        ModelResourceLocation auxiliary = getActiveAuxiliaryRingModel(blockEntity, 1);
        return auxiliary == null ? R1 : auxiliary;
    }

    /// 获取第二环对应的模型，考虑巨构替换。
    private ModelResourceLocation getRing2Model(CelestialForgingAnvilBlockEntity blockEntity) {
        ModelResourceLocation active = getActiveRingModel(blockEntity, 2);
        if (active != null) return active;
        ModelResourceLocation auxiliary = getActiveAuxiliaryRingModel(blockEntity, 2);
        return auxiliary == null ? R2 : auxiliary;
    }

    private ModelResourceLocation getRing3Model(CelestialForgingAnvilBlockEntity blockEntity) {
        ModelResourceLocation active = getActiveRingModel(blockEntity, 3);
        if (active != null) return active;
        ModelResourceLocation auxiliary = getActiveAuxiliaryRingModel(blockEntity, 3);
        return auxiliary == null ? R3 : auxiliary;
    }

    /// 获取第四环对应的模型，考虑替换第四环的巨构。
    private ModelResourceLocation getRing4Model(CelestialForgingAnvilBlockEntity blockEntity) {
        ModelResourceLocation active = getActiveRingModel(blockEntity, 4);
        if (active != null) return active;
        ModelResourceLocation auxiliary = getActiveAuxiliaryRingModel(blockEntity, 4);
        return auxiliary == null ? R4 : auxiliary;
    }

    /// 获取第五环对应的模型，考虑替换第五环的巨构。
    private ModelResourceLocation getRing5Model(CelestialForgingAnvilBlockEntity blockEntity) {
        ModelResourceLocation active = getActiveRingModel(blockEntity, 5);
        if (active != null) return active;
        ModelResourceLocation auxiliary = getActiveAuxiliaryRingModel(blockEntity, 5);
        return auxiliary == null ? R5 : auxiliary;
    }

    /// 获取第六环对应的模型，考虑替换第六环的巨构。
    private ModelResourceLocation getRing6Model(CelestialForgingAnvilBlockEntity blockEntity) {
        ModelResourceLocation active = getActiveRingModel(blockEntity, 6);
        if (active != null) return active;
        ModelResourceLocation auxiliary = getActiveAuxiliaryRingModel(blockEntity, 6);
        return auxiliary == null ? R6 : auxiliary;
    }

    private @Nullable ModelResourceLocation getActiveRingModel(
        CelestialForgingAnvilBlockEntity blockEntity,
        int ring
    ) {
        CelestialRefactorOption option = blockEntity.getActiveMegastructureOption();
        if (option == null || option.ring() != ring) return null;
        if (ModMegastructures.PLANET_EXCAVATOR.getId().equals(option.id())
            && !blockEntity.isExcavatorLaserActive()) {
            return R1_EXCAVATOR_OFF;
        }
        return ModelResourceLocation.standalone(option.modelLocation());
    }

    /**
     * Resolves the rotation for a mechanical ring, allowing both the active
     * primary structure and an active auxiliary to provide custom
     * rotation functions.  A ring without a structure keeps the base bone
     * animation used by the vanilla CFA model.
     */
    private float getMegastructureRotation(
        CelestialForgingAnvilBlockEntity blockEntity,
        int ring,
        float baseRotation,
        float bodyRotation
    ) {
        CelestialRefactorOption active = blockEntity.getActiveMegastructureOption();
        if (active != null && active.ring() == ring) {
            return active.rotation(baseRotation, bodyRotation);
        }
        CelestialRefactorOption auxiliary = blockEntity.getActiveAuxiliaryMegastructureOption(ring);
        return auxiliary != null
            ? auxiliary.rotation(baseRotation, bodyRotation)
            : baseRotation;
    }

    private @Nullable ModelResourceLocation getActiveAuxiliaryRingModel(
        CelestialForgingAnvilBlockEntity blockEntity,
        int ring
    ) {
        CelestialRefactorOption option = blockEntity.getActiveAuxiliaryMegastructureOption(ring);
        if (option != null) return ModelResourceLocation.standalone(option.modelLocation());
        if (!blockEntity.isAcceleratorActive()) return null;
        if (blockEntity.getCelestialBodyData() instanceof StarData star) {
            if (ring == 5 && star.size() < 48) return R5_STELLAR_EVOLUTION_ACCELERATOR;
            if (ring == 6 && star.size() >= 48) return R6_STELLAR_EVOLUTION_ACCELERATOR;
        }
        return null;
    }

    /// 检查指定环索引上的戴森球巨构是否激活。
    private static boolean isDysonSphereActive(CelestialForgingAnvilBlockEntity blockEntity, int ring) {
        CelestialRefactorOption option = blockEntity.getActiveMegastructureOption();
        if (option == null) return false;
        if (ring == 4) return ModMegastructures.DYSON_SPHERE_SMALL.getId().equals(option.id());
        if (ring == 5) return ModMegastructures.DYSON_SPHERE_LARGE.getId().equals(option.id());
        return false;
    }

    private static boolean isBrownDwarfDysonSphereActive(CelestialForgingAnvilBlockEntity blockEntity) {
        CelestialRefactorOption option = blockEntity.getActiveMegastructureOption();
        return option != null && ModMegastructures.DYSON_SPHERE_BROWN_DWARF.getId().equals(option.id());
    }

    /// 检查磁星线圈巨构是否激活。
    private static boolean isMagnetarCoilActive(CelestialForgingAnvilBlockEntity blockEntity) {
        CelestialRefactorOption option = blockEntity.getActiveMegastructureOption();
        return option != null && ModMegastructures.MAGNETAR_COIL.getId().equals(option.id());
    }

    /// 检查彭罗斯球巨构是否激活。
    private static boolean isPenroseSphereActive(CelestialForgingAnvilBlockEntity blockEntity) {
        CelestialRefactorOption option = blockEntity.getActiveMegastructureOption();
        return option != null && ModMegastructures.PENROSE_SPHERE.getId().equals(option.id());
    }

    /// 检查物质解压器巨构是否激活。
    private static boolean isMatterDecompressorActive(CelestialForgingAnvilBlockEntity blockEntity) {
        CelestialRefactorOption option = blockEntity.getActiveMegastructureOption();
        return option != null && ModMegastructures.MATTER_DECOMPRESSOR.getId().equals(option.id());
    }

    /// 以恒星同步旋转（Y 旋转匹配恒星）渲染戴森球圆环，
    /// 而非其他圆环使用的机械 X/Z 旋转。
    private void renderDysonSphereRings(
        boolean renderR4,
        boolean renderR5,
        float centerY,
        float bodyRot,
        StarData star,
        float scale,
        float dynamicRingScale,
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int packedOverlay,
        ModelBlockRenderer modelRenderer
    ) {
        if (!renderR4 && !renderR5) return;
        if (scale < 0.001f) return;

        float rscale = dynamicRingScale;

        poseStack.pushPose();
        poseStack.translate(0.5, centerY, 0.5);
        poseStack.scale(rscale, rscale, rscale);
        /// 应用恒星的轴倾斜和 Y 旋转 —— 与恒星本体渲染相同
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

    private void renderBrownDwarfDysonSphereRings(
        float centerY,
        float bodyRot,
        CelestialBodyData body,
        float scale,
        float redstoneFactor,
        float ringScale,
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int packedOverlay,
        ModelBlockRenderer modelRenderer,
        CelestialForgingAnvilBlockEntity blockEntity
    ) {
        if (scale < 0.001f) return;

        float fullRScale = ringSystemScale(body, false);
        float rscale = 6.0f + (fullRScale - 6.0f) * redstoneFactor;
        poseStack.pushPose();
        poseStack.translate(0.5, centerY, 0.5);
        poseStack.scale(rscale, rscale, rscale);
        poseStack.mulPose(Axis.XP.rotationDegrees(body.axialTilt()));
        poseStack.mulPose(Axis.YP.rotationDegrees(bodyRot * CelestialBodyData.getVisualRotationSpeed(body.rotationSpeed())));
        renderRingCutout(R2_DYSON_SPHERE, poseStack, bufferSource, packedOverlay, modelRenderer);
        poseStack.popPose();

        /// 与大戴森球相同，R3 作为同步旋转的外环。
        poseStack.pushPose();
        poseStack.translate(0.5, centerY, 0.5);
        poseStack.scale(ringScale, ringScale, ringScale);
        poseStack.mulPose(Axis.XP.rotationDegrees(body.axialTilt()));
        poseStack.mulPose(Axis.YP.rotationDegrees(bodyRot * CelestialBodyData.getVisualRotationSpeed(body.rotationSpeed())));
        renderRingCutout(getRing3Model(blockEntity), poseStack, bufferSource, packedOverlay, modelRenderer);
        poseStack.popPose();
    }

    /// 在 R4 位置用两个模型渲染磁星线圈圆环：
    /// - ring_4_coil_ring —— 机械旋转（与原 R4 内环相同的骨骼层级）
    /// - ring_4_coil_fix —— 恒星同步旋转（类似戴森球，相对恒星静止）
    private void renderMagnetarCoilRings(
        float centerY,
        float rot,
        float bodyRot,
        StarData star,
        float scale,
        float dynamicRingScale,
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int packedOverlay,
        ModelBlockRenderer modelRenderer
    ) {
        if (scale < 0.001f) return;

        float rscale = dynamicRingScale;

        /// === Ring 模型（机械旋转，与原 R4 内环相同） ===
        poseStack.pushPose();
        poseStack.translate(0.5, centerY, 0.5);
        poseStack.scale(rscale, rscale, rscale);
        /// 应用完整骨骼层级以定位到 "in" 骨骼层级
        poseStack.mulPose(Axis.YP.rotationDegrees(-rot));
        poseStack.mulPose(Axis.XP.rotationDegrees(14.5108f));
        poseStack.mulPose(Axis.YP.rotationDegrees(-3.8411f));
        poseStack.mulPose(Axis.ZP.rotationDegrees(14.5109f));
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0f + rot));
        poseStack.mulPose(Axis.ZP.rotationDegrees(rot));
        renderRingCutout(R4_COIL_RING, poseStack, bufferSource, packedOverlay, modelRenderer);
        poseStack.popPose();

        /// === Fix 模型（完全静态，无旋转） ===
        poseStack.pushPose();
        poseStack.translate(0.5, centerY, 0.5);
        poseStack.scale(rscale, rscale, rscale);
        renderRingCutout(R4_COIL_FIX, poseStack, bufferSource, packedOverlay, modelRenderer);
        poseStack.popPose();
    }

    /// 在 R4 位置用两个模型渲染彭罗斯球圆环：
    /// - ring_4_penrose_sphere_laser / ring_4_penrose_sphere_laser_off —— 机械旋转（与原 R4 内环相同的骨骼层级）
    /// - ring_4_penrose_sphere_fix —— 恒星同步旋转（类似戴森球，相对黑洞静止）
    private void renderPenroseSphereRings(
        float centerY,
        float rot,
        float bodyRot,
        StarData star,
        float scale,
        boolean laserActive,
        float dynamicRingScale,
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int packedOverlay,
        ModelBlockRenderer modelRenderer
    ) {
        if (scale < 0.001f) return;

        float rscale = dynamicRingScale;

        /// === Laser/Off 模型（恒星同步，反向 Y 旋转） ===
        poseStack.pushPose();
        poseStack.translate(0.5, centerY, 0.5);
        poseStack.scale(rscale, rscale, rscale);
        /// 与黑洞相同的轴倾斜，但反方向旋转
        poseStack.mulPose(Axis.XP.rotationDegrees(star.axialTilt()));
        poseStack.mulPose(Axis.YP.rotationDegrees(-bodyRot * CelestialBodyData.getVisualRotationSpeed(star.rotationSpeed())));
        ModelResourceLocation laserModel = laserActive ? R4_PENROSE_SPHERE_LASER : R4_PENROSE_SPHERE_LASER_OFF;
        renderRingCutout(laserModel, poseStack, bufferSource, packedOverlay, modelRenderer);
        poseStack.popPose();

        /// === Fix 模型（恒星同步旋转，与黑洞同方向） ===
        poseStack.pushPose();
        poseStack.translate(0.5, centerY, 0.5);
        poseStack.scale(rscale, rscale, rscale);
        /// 应用恒星轴倾斜和 Y 旋转（与黑洞同方向）
        poseStack.mulPose(Axis.XP.rotationDegrees(star.axialTilt()));
        poseStack.mulPose(Axis.YP.rotationDegrees(bodyRot * CelestialBodyData.getVisualRotationSpeed(star.rotationSpeed())));
        renderRingCutout(R4_PENROSE_SPHERE_FIX, poseStack, bufferSource, packedOverlay, modelRenderer);
        poseStack.popPose();
    }

    /// 在 R4 位置用两个模型渲染物质解压器圆环：
    /// - ring_4_matter_decompressor_ring —— 机械旋转（与原 R4 内环相同的骨骼层级）
    /// - ring_4_matter_decompressor_fix —— 恒星同步旋转（类似戴森球，相对恒星残骸静止）
    private void renderMatterDecompressorRings(
        float centerY,
        float rot,
        float bodyRot,
        StarData star,
        float scale,
        float dynamicRingScale,
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int packedOverlay,
        ModelBlockRenderer modelRenderer
    ) {
        if (scale < 0.001f) return;

        float rscale = dynamicRingScale;

        /// === Ring 模型（机械旋转，与原 R4 内环相同） ===
        poseStack.pushPose();
        poseStack.translate(0.5, centerY, 0.5);
        poseStack.scale(rscale, rscale, rscale);
        /// 应用完整骨骼层级以定位到 "in" 骨骼层级
        poseStack.mulPose(Axis.YP.rotationDegrees(-rot));
        poseStack.mulPose(Axis.XP.rotationDegrees(14.5108f));
        poseStack.mulPose(Axis.YP.rotationDegrees(-3.8411f));
        poseStack.mulPose(Axis.ZP.rotationDegrees(14.5109f));
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0f + rot));
        poseStack.mulPose(Axis.ZP.rotationDegrees(rot));
        renderRingCutout(R4_MATTER_DECOMPRESSOR_RING, poseStack, bufferSource, packedOverlay, modelRenderer);
        poseStack.popPose();

        /// === Fix 模型（恒星同步旋转，与恒星残骸同方向） ===
        poseStack.pushPose();
        poseStack.translate(0.5, centerY, 0.5);
        poseStack.scale(rscale, rscale, rscale);
        /// 应用恒星轴倾斜和 Y 旋转（与天体同方向）
        poseStack.mulPose(Axis.XP.rotationDegrees(star.axialTilt()));
        poseStack.mulPose(Axis.YP.rotationDegrees(bodyRot * CelestialBodyData.getVisualRotationSpeed(star.rotationSpeed())));
        renderRingCutout(R4_MATTER_DECOMPRESSOR_FIX, poseStack, bufferSource, packedOverlay, modelRenderer);
        poseStack.popPose();
    }

    /// 判断机械圆环对给定天体数据是否应可见。
    private static boolean isRingVisible(int ring, @Nullable CelestialBodyData bodyData, boolean isAmplify) {
        if (isPlayerHead(bodyData)) return false;
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

    private static boolean isPlayerHead(@Nullable CelestialBodyData bodyData) {
        return bodyData instanceof SpecialCelestialBodyData special && special.isPlayerHead();
    }

    /// 渲染机械圆环，当前后天体切换可见性变化时带有淡入淡出过渡。
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
        if (isPlayerHead(currBody) || (currBody == null && isPlayerHead(prevBody))) return;
        /// 第 1-3 环：非增幅模式，第 4-6 环：增幅模式
        boolean isAmplify = ringIndex >= 4;
        boolean visibleNow = isRingVisible(ringIndex, currBody, isAmplify);
        /// prevBody 为 null 时：CFA 圆环在天体出现前就已存在 —— 所有环均先前可见。
        /// 不再适用的环淡出；保持不变的环继续可见。
        boolean wasVisible = prevBody == null || isRingVisible(ringIndex, prevBody, isAmplify);

        if (!isAnimating) {
            /// 无动画 —— 可见时直接渲染
            if (visibleNow) {
                renderRingCutout(modelId, poseStack, bufferSource, packedOverlay, modelRenderer);
            }
            return;
        }

        if (visibleNow && wasVisible) {
            /// 始终可见 —— 正常以 cutout 渲染
            renderRingCutout(modelId, poseStack, bufferSource, packedOverlay, modelRenderer);
        } else if (visibleNow) {
            /// 隐藏 → 可见：从 0.0 缩放到 1.0
            float scale = animForward ? animProgress : (1.0f - animProgress);
            if (scale > 0.01f) {
                renderRingScaled(modelId, scale, poseStack, bufferSource, packedOverlay, modelRenderer);
            }
        } else if (wasVisible) {
            /// 可见 → 隐藏：从 1.0 缩放到 0.0
            float scale = animForward ? (1.0f - animProgress) : animProgress;
            if (scale > 0.01f) {
                renderRingScaled(modelId, scale, poseStack, bufferSource, packedOverlay, modelRenderer);
            }
        }
        /// 否则：!visibleNow && !wasVisible —— 始终隐藏，不执行任何操作
    }

    /// 以标准 cutout 渲染类型渲染圆环模型。
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

    /// 以指定的渲染类型渲染烘焙模型。
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

    /// 围绕中心（模型原点，因为圆环居中于 0,0,0）缩放渲染圆环模型。
    /// scale 为 0.0 时不可见，为 1.0 时为完整大小。
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
        float animProgress,
        float bodyScale,
        @Nullable StellarVisualState stellarVisual,
        @Nullable StellarEventProfile eventProfile,
        float eventProgress
    ) {
        poseStack.pushPose();
        poseStack.translate(0.5, centerY, 0.5);
        float baseScale = bodyScale;
        if (bodyData instanceof SpecialCelestialBodyData s) {
            if (s.isPlayerHead()) {
                baseScale *= PLAYER_HEAD_SCALE;
            } else if (s.isErrorPlanet()) {
                baseScale *= 0.25f;
            }
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

        /// 复杂自定义模型（破碎、空心、血肉、智慧、错误）和玩家头颅天体
        if (bodyData instanceof SpecialCelestialBodyData s && s.needsCustomModel()) {
            if (s.isPlayerHead()) {
                renderPlayerHeadBody(s, poseStack, bufferSource, packedOverlay);
            } else {
                renderComplexModelBody(s, poseStack, bufferSource, packedOverlay);
                /// 具有大气层的复杂模型天体的大气渲染（血肉、智慧）
                @Nullable ColorRGBA atmosphereColor = s.atmosphereColor();
                if (atmosphereColor != null) {
                    poseStack.pushPose();
                    poseStack.translate(0.5, 0.5, 0.5);
                    poseStack.scale(1.125f, 1.125f, 1.125f);
                    poseStack.translate(-0.5, -0.5, -0.5);
                    float[] atmosRgb = CelestialBodyRenderer.getAtmosphereColor(atmosphereColor);
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
            }
        } else if (bodyData instanceof StarData star) {
            renderStarModel(
                star,
                bodyRotation,
                poseStack,
                bufferSource,
                packedOverlay,
                seed,
                stellarVisual,
                eventProfile,
                eventProgress
            );
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

    /// 通过方块模型文件渲染复杂模型天体（破碎星球、空心星球等）。
    private void renderComplexModelBody(
        SpecialCelestialBodyData special,
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int packedOverlay
    ) {
        BakedModel model = Minecraft.getInstance()
            .getModelManager()
            .getModel(ModelResourceLocation.standalone(special.getModelLocation()));
        if (model == Minecraft.getInstance().getModelManager().getMissingModel()) return;

        CelestialBodyRenderer.renderCustomModel(poseStack.last(), bufferSource, model, packedOverlay);
    }

    /// 使用玩家皮肤纹理渲染玩家头颅天体。
    private void renderPlayerHeadBody(
        SpecialCelestialBodyData special,
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int packedOverlay
    ) {
        net.minecraft.nbt.CompoundTag profileTag = special.playerHeadProfile();
        if (profileTag == null) return;

        net.minecraft.world.item.component.ResolvableProfile profile =
            net.minecraft.world.item.component.ResolvableProfile.CODEC
                .parse(net.minecraft.nbt.NbtOps.INSTANCE, profileTag)
                .getOrThrow();

        ResourceLocation skinTexture = Minecraft.getInstance()
            .getSkinManager()
            .getInsecureSkin(profile.gameProfile())
            .texture();

        net.minecraft.client.model.SkullModel skullModel = new net.minecraft.client.model.SkullModel(
            Minecraft.getInstance()
                .getEntityModels()
                .bakeLayer(new net.minecraft.client.model.geom.ModelLayerLocation(
                    ResourceLocation.withDefaultNamespace("player_head"), "main"))
        );

        RenderType renderType = RenderType.entityTranslucent(skinTexture);
        VertexConsumer vc = bufferSource.getBuffer(renderType);

        /// 将头颅居中于方块空间：头颅模型高度 0.5 格（从 Y=0 到 Y=0.5），
        /// 模型中心在 Y=0.25，方块中心在 Y=0.5，故 Y 偏移 = 0.5 − 0.25 = 0.25
        poseStack.pushPose();
        poseStack.translate(0.5f, 0.25f, 0.5f);
        poseStack.scale(-1.0f, -1.0f, 1.0f);
        skullModel.setupAnim(0f, 0f, 0f);
        skullModel.renderToBuffer(poseStack, vc, LightTexture.FULL_BRIGHT, packedOverlay);
        poseStack.popPose();
    }

    /// 渲染恒星：动画基础模型 + 颜色叠加 + 光晕。
    /// 方块模型通过 .mcmeta 提供动画，半透明叠加立方体提供由能量砧子
    /// 数量决定的天体专属颜色。中子星和黑洞使用专用模型特殊处理。
    private void renderStarModel(
        StarData star,
        float bodyRotation,
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int packedOverlay,
        long seed,
        @Nullable StellarVisualState stellarVisual,
        @Nullable StellarEventProfile eventProfile,
        float eventProgress
    ) {
        /// 恒星残骸使用专用模型，无颜色叠加和光晕
        if (star.bodyClass() == CelestialBodyClass.BLACK_HOLE) {
            renderBakedModel(getStarModel(star), poseStack, bufferSource, packedOverlay, RenderType.translucent());
            return;
        }
        if (star.bodyClass() == CelestialBodyClass.NEUTRON_STAR) {
            renderBakedModelCutout(getStarModel(star), poseStack, bufferSource, packedOverlay);

            /// 沿磁轴渲染相对论喷流 —— 仅限超快脉冲星。
            /// 旋转速度 5+ = 超快（≥100倍视觉倍率），产生可观测相对论喷流所需的极端磁场。
            if (star.rotationSpeed() >= 5) {
                float visualSpeed = CelestialBodyData.getVisualRotationSpeed(star.rotationSpeed());
                float extraJetRotation = bodyRotation * visualSpeed * 0.5f;
                /// 恒星磁场强度为 4（正常）或 5（磁星）
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

        /// 动画灰度恒星模型（方块图集，支持 .mcmeta）
        BakedModel model = Minecraft.getInstance().getModelManager().getModel(getStarModel(star));
        if (model != Minecraft.getInstance().getModelManager().getMissingModel()) {
            VertexConsumer consumer = bufferSource.getBuffer(RenderType.cutout());
            Minecraft.getInstance()
                .getBlockRenderer()
                .getModelRenderer()
                .renderModel(poseStack.last(), consumer, null, model, 1.0f, 1.0f, 1.0f, LightTexture.FULL_BRIGHT, packedOverlay);
        }

        /// 颜色叠加 —— 演化期间来自温度快照，空闲时回退到旧颜色字段。
        float[] rgb = stellarVisual == null
            ? CelestialBodyTextureBakery.starColor(star)
            : new float[] {stellarVisual.red(), stellarVisual.green(), stellarVisual.blue()};
        float emission = stellarVisual == null
            ? 1.0f
            : Math.clamp(0.85f + 0.15f * (float) Math.log10(1.0f + stellarVisual.luminosity()), 0.85f, 1.25f);
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.scale(1.005f, 1.005f, 1.005f);
        poseStack.translate(-0.5, -0.5, -0.5);
        renderColorOverlay(poseStack, bufferSource, rgb[0], rgb[1], rgb[2], packedOverlay);
        poseStack.popPose();

        /// 恒星光晕：演化中和静态恒星共用同一套层数和透明度衰减，只有颜色来自快照。
        /// 演化快照的 emission 是光度的平方根（巨星能到几十），直接当透明度用会让光晕
        /// 又深又短、和进入演化前完全不连贯，所以先压到 1 附近的窄区间。
        int haloIterations = 10;
        for (int i = 0; i < haloIterations; i++) {
            float progress = (float) i / haloIterations;
            float haloScale = 1.0f + progress * 0.6f;
            float alpha = emission * (1.2f - 1.125f * progress) / haloIterations;
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

        if (eventProfile != null && stellarVisual != null) {
            renderStellarEventLayers(
                poseStack,
                bufferSource,
                packedOverlay,
                seed,
                stellarVisual,
                eventProfile,
                eventProgress
            );
        }
    }

    /** 渲染事件的独立抛射壳和核心光晕，二者不共享缩放状态。 */
    private void renderStellarEventLayers(
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int packedOverlay,
        long seed,
        StellarVisualState visual,
        StellarEventProfile profile,
        float eventProgress
    ) {
        float core = Math.max(0.05f, profile.coreRadius(eventProgress));
        float ejecta = Math.max(0.0f, profile.ejectaRadius(eventProgress));
        float coreRatio = Math.clamp(core, 0.05f, 2.0f);
        int eventRgb = profile.color(eventProgress);
        float er = ((eventRgb >> 16) & 0xFF) / 255.0f;
        float eg = ((eventRgb >> 8) & 0xFF) / 255.0f;
        float eb = (eventRgb & 0xFF) / 255.0f;
        float intensity = Math.max(0.1f, profile.emission(eventProgress));

        /// 核心层使用较紧的立方体壳，避免把核心和抛射物误画成一个整体。
        if (coreRatio < 0.98f) {
            poseStack.pushPose();
            poseStack.translate(0.5, 0.5, 0.5);
            poseStack.scale(coreRatio, coreRatio, coreRatio);
            poseStack.translate(-0.5, -0.5, -0.5);
            renderTranslucentCube(
                poseStack,
                bufferSource,
                Math.min(1.0f, er * 1.2f),
                Math.min(1.0f, eg * 1.2f),
                Math.min(1.0f, eb * 1.2f),
                Math.min(0.85f, 0.20f + intensity * 0.08f),
                LightTexture.FULL_BRIGHT,
                packedOverlay,
                seed ^ 0x51A7L
            );
            poseStack.popPose();
        }

        /// 抛射物独立向外膨胀，最多固定六层以控制顶点数量。
        if (ejecta > 0.01f) {
            int layers = Math.min(6, Math.max(1, profile.shellCount()));
            float base = Math.max(visual.radius(), 0.05f);
            float outer = Math.max(1.0f, ejecta / base);
            for (int i = layers; i >= 1; i--) {
                float layerT = i / (float) layers;
                float shellScale = 1.0f + (outer - 1.0f) * layerT;
                float alpha = Math.min(0.55f, (0.12f + intensity * 0.035f) * (1.0f - layerT * 0.55f));
                poseStack.pushPose();
                poseStack.translate(0.5, 0.5, 0.5);
                poseStack.scale(shellScale, shellScale, shellScale);
                poseStack.translate(-0.5, -0.5, -0.5);
                renderTranslucentCube(
                    poseStack,
                    bufferSource,
                    Math.min(1.0f, er + 0.08f),
                    Math.min(1.0f, eg + 0.08f),
                    Math.min(1.0f, eb + 0.08f),
                    alpha,
                    LightTexture.FULL_BRIGHT,
                    packedOverlay,
                    seed ^ (0x51A7L + i)
                );
                poseStack.popPose();
            }
        }
    }

    private void renderCelestialRing(
        CelestialBodyData bodyData,
        float centerY,
        float bodyRotation,
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int packedOverlay,
        float animProgress,
        float bodyScaleMultiplier
    ) {
        if (bodyData.ringType() == RingType.NONE) return;
        ResourceLocation ringTexture = CelestialBodyTextureBakery.getOrBakeRing(bodyData);
        if (ringTexture == null) return;

        poseStack.pushPose();
        poseStack.translate(0.5, centerY, 0.5);
        float ringMultiplier = switch (bodyData) {
            case RockyPlanetData rp -> 1.35f;
            case GiantPlanetData gp -> 1.3f;
            default -> 1.4f;
        };
        float ringScale = bodyScaleMultiplier * ringMultiplier * animProgress;
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

    /// 四棱锥托举光束：底面在锻星砧上表面下方半格（local Y=1.5，因砧顶非平面），锥尖向上指向天体。
    /// 多层结构：核心窄而亮，外层逐层加宽、变暗、更透明，形成"核心 + 向外渐淡光晕"的炫光感，
    /// 使光柱边界柔和、不明显（类似配置里可开启的输电杆连线炫光）。
    /// 颜色青色偏白，叠加混合且整体强度很低 —— 只能模糊看见。
    private static final float BEAM_BASE_Y = 1.5f;
    private static final float BEAM_INNER_HALF = 0.08f; /// 核心底面半宽（约 0.16 格宽，比原来窄一半）
    /// 炫光层数：每层在核心外扩张、亮度递减，营造柔和发光边缘。
    private static final int BEAM_GLOW_LAYERS = 4;
    private static final float BEAM_GLOW_HALF_STEP = 0.06f; /// 每层相对核心额外加宽的半宽

    private static void renderTractorBeam(
        float beamHeight,
        float animProgress,
        PoseStack poseStack,
        MultiBufferSource bufferSource
    ) {
        if (beamHeight <= 0.01f || animProgress <= 0.01f) return;

        VertexConsumer vc = bufferSource.getBuffer(ModRenderTypes.TRACTOR_BEAM);
        PoseStack.Pose pose = poseStack.last();
        float apexY = BEAM_BASE_Y + beamHeight;

        /// 由外到内绘制：外层最宽最暗（炫光晕），逐层收窄变亮，最后是明亮核心。
        /// 叠加混合下颜色相加，使中心累积最亮、向外快速变淡，边界因此模糊柔和。
        for (int layer = BEAM_GLOW_LAYERS; layer >= 1; layer--) {
            float half = BEAM_INNER_HALF + BEAM_GLOW_HALF_STEP * layer;
            /// 越外层强度越低（二次衰减），边缘几乎不可见。
            float falloff = 1.0f / (layer + 1);
            falloff *= falloff * 2.0f;
            float r = 0.045f * falloff * animProgress;
            float g = 0.10f * falloff * animProgress;
            float b = 0.13f * falloff * animProgress;
            emitBeamPyramid(vc, pose, half, apexY, r, g, b, 0.12f);
        }
        /// 核心：窄而亮（偏白青），叠加在炫光之上形成明亮光束芯。
        emitBeamPyramid(vc, pose, BEAM_INNER_HALF, apexY,
            0.11f * animProgress, 0.20f * animProgress, 0.23f * animProgress, 0.22f);
    }

    /// 发射一个以 (0.5,0.5) 为水平中心的四棱锥：方形底面在 BEAM_BASE_Y、半宽 halfWidth，锥尖在 (0.5, apexY, 0.5)。
    /// 四个侧面四边形（顶部两点合并成尖）；底亮顶渐隐（apexFade 为锥尖亮度比例）。
    private static void emitBeamPyramid(
        VertexConsumer vc,
        PoseStack.Pose pose,
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
        /// 底面四角（顺时针），按四条边各生成一个三角形侧面（用退化四边形表示）。
        float[][] corners = {
            {x0, z0}, {x1, z0}, {x1, z1}, {x0, z1}
        };
        float ar = r * apexFade;
        float ag = g * apexFade;
        float ab = b * apexFade;
        for (int i = 0; i < 4; i++) {
            float[] c0 = corners[i];
            float[] c1 = corners[(i + 1) % 4];
            vc.addVertex(pose, c0[0], BEAM_BASE_Y, c0[1]).setColor(r, g, b, 1.0f);
            vc.addVertex(pose, c1[0], BEAM_BASE_Y, c1[1]).setColor(r, g, b, 1.0f);
            vc.addVertex(pose, cx, apexY, cz).setColor(ar, ag, ab, 1.0f);
        }
    }

    /// 超新星闪光是水平展开的动画 billboard，中心跟随天体中心、大小随红石缩放。
    private static final float SUPERNOVA_MAX_RADIUS = 8.0f;

    private void renderSupernovaFlash(
        CelestialForgingAnvilBlockEntity blockEntity,
        float partialTick,
        PoseStack poseStack,
        MultiBufferSource bufferSource
    ) {
        int ticks = blockEntity.getSupernovaFlashTicks();
        int total = CelestialForgingAnvilBlockEntity.SUPERNOVA_FLASH_TICKS;
        /// 已经过的进度 0→1（含 partialTick 平滑）
        float elapsed = (total - ticks + partialTick);
        float t = Math.clamp(elapsed / total, 0.0f, 1.0f);

        /// 8 帧均匀分布在整个时长上，0→7
        int frame = Math.clamp((int) (t * 8.0f), 0, 7);
        ResourceLocation tex = AnvilCraft.of("textures/particle/supernova_" + frame + ".png");

        /// 快速扩张：ease-out（sqrt）使开头扩张最快，随后趋缓。
        float expand = (float) Math.sqrt(t);
        float scale = blockEntity.getSupernovaScale();
        if (scale <= 0f) scale = 1.0f;
        StellarEventProfile profile = StellarTrackLibrary.eventProfile(blockEntity.getSupernovaProfileId());
        float profileRadius = profile == null ? SUPERNOVA_MAX_RADIUS : Math.max(2.0f, profile.rayLength() * 0.65f);
        float radius = profileRadius * expand * scale;
        if (radius < 0.01f) return;

        /// 末段淡出，避免突兀消失。
        float alpha = t > 0.75f ? (1.0f - (t - 0.75f) / 0.25f) : 1.0f;
        if (alpha <= 0.01f) return;

        /// 中心：水平居中于方块，竖直对齐捕获的天体中心世界 Y。
        double localCenterY = blockEntity.getSupernovaCenterY() - blockEntity.getBlockPos().getY();

        poseStack.pushPose();
        poseStack.translate(0.5, localCenterY, 0.5);

        VertexConsumer vc = bufferSource.getBuffer(ModRenderTypes.SUPERNOVA_FLASH.apply(tex));
        PoseStack.Pose pose = poseStack.last();
        int light = LightTexture.FULL_BRIGHT;

        /// 水平四边形（朝上）。NO_CULL 使上下双面可见。
        /// 颜色取自事件 profile：II-P 偏红橙、剥离型偏蓝白、直接坍缩偏暗，
        /// 几何仍是以前的单个平面粒子。
        int flashColor = profile == null ? 0xFFFFFF : profile.color(t);
        emitFlatQuad(
            vc,
            pose,
            radius,
            ((flashColor >> 16) & 0xFF) / 255.0f,
            ((flashColor >> 8) & 0xFF) / 255.0f,
            (flashColor & 0xFF) / 255.0f,
            alpha,
            light
        );
        poseStack.popPose();

        /// 向外放射的立方体光锥，核心收缩时抛射物仍独立扩张。
        renderSupernovaRays(blockEntity, t, scale, localCenterY, profile, poseStack, bufferSource);
    }

    /// 类似末影龙死亡时的向外发光：从天体中心向随机但固定的方向发射一束束光锥，
    /// 快速向外延伸到约 16 格，速度匹配超新星爆发时长。叠加混合 + 末段淡出。
    private static final int SUPERNOVA_RAY_COUNT = 24;
    private static final float SUPERNOVA_RAY_LENGTH = 12.0f;

    private void renderSupernovaRays(
        CelestialForgingAnvilBlockEntity blockEntity,
        float t,
        float scale,
        double localCenterY,
        @Nullable StellarEventProfile profile,
        PoseStack poseStack,
        MultiBufferSource bufferSource
    ) {
        /// 光束长度快速增长（ease-out），匹配爆发节奏；末段淡出。
        float grow = (float) Math.sqrt(t);
        int rayCount = profile == null ? SUPERNOVA_RAY_COUNT : Math.min(SUPERNOVA_RAY_COUNT, profile.rayCount());
        float configuredLength = profile == null ? SUPERNOVA_RAY_LENGTH : Math.max(1.0f, profile.rayLength());
        float length = configuredLength * grow * scale;
        float intensity = t > 0.6f ? (1.0f - (t - 0.6f) / 0.4f) : 1.0f;
        if (length < 0.01f || intensity <= 0.01f) return;

        VertexConsumer vc = bufferSource.getBuffer(ModRenderTypes.STELLAR_BEAM);
        poseStack.pushPose();
        poseStack.translate(0.5, localCenterY, 0.5);
        PoseStack.Pose pose = poseStack.last();

        /// 以方块位置为种子的固定随机方向，使每次爆发的光束朝向稳定不抖动。
        long eventSeed = blockEntity.getSupernovaEventSeed();
        if (eventSeed == 0L) eventSeed = blockEntity.getBlockPos().asLong() ^ 0x5DEECE66DL;
        RandomSource rand = RandomSource.create(eventSeed);
        float baseWidth = 0.25f * scale;
        float asymmetry = profile == null ? 0.0f : profile.asymmetry();
        int rayColor = profile == null ? 0xB8E8FF : profile.color(t);
        float rayR = ((rayColor >> 16) & 0xFF) / 255.0f;
        float rayG = ((rayColor >> 8) & 0xFF) / 255.0f;
        float rayB = (rayColor & 0xFF) / 255.0f;
        for (int i = 0; i < rayCount; i++) {
            /// 球面均匀方向
            float u = rand.nextFloat() * 2.0f - 1.0f;
            float theta = rand.nextFloat() * (float) (Math.PI * 2.0);
            float s = (float) Math.sqrt(1.0f - u * u);
            float dx = s * (float) Math.cos(theta);
            float dy = u;
            float dz = s * (float) Math.sin(theta);
            /// 每束略微随机的长度与强度
            float len = length * (1.0f - asymmetry * 0.3f + (0.6f + asymmetry * 0.6f) * rand.nextFloat());
            float rayI = intensity * (0.5f + 0.5f * rand.nextFloat());
            emitRay(vc, pose, dx, dy, dz, len, baseWidth,
                rayR * rayI, rayG * rayI, rayB * rayI);
        }
        poseStack.popPose();
    }

    /// 发射一束从原点沿方向 (dx,dy,dz) 延伸的细长四棱锥光束：根部为方形截面、尖端收拢。
    private static void emitRay(
        VertexConsumer vc,
        PoseStack.Pose pose,
        float dx,
        float dy,
        float dz,
        float length,
        float halfWidth,
        float r,
        float g,
        float b
    ) {
        /// 构造与方向垂直的两个基向量
        org.joml.Vector3f dir = new org.joml.Vector3f(dx, dy, dz).normalize();
        org.joml.Vector3f up = Math.abs(dir.y) > 0.99f
            ? new org.joml.Vector3f(1f, 0f, 0f)
            : new org.joml.Vector3f(0f, 1f, 0f);
        org.joml.Vector3f n1 = new org.joml.Vector3f(dir).cross(up).normalize().mul(halfWidth);
        org.joml.Vector3f n2 = new org.joml.Vector3f(dir).cross(n1).normalize().mul(halfWidth);
        org.joml.Vector3f tip = new org.joml.Vector3f(dir).mul(length);

        float[][] base = {
            {-n1.x - n2.x, -n1.y - n2.y, -n1.z - n2.z},
            {n1.x - n2.x, n1.y - n2.y, n1.z - n2.z},
            {n1.x + n2.x, n1.y + n2.y, n1.z + n2.z},
            {-n1.x + n2.x, -n1.y + n2.y, -n1.z + n2.z}
        };
        for (int i = 0; i < 4; i++) {
            float[] c0 = base[i];
            float[] c1 = base[(i + 1) % 4];
            vc.addVertex(pose, c0[0], c0[1], c0[2]).setColor(r, g, b, 1.0f);
            vc.addVertex(pose, c1[0], c1[1], c1[2]).setColor(r, g, b, 1.0f);
            vc.addVertex(pose, tip.x, tip.y, tip.z).setColor(0f, 0f, 0f, 1.0f);
            vc.addVertex(pose, tip.x, tip.y, tip.z).setColor(0f, 0f, 0f, 1.0f);
        }
    }

    /// 发射一个以原点为中心、铺于 XZ 平面、边长 2r 的水平四边形（法线朝上）。
    private static void emitFlatQuad(
        VertexConsumer vc,
        PoseStack.Pose pose,
        float r,
        float red,
        float green,
        float blue,
        float alpha,
        int light
    ) {
        int overlay = net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;
        vc.addVertex(pose, -r, 0f, -r).setColor(red, green, blue, alpha).setUv(0f, 0f)
            .setOverlay(overlay).setLight(light).setNormal(pose, 0f, 1f, 0f);
        vc.addVertex(pose, -r, 0f, r).setColor(red, green, blue, alpha).setUv(0f, 1f)
            .setOverlay(overlay).setLight(light).setNormal(pose, 0f, 1f, 0f);
        vc.addVertex(pose, r, 0f, r).setColor(red, green, blue, alpha).setUv(1f, 1f)
            .setOverlay(overlay).setLight(light).setNormal(pose, 0f, 1f, 0f);
        vc.addVertex(pose, r, 0f, -r).setColor(red, green, blue, alpha).setUv(1f, 0f)
            .setOverlay(overlay).setLight(light).setNormal(pose, 0f, 1f, 0f);
    }

    private void renderPlanetBody(
        CelestialBodyData bodyData,
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int packedOverlay,
        long seed
    ) {
        if (bodyData instanceof SpecialCelestialBodyData gateway && gateway.usesEndGatewayModel()) {
            CelestialBodyRenderer.renderEndGatewayBody(poseStack, bufferSource);
        } else {
            ResourceLocation bodyTexture = CelestialBodyTextureBakery.getOrBakeBody(bodyData);
            if (bodyTexture != null) {
                VertexConsumer bodyConsumer = bufferSource.getBuffer(ModRenderTypes.STAR_CUTOUT.apply(bodyTexture));
                CelestialBodyRenderer.renderPlanetBody(poseStack, bodyConsumer, LightTexture.FULL_BRIGHT, packedOverlay);
            }
        }

        /// 大气层 —— 针对具有大气层的岩石行星和特殊天体
        @Nullable float[] atmosRgb = null;
        if (bodyData instanceof RockyPlanetData rp) {
            if (rp.hasAtmosphere()) {
                atmosRgb = getAtmosphereColor(rp.temperature());
            }
        } else if (bodyData instanceof SpecialCelestialBodyData s) {
            @Nullable ColorRGBA atmosphereColor = s.atmosphereColor();
            if (atmosphereColor != null) {
                atmosRgb = CelestialBodyRenderer.getAtmosphereColor(atmosphereColor);
            }
        }
        if (atmosRgb != null) {
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

        /// 褐矮星：微弱的类恒星光晕
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

    /// 以乘法混合（DST_COLOR * SRC_COLOR）渲染立方体，
    /// 用于恒星颜色叠加以实现精确调色板着色。
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

    /// 类似 renderTranslucentCube，但每面的透明度根据方向变化：
    /// 背向光源的面散射更多，与相机视线相切的面（行星边缘）发光最亮
    /// —— 在暗半球周围营造出大气边缘效果。
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

        /// 在眼空间中计算从天体中心指向相机的视线方向
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
        CelestialBodyData body = blockEntity.getCelestialBodyData();
        float centerY = dynamicCenterY(body, blockEntity.isAmplify());
        float fullBodyScale = body != null ? bodyScale(body) * CelestialBodyData.BODY_SCALE_FACTOR : 6.0f;
        StellarVisualState visual = body instanceof StarData ? blockEntity.getStellarVisualState(1.0f) : null;
        if (visual != null && body instanceof StarData && blockEntity.getEvolutionTrack() != null
            && blockEntity.getStellarEvolutionState() != null) {
            float rawScale = blockEntity.getStellarVisualBodyScale(1.0f);
            fullBodyScale = rawScale * CelestialBodyData.BODY_SCALE_FACTOR;
            centerY = stellarCenterY(stellarRingSystemScale(rawScale), blockEntity.isAmplify());
            if (visual.ejectaRadius() > visual.radius()) {
                fullBodyScale = Math.max(fullBodyScale,
                    fullBodyScale * visual.ejectaRadius() / Math.max(visual.radius(), 0.01f));
            }
        }
        float bs = fullBodyScale;
        if (visual != null) {
            float pulseReach = 1.0f + visual.pulsationAmplitude();
            float haloReach = 1.18f + Math.min(0.35f, visual.envelopeOpacity() * 0.25f);
            bs *= Math.max(pulseReach, haloReach);
            StellarEventProfile eventProfile = blockEntity.getStellarEventProfile();
            if (eventProfile != null) {
                float eventReach = Math.max(eventProfile.maxCoreRadius(), eventProfile.maxEjectaRadius());
                bs = Math.max(bs, fullBodyScale * Math.max(1.0f, eventReach));
            }
        }
        if (body instanceof SpecialCelestialBodyData special && special.isPlayerHead()) {
            bs *= PLAYER_HEAD_SCALE;
            centerY = playerHeadCenterY(special, blockEntity.isAmplify(), fullBodyScale, 1.0f);
        }
        // The mechanical rings can extend beyond the body, especially for small planets.
        // Size the box from the larger of the body and the complete ring system.
        float ringScale = visual != null
            ? stellarRingSystemScale(blockEntity.getStellarVisualBodyScale(1.0f))
            : ringSystemScale(body, blockEntity.isAmplify());
        // 包围盒同时覆盖平滑器尚未追上的上一帧尺寸，避免恒星快速收缩时
        // 环和外包层在过渡窗口被视锥裁掉。
        float maxVisualScale = Math.max(bs, ringScale);
        maxVisualScale = Math.max(maxVisualScale, blockEntity.getSmoothRingScale());
        maxVisualScale = Math.max(maxVisualScale, blockEntity.getSmoothBodyScale());
        float maxHeight = Math.max(
            centerY + maxVisualScale * 1.5f,
            blockEntity.isAmplify() ? 18.0f : 12.0f
        );
        float horizontalInset = maxVisualScale * 1.5f;
        float verticalInset = maxVisualScale * 1.5f;
        /// 超新星爆发：水平闪光约 16 格 + 向四面八方（含下方）射出约 12 格光束，
        /// 用以天体中心为心的对称大包围盒覆盖，避免被裁剪。
        if (blockEntity.getSupernovaFlashTicks() > 0) {
            float explosionScale = Math.max(1.0f, blockEntity.getSupernovaScale());
            StellarEventProfile profile = StellarTrackLibrary.eventProfile(blockEntity.getSupernovaProfileId());
            float profileRayLength = profile == null ? SUPERNOVA_RAY_LENGTH : profile.rayLength();
            float reach = Math.max(SUPERNOVA_MAX_RADIUS, profileRayLength)
                * explosionScale * 1.5f + 2;
            double cy = blockEntity.getSupernovaCenterY();
            return new AABB(
                blockEntity.getBlockPos().getX() + 0.5,
                cy,
                blockEntity.getBlockPos().getZ() + 0.5,
                blockEntity.getBlockPos().getX() + 0.5,
                cy,
                blockEntity.getBlockPos().getZ() + 0.5
            ).inflate(reach);
        }
        if (!blockEntity.isAmplify()) {
            AABB aabb = new AABB(blockEntity.getBlockPos().offset(state.getValue(CelestialForgingAnvilBlock.HALF).getOffset())).inflate(
                Math.max(horizontalInset, 1),
                verticalInset,
                Math.max(horizontalInset, 1)
            );
            return aabb.setMaxY(aabb.maxY + maxHeight);
        }
        AABB aabb = new AABB(blockEntity.getBlockPos().offset(state.getValue(CelestialForgingAnvilBlock.HALF).getOffset())).inflate(
            Math.max(horizontalInset, 3),
            verticalInset,
            Math.max(horizontalInset, 3)
        );
        return aabb.setMaxY(aabb.maxY + maxHeight);
    }
}
