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

        /// 红石信号控制缩放倍率：0 级时使用固定原始值（天体=最小，环=6，高度=4.5/6.5），
        /// 15 级时达到完整动态缩放。中间级别线性插值。
        float redstoneFactor = blockEntity.getRedstoneSignal() / 15.0f;

        float fullRingScale = ringSystemScale(bodyData, isAmplify);
        float fullCenterY = dynamicCenterY(bodyData, isAmplify);
        float baseRingScale = 6.0f;
        float baseCenterY = isAmplify ? 6.5f : 4.5f;
        float ringScale = baseRingScale + (fullRingScale - baseRingScale) * redstoneFactor;
        float centerY = baseCenterY + (fullCenterY - baseCenterY) * redstoneFactor;

        /// 天体缩放跟随红石信号：0 级为原始比例（bodyScale，最小~0.3 格 / 最大~3.9 格），
        /// 15 级为完整放大（getBodyScale = bodyScale × CelestialBodyData.BODY_SCALE_FACTOR）。
        /// 各尺寸天体的比例关系在所有红石级别下保持不变。
        float bodyScaleMultiplier = 2.0f; // 无天体时的默认值
        if (bodyData != null) {
            float rawBodyScale = bodyScale(bodyData);           // 信号 0 时的缩放
            float fullBodyScale = getBodyScale(bodyData);       // 信号 15 时的缩放
            bodyScaleMultiplier = rawBodyScale + (fullBodyScale - rawBodyScale) * redstoneFactor;
        }

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
        poseStack.mulPose(Axis.YP.rotationDegrees(-rot));        /// outout：Y 轴旋转
        poseStack.mulPose(Axis.XP.rotationDegrees(14.5108f));    /// out：静态倾斜 X
        poseStack.mulPose(Axis.YP.rotationDegrees(-3.8411f));    /// out：静态倾斜 Y
        poseStack.mulPose(Axis.ZP.rotationDegrees(14.5109f));    /// out：静态倾斜 Z

        /// === 最外层圆环 —— "out" 的子骨骼 ===
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

        /// mid：X 旋转（中骨骼）
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0f + rot));

        /// === 中间圆环 —— "mid" 的子骨骼 ===
        if (blockEntity.isAmplify()) {
            boolean anyDysonSphere = isDysonSphereActive(blockEntity, 4) || isDysonSphereActive(blockEntity, 5);
            /// 当彭罗斯球激活且第五环为最外层（小恒星）时隐藏第五环
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

        /// in：Z 旋转（内骨骼）
        poseStack.mulPose(Axis.ZP.rotationDegrees(rot));

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
                float rotationBoost = blockEntity.getAnimationRotationBoost(partialTick);
                float bodyRot = (blockEntity.getBodyRotation() + partialTick) * rotationBoost;
                renderDysonSphereRings(
                    isDysonSphereR4,
                    isDysonSphereR5,
                    centerY,
                    bodyRot,
                    star,
                    animProgress,
                    redstoneFactor,
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

        /// 渲染彭罗斯球圆环：fix（恒星同步）+ laser/off（机械旋转）
        if (blockEntity.isAmplify() && isPenroseSphereActive(blockEntity) && bodyData instanceof StarData star) {
            float rotationBoost = blockEntity.getAnimationRotationBoost(partialTick);
            float bodyRot = (blockEntity.getBodyRotation() + partialTick) * rotationBoost;
            renderPenroseSphereRings(centerY, rot, bodyRot, star, animProgress,
                blockEntity.isPenroseSphereLaserActive(), redstoneFactor,
                poseStack, multiBufferSource, packedOverlay, modelRenderer);
        }

        /// 渲染磁星线圈圆环：fix（恒星同步）+ ring（机械旋转）
        if (blockEntity.isAmplify() && isMagnetarCoilActive(blockEntity) && bodyData instanceof StarData star) {
            float rotationBoost = blockEntity.getAnimationRotationBoost(partialTick);
            float bodyRot = (blockEntity.getBodyRotation() + partialTick) * rotationBoost;
            renderMagnetarCoilRings(
                centerY,
                rot,
                bodyRot,
                star,
                animProgress,
                redstoneFactor,
                poseStack,
                multiBufferSource,
                packedOverlay,
                modelRenderer
            );
        }

        /// 渲染物质解压器圆环：fix（恒星同步，类似戴森球）+ ring（机械旋转）
        if (blockEntity.isAmplify() && isMatterDecompressorActive(blockEntity) && bodyData instanceof StarData star) {
            float rotationBoost = blockEntity.getAnimationRotationBoost(partialTick);
            float bodyRot = (blockEntity.getBodyRotation() + partialTick) * rotationBoost;
            renderMatterDecompressorRings(
                centerY,
                rot,
                bodyRot,
                star,
                animProgress,
                redstoneFactor,
                poseStack,
                multiBufferSource,
                packedOverlay,
                modelRenderer
            );
        }

        /// 使用有效天体数据（考虑 celestialBodyData 已置为 null 的逆向动画情形）
        CelestialBodyData effectiveBodyData = blockEntity.getEffectiveBodyDataForRendering();
        boolean canRender = effectiveBodyData != null
            && (!(effectiveBodyData instanceof StarData) || blockEntity.isAmplifierPresent());
        if (canRender) {
            float rotationBoost = blockEntity.getAnimationRotationBoost(partialTick);
            float bodyRot = (blockEntity.getBodyRotation() + partialTick) * rotationBoost;
            /// 天体缩放完全由 StarData.size() 驱动，坍缩期间通过 applyCollapseColor() 缩小 —— 无需额外的 pose 缩放。
            renderCelestialBody(
                effectiveBodyData,
                centerY,
                bodyRot,
                poseStack,
                multiBufferSource,
                packedOverlay,
                blockEntity.getBlockPos().asLong(),
                animProgress,
                bodyScaleMultiplier
            );
            renderCelestialRing(
                effectiveBodyData,
                centerY,
                bodyRot,
                poseStack,
                multiBufferSource,
                packedOverlay,
                animProgress,
                bodyScaleMultiplier
            );
        }

    }

    /// 获取第一环对应的模型，考虑抽取器巨构替换。
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

    /// 获取第二环对应的模型，考虑巨行星提取器巨构替换。
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

    /// 获取第四环对应的模型，考虑替换第四环的巨构。
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

    /// 获取第五环对应的模型，考虑替换第五环的巨构。
    private ModelResourceLocation getRing5Model(CelestialForgingAnvilBlockEntity blockEntity) {
        if (blockEntity.getActiveMegastructureIndex() >= 0) {
            var option = blockEntity.getActiveMegastructureOption();
            if (option != null) {
                if ("dyson_sphere_large".equals(option.megastructure())) {
                    return R5_DYSON_SPHERE;
                }
            }
        }
        /// 恒星演化加速器用于小恒星时替换第五环
        if (blockEntity.isAcceleratorActive() && blockEntity.getCelestialBodyData() instanceof StarData star && star.size() < 48) {
            return R5_STELLAR_EVOLUTION_ACCELERATOR;
        }
        return R5;
    }

    /// 获取第六环对应的模型，考虑替换第六环的巨构。
    private ModelResourceLocation getRing6Model(CelestialForgingAnvilBlockEntity blockEntity) {
        /// 恒星演化加速器用于大恒星时替换第六环
        if (blockEntity.isAcceleratorActive() && blockEntity.getCelestialBodyData() instanceof StarData star && star.size() >= 48) {
            return R6_STELLAR_EVOLUTION_ACCELERATOR;
        }
        return R6;
    }

    /// 检查指定环索引上的戴森球巨构是否激活。
    private static boolean isDysonSphereActive(CelestialForgingAnvilBlockEntity blockEntity, int ring) {
        if (blockEntity.getActiveMegastructureIndex() < 0) return false;
        var option = blockEntity.getActiveMegastructureOption();
        if (option == null) return false;
        if (ring == 4) return "dyson_sphere_small".equals(option.megastructure());
        if (ring == 5) return "dyson_sphere_large".equals(option.megastructure());
        return false;
    }

    /// 检查磁星线圈巨构是否激活。
    private static boolean isMagnetarCoilActive(CelestialForgingAnvilBlockEntity blockEntity) {
        if (blockEntity.getActiveMegastructureIndex() < 0) return false;
        var option = blockEntity.getActiveMegastructureOption();
        if (option == null) return false;
        return "magnetar_coil".equals(option.megastructure());
    }

    /// 检查彭罗斯球巨构是否激活。
    private static boolean isPenroseSphereActive(CelestialForgingAnvilBlockEntity blockEntity) {
        if (blockEntity.getActiveMegastructureIndex() < 0) return false;
        var option = blockEntity.getActiveMegastructureOption();
        if (option == null) return false;
        return "penrose_sphere".equals(option.megastructure());
    }

    /// 检查物质解压器巨构是否激活。
    private static boolean isMatterDecompressorActive(CelestialForgingAnvilBlockEntity blockEntity) {
        if (blockEntity.getActiveMegastructureIndex() < 0) return false;
        var option = blockEntity.getActiveMegastructureOption();
        if (option == null) return false;
        return "matter_decompressor".equals(option.megastructure());
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
        float redstoneFactor,
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int packedOverlay,
        ModelBlockRenderer modelRenderer
    ) {
        if (!renderR4 && !renderR5) return;
        if (scale < 0.001f) return;

        float fullRScale = ringSystemScale(star, true);
        float rscale = 6.0f + (fullRScale - 6.0f) * redstoneFactor;

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

    /// 在 R4 位置用两个模型渲染磁星线圈圆环：
    /// - ring_4_coil_ring —— 机械旋转（与原 R4 内环相同的骨骼层级）
    /// - ring_4_coil_fix —— 恒星同步旋转（类似戴森球，相对恒星静止）
    private void renderMagnetarCoilRings(
        float centerY,
        float rot,
        float bodyRot,
        StarData star,
        float scale,
        float redstoneFactor,
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int packedOverlay,
        ModelBlockRenderer modelRenderer
    ) {
        if (scale < 0.001f) return;

        float fullRScale = ringSystemScale(star, true);
        float rscale = 6.0f + (fullRScale - 6.0f) * redstoneFactor;

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
        float redstoneFactor,
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int packedOverlay,
        ModelBlockRenderer modelRenderer
    ) {
        if (scale < 0.001f) return;

        float fullRScale = ringSystemScale(star, true);
        float rscale = 6.0f + (fullRScale - 6.0f) * redstoneFactor;

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
        float redstoneFactor,
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int packedOverlay,
        ModelBlockRenderer modelRenderer
    ) {
        if (scale < 0.001f) return;

        float fullRScale = ringSystemScale(star, true);
        float rscale = 6.0f + (fullRScale - 6.0f) * redstoneFactor;

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
        float bodyScale
    ) {
        poseStack.pushPose();
        poseStack.translate(0.5, centerY, 0.5);
        float baseScale = bodyScale;
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

        /// 复杂自定义模型（破碎、空心、血肉、智慧、错误）和玩家头颅天体
        if (bodyData instanceof SpecialCelestialBodyData s && s.needsCustomModel()) {
            if (s.isPlayerHead()) {
                renderPlayerHeadBody(s, poseStack, bufferSource, packedOverlay);
            } else {
                renderComplexModelBody(s, poseStack, bufferSource, packedOverlay);
                /// 具有大气层的复杂模型天体的大气渲染（血肉、智慧）
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

    /// 通过方块模型文件渲染复杂模型天体（破碎星球、空心星球等）。
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
        long seed
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

        /// 颜色叠加 —— 乘法混合以实现精确调色板着色
        float[] rgb = CelestialBodyTextureBakery.starColor(star);
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.scale(1.005f, 1.005f, 1.005f);
        poseStack.translate(-0.5, -0.5, -0.5);
        renderColorOverlay(poseStack, bufferSource, rgb[0], rgb[1], rgb[2], packedOverlay);
        poseStack.popPose();

        /// 恒星光晕
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

        /// 大气层 —— 针对具有大气层的岩石行星和特殊天体
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

    private static float getBodyScale(CelestialBodyData data) {
        return data.bodyScale() * CelestialBodyData.BODY_SCALE_FACTOR;
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
        float bs = body != null ? bodyScale(body) * CelestialBodyData.BODY_SCALE_FACTOR : 6.0f;
        float maxHeight = Math.max(centerY + bs * 1.5f, blockEntity.isAmplify() ? 18.0f : 12.0f);
        float horizInset = bs * 0.8f;
        if (!blockEntity.isAmplify()) {
            AABB aabb = new AABB(blockEntity.getBlockPos().offset(state.getValue(CelestialForgingAnvilBlock.HALF).getOffset())).inflate(
                Math.max(horizInset, 1),
                0,
                Math.max(horizInset, 1)
            );
            return aabb.setMaxY(aabb.maxY + maxHeight);
        }
        AABB aabb = new AABB(blockEntity.getBlockPos().offset(state.getValue(CelestialForgingAnvilBlock.HALF).getOffset())).inflate(
            Math.max(horizInset, 3),
            0,
            Math.max(horizInset, 3)
        );
        return aabb.setMaxY(aabb.maxY + maxHeight);
    }
}
