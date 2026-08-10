package dev.dubhe.anvilcraft.client.renderer.blockentity.state;

import dev.dubhe.anvilcraft.api.rendering.BlockStateModelTessellateState;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyData;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.component.ResolvableProfile;
import org.jspecify.annotations.Nullable;

/**
 * 锻星砧世界内渲染状态，在提取阶段保存提交阶段所需的全部数据。
 */
@Getter
@Setter
public class CFARenderState extends BlockEntityRenderState {
    // 基础束星环动画。
    private float rotation;
    private boolean amplified;
    private int activeMegastructureRing = -1;
    private float activeMegastructureRotation;
    private int auxiliaryMegastructureRing = -1;
    private float auxiliaryMegastructureRotation;

    // 红石驱动并经过平滑插值的缩放参数。
    private float ringScale;
    private float centerY;
    private float bodyScaleMultiplier;
    private float beamHeight;
    private float redstoneFactor;

    // 机械束星环模型。
    @Nullable
    private BlockStateModelTessellateState innerRingModel;   // R1 (non-amp) / R4 (amp)
    @Nullable
    private BlockStateModelTessellateState middleRingModel;  // R2 (non-amp) / R5 (amp)
    @Nullable
    private BlockStateModelTessellateState outerRingModel;    // R3 (non-amp) / R6 (amp)
    private boolean hasInnerRing;
    private boolean hasMiddleRing;
    private boolean hasOuterRing;

    // 提取阶段计算的束星环可见性，用于淡入淡出。
    private boolean innerVisibleNow;
    private boolean innerWasVisible;
    private boolean middleVisibleNow;
    private boolean middleWasVisible;
    private boolean outerVisibleNow;
    private boolean outerWasVisible;

    // 巨构的恒星同步或机械旋转附加层。
    // 为恒星同步巨构环保存天体数据和旋转角度。
    @Nullable
    private CelestialBodyData bodyData;
    @Nullable
    private CelestialBodyData effectiveBodyData;
    private float bodyRotation;

    private boolean dysonSphereR4;
    private boolean dysonSphereR5;
    private boolean magnetarCoil;
    private boolean penroseSphere;
    private boolean penroseLaserActive;
    private boolean matterDecompressor;
    private boolean acceleratorActive;

    @Nullable
    private BlockStateModelTessellateState r4DysonModel;
    @Nullable
    private BlockStateModelTessellateState r5DysonModel;
    @Nullable
    private BlockStateModelTessellateState coilFixModel;
    @Nullable
    private BlockStateModelTessellateState coilRingModel;
    @Nullable
    private BlockStateModelTessellateState penroseFixModel;
    @Nullable
    private BlockStateModelTessellateState penroseLaserModel;
    @Nullable
    private BlockStateModelTessellateState decompressorFixModel;
    @Nullable
    private BlockStateModelTessellateState decompressorRingModel;
    // 戴森球使用恒星同步方式渲染的外环模型。
    @Nullable
    private BlockStateModelTessellateState dysonOuterRingModel;
    private boolean dysonSmallStar;

    // 动画状态。
    private float animationProgress;
    private boolean animating;
    private boolean animationForward;

    // 天体状态。
    private boolean canRenderBody;
    // 主序星、中子星和黑洞使用的烘焙动画模型。
    @Nullable
    private BlockStateModelTessellateState bodyModel;
    @Nullable
    private BlockStateModelTessellateState neutronJetModel;
    // 色板行星的动态烘焙贴图标识；恒星和复杂模型天体不使用。
    @Nullable
    private Identifier bodyTexture;
    // 粉碎、空洞、血肉、智慧和错误天体的复杂模型。
    @Nullable
    private BlockStateModelTessellateState complexBodyModel;
    @Nullable
    private ResolvableProfile playerHeadProfile;
    // 使用烘焙贴图的天体环。
    @Nullable
    private Identifier bodyRingTexture;

    // 超新星状态。
    private int supernovaFlashTicks;
    private float supernovaProgress;
    private long supernovaSeed;
    private double supernovaLocalCenterY;
    private float supernovaScale;
    @Nullable
    private Identifier supernovaFrameTexture;
    private float supernovaFlashAlpha;
    private float supernovaFlashRadius;
}
