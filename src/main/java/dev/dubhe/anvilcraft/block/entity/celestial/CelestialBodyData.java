package dev.dubhe.anvilcraft.block.entity.celestial;

import net.minecraft.nbt.CompoundTag;
import org.jspecify.annotations.Nullable;

public sealed interface CelestialBodyData permits RockyPlanetData, GiantPlanetData, StarData, SpecialCelestialBodyData {

    CelestialBodyType type();

    /** 从星图中匹配得到的天体类别。 */
    CelestialBodyClass bodyClass();

    RingType ringType();

    int size();

    float axialTilt();

    /**
     * 自转速度等级（0-5）。
     * <ul>
     *   <li>0 = 极慢</li>
     *   <li>1 = 慢</li>
     *   <li>2 = 中等</li>
     *   <li>3 = 快</li>
     *   <li>4 = 极快</li>
     *   <li>5 = 超高速</li>
     * </ul>
     */
    int rotationSpeed();

    int magneticFieldStrength();

    /** 将自转速度等级转换为视觉旋转速度（度/刻）。 */
    static float getVisualRotationSpeed(int level) {
        return switch (level) {
            case 0 -> 0.1f;
            case 1 -> 0.5f;
            case 2 -> 1.0f;
            case 3 -> 1.5f;
            case 4 -> 3.0f;
            default -> 100.0f; // 5 级及以上为超高速
        };
    }

    /**
     * 计算天体原始视觉缩放，不包含 {@link #BODY_SCALE_FACTOR} 倍率。
     * 黑洞和中子星使用固定值，其余天体按大小进行分段映射。
     */
    default float bodyScale() {
        if (this instanceof StarData star) {
            if (star.bodyClass() == CelestialBodyClass.BLACK_HOLE) return 1.5f;
            if (star.bodyClass() == CelestialBodyClass.NEUTRON_STAR) return 0.8f;
        }
        int size = this.size();
        if (size <= 20) {
            return 1.5f * (0.2f + (size - 1) * 0.8f / 19f);
        } else {
            float t = (size - 20) / 44f;
            return 1.5f * (1.0f + t * t * 1.63f);
        }
    }

    // === 束星环与天体缩放常量，渲染和引力共用 ===

    /** 天体完整视觉缩放倍率。 */
    float BODY_SCALE_FACTOR = 10.0f / 1.5f;
    /** 束星环系统缩放与天体缩放的比值。 */
    float RING_TO_BODY_RATIO = 1.8f;
    /** 非增幅 R1-R3 内半径较小，用此补偿系数保持与 R4-R6 一致的天体间距。 */
    float RING_SMALL_INNER_RADIUS_FACTOR = 2.0f;
    /** 内层骨骼累积倾斜的视觉补偿上限。 */
    float INNER_BONE_BOOST_MAX = 5.5f;
    /** 内层骨骼视觉补偿随天体缩放衰减的速率。 */
    float INNER_BONE_BOOST_RATE = 0.8f;
    /** 没有天体时使用的默认束星环缩放。 */
    float BASE_RING_SCALE = 6.0f;

    /** 计算指定天体的完整束星环系统缩放，不包含红石插值。 */
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

    /** 计算指定天体的动态中心高度，不包含红石插值。 */
    static float dynamicCenterY(@Nullable CelestialBodyData data, boolean isAmplify) {
        if (data == null) return isAmplify ? 6.5f : 4.5f;
        float ringScale = ringSystemScale(data, isAmplify);
        float baseHeight = isAmplify ? 2.5f : 1.5f;
        float height = baseHeight + ringScale * 0.74f;
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
        String typeName = tag.getStringOr("bodyType", "rocky_planet");
        CelestialBodyType type = CelestialBodyType.fromName(typeName);
        return switch (type) {
            case ROCKY_PLANET -> RockyPlanetData.fromTag(tag);
            case GIANT_PLANET -> GiantPlanetData.fromTag(tag);
            case STAR -> StarData.fromTag(tag);
            case SPECIAL -> SpecialCelestialBodyData.fromTag(tag);
        };
    }

    static CelestialBodyClass readClass(CompoundTag tag, CelestialBodyType bodyType) {
        String className = tag.getStringOr("bodyClass", "");
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
