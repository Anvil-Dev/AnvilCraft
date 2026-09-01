package dev.dubhe.anvilcraft.block.entity.celestial;

import net.minecraft.nbt.CompoundTag;

import javax.annotation.Nullable;

public sealed interface CelestialBodyData permits RockyPlanetData, GiantPlanetData, StarData, SpecialCelestialBodyData {

    CelestialBodyType type();

    /// 从图表中匹配到的天体类别。
    CelestialBodyClass bodyClass();

    RingType ringType();

    int size();

    float axialTilt();

    /// 自转速度等级（0-5）。
    /// - 0 = 极慢
    /// - 1 = 慢
    /// - 2 = 中等
    /// - 3 = 快
    /// - 4 = 极快
    /// - 5 = 超快
    int rotationSpeed();

    int magneticFieldStrength();

    /// 将自转速度等级转换为视觉旋转倍率（度/刻）。
    static float getVisualRotationSpeed(int level) {
        return switch (level) {
            case 0 -> 0.1f;
            case 1 -> 0.5f;
            case 2 -> 1.0f;
            case 3 -> 1.5f;
            case 4 -> 3.0f;
            default -> 100.0f; /// 5 = 部分中子星专用
        };
    }

    /// 根据天体数据计算原始视觉缩放（不含 BODY_SCALE_FACTOR 倍率）。
    /// 黑洞中子星使用固定值，普通天体使用分段函数映射 size→scale。
    default float bodyScale() {
        if (this instanceof StarData star) {
            if (star.bodyClass() == CelestialBodyClass.BLACK_HOLE) return 1.5f;
            if (star.bodyClass() == CelestialBodyClass.NEUTRON_STAR) return 0.8f;
        }
        return bodyScaleForSize(size());
    }

    /** 根据服务端 StarData 的离散尺寸计算原始视觉缩放。 */
    static float bodyScaleForSize(int size) {
        int clampedSize = Math.clamp(size, 1, 64);
        if (clampedSize <= 20) {
            return 1.5f * (0.2f + (clampedSize - 1) * 0.8f / 19f);
        } else {
            float t = (clampedSize - 20) / 44f;
            return 1.5f * (1.0f + t * t * 1.63f);
        }
    }

    /// === 束星环 / 天体缩放常量（渲染与引力共用） ===

    /// 天体完整视觉缩放倍率（BODY_SCALE_FACTOR × bodyScale = 完整视觉大小）。
    float BODY_SCALE_FACTOR = 10.0f / 1.5f;
    /// 束星环系统缩放与天体缩放的比值。
    float RING_TO_BODY_RATIO = 1.8f;
    /// R1-R3 非增幅环内半径约为 R4-R6 增幅环的 1/2，额外系数使非增幅环获得与增幅环相同的天体间距。
    float RING_SMALL_INNER_RADIUS_FACTOR = 2.0f;
    /// "in" 骨骼累积倾斜的视觉补偿上限。
    float INNER_BONE_BOOST_MAX = 5.5f;
    /// "in" 骨骼视觉补偿随 bodyScale 的衰减速率。
    float INNER_BONE_BOOST_RATE = 0.8f;
    /// 无天体时的默认环缩放。
    float BASE_RING_SCALE = 6.0f;
    /// 天体中心高度与束星环缩放的比值。
    float RING_CENTER_HEIGHT_RATIO = 0.74f;

    /// 根据天体数据计算束星环系统完整缩放（不含红石插值），与 whether amplifies 有关。
    static float ringSystemScale(@Nullable CelestialBodyData data, boolean isAmplify) {
        if (data == null) return BASE_RING_SCALE;
        float bodyS = data.bodyScale();
        float proportional = bodyS * BODY_SCALE_FACTOR * RING_TO_BODY_RATIO;
        if (data instanceof StarData) {
            float inBoneBoost = Math.max(0.0f, INNER_BONE_BOOST_MAX - bodyS * INNER_BONE_BOOST_RATE);
            return proportional + inBoneBoost;
        } else {
            float inBoneBoost = Math.max(0.0f, INNER_BONE_BOOST_MAX * 1.5f - bodyS * INNER_BONE_BOOST_RATE);
            return proportional * RING_SMALL_INNER_RADIUS_FACTOR + inBoneBoost;
        }
    }

    /** 根据演化中的浮点本体缩放计算恒星环系统缩放。 */
    static float ringSystemScaleForVisualBodyScale(float bodyScale) {
        float safeBodyScale = Float.isFinite(bodyScale) ? Math.max(0.01f, bodyScale) : 0.01f;
        float proportional = safeBodyScale * BODY_SCALE_FACTOR * RING_TO_BODY_RATIO;
        float inBoneBoost = Math.max(0.0f, INNER_BONE_BOOST_MAX - safeBodyScale * INNER_BONE_BOOST_RATE);
        return proportional + inBoneBoost;
    }

    /**
     * 由实际渲染出来的本体缩放（已含红石倍率）计算束星环必须达到的缩放。
     *
     * <p>红石信号较低时环和中心高度被钳在固定基准值上，而演化中的恒星半径可以跨越
     * 数量级；没有这个下限，膨胀期的恒星会长到环外并穿进锻星砧。</p>
     */
    static float ringScaleForRenderedBodyScale(float renderedBodyScale) {
        float safe = Float.isFinite(renderedBodyScale) ? Math.max(0.01f, renderedBodyScale) : 0.01f;
        return safe * RING_TO_BODY_RATIO;
    }

    /** 由束星环缩放计算天体中心高度，环与中心共用同一套比例。 */
    static float centerYForRingScale(float ringScale, boolean isAmplify) {
        float safe = Float.isFinite(ringScale) ? Math.max(0.0f, ringScale) : 0.0f;
        return (isAmplify ? 2.5f : 1.5f) + safe * RING_CENTER_HEIGHT_RATIO;
    }

    /** 根据演化中的浮点本体缩放计算恒星中心高度。 */
    static float centerYForVisualBodyScale(float bodyScale, boolean isAmplify) {
        return centerYForRingScale(ringSystemScaleForVisualBodyScale(bodyScale), isAmplify);
    }

    /// 根据天体数据计算动态天体中心高度（不含红石插值）。
    static float dynamicCenterY(@Nullable CelestialBodyData data, boolean isAmplify) {
        if (data == null) return isAmplify ? 6.5f : 4.5f;
        float ringScale = ringSystemScale(data, isAmplify);
        float height = centerYForRingScale(ringScale, isAmplify);
        if (!(data instanceof StarData)) {
            float bodyS = data.bodyScale();
            float planetMinBS = 0.3f;
            float planetMaxBS = 1.5f;
            float t = Math.min(1.0f, Math.max(0.0f, (bodyS - planetMinBS) / (planetMaxBS - planetMinBS)));
            float planetReduction = isAmplify
                ? 0.5f + t * 1.5f
                : 4.0f + t * 1.5f;
            height -= planetReduction;
        }
        return height;
    }

    CompoundTag toTag();

    static CelestialBodyData fromTag(CompoundTag tag) {
        String typeName = tag.getString("bodyType");
        CelestialBodyType type = CelestialBodyType.fromName(typeName);
        return switch (type) {
            case ROCKY_PLANET -> RockyPlanetData.fromTag(tag);
            case GIANT_PLANET -> GiantPlanetData.fromTag(tag);
            case STAR -> StarData.fromTag(tag);
            case SPECIAL -> SpecialCelestialBodyData.fromTag(tag);
        };
    }

    static CelestialBodyClass readClass(CompoundTag tag, CelestialBodyType bodyType) {
        String className = tag.getString("bodyClass");
        if (!className.isEmpty()) {
            return CelestialBodyClass.valueOf(className);
        }
        return switch (bodyType) {
            case ROCKY_PLANET -> CelestialBodyClass.ROCKY_NO_LIQUID;
            case GIANT_PLANET -> CelestialBodyClass.GAS_GIANT;
            case STAR -> CelestialBodyClass.M_MAIN;
            case SPECIAL -> CelestialBodyClass.LARGE_MOON;
        };
    }
}
