package dev.dubhe.anvilcraft.util;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialTravelManager;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.entity.living.LivingBreatheEvent;

import java.util.HashMap;
import java.util.Map;

/** 管理各维度的大气成分、呼吸条件和空气阻力。 */
@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class AtmosphereManager {
    /** 原版空气阻力，未配置大气和阻力的维度默认使用此值。 */
    public static final double DEFAULT_AIR_RESISTANCE = 1.0;

    public static final double MIN_AIR_RESISTANCE = 0.1;

    private static final Map<ResourceKey<Level>, Atmosphere> DIMENSION_ATMOSPHERES = new HashMap<>();
    private static final Map<ResourceKey<Level>, Double> DIMENSION_AIR_RESISTANCE_MAP = new HashMap<>();

    static {
        registerDimensionAtmosphere(CelestialTravelManager.MUN_LEVEL, Atmosphere.VACUUM);
        registerDimensionAtmosphere(CelestialTravelManager.VOID_PLANET_LEVEL, Atmosphere.VACUUM);
        registerDimensionAirResistance(CelestialTravelManager.VOID_PLANET_LEVEL, 0.0);
    }

    private AtmosphereManager() {
    }

    public static void registerDimensionAtmosphere(ResourceKey<Level> dimension, Atmosphere atmosphere) {
        DIMENSION_ATMOSPHERES.put(dimension, atmosphere);
    }

    public static Atmosphere getDimensionAtmosphere(ResourceKey<Level> dimension) {
        return DIMENSION_ATMOSPHERES.getOrDefault(dimension, Atmosphere.OVERWORLD);
    }

    public static Atmosphere getDimensionAtmosphere(Level level) {
        return getDimensionAtmosphere(level.dimension());
    }

    /** 查询指定位置的大气，当前使用维度默认值，后续可在此接入局部大气区域。 */
    public static Atmosphere getAtmosphere(Level level, BlockPos pos) {
        return getDimensionAtmosphere(level);
    }

    /** 单独设置的阻力不受气压推导的最小值限制，例如虚空维度可设为零阻力。 */
    public static void registerDimensionAirResistance(ResourceKey<Level> dimension, double airResistance) {
        if (!Double.isFinite(airResistance)) {
            throw new IllegalArgumentException("Air resistance must be finite");
        }
        DIMENSION_AIR_RESISTANCE_MAP.put(dimension, Math.max(0.0, airResistance));
    }

    public static double getDimensionAirResistance(Level level) {
        return getDimensionAirResistance(level.dimension());
    }

    public static double getDimensionAirResistance(ResourceKey<Level> dimension) {
        Double override = DIMENSION_AIR_RESISTANCE_MAP.get(dimension);
        return override != null ? override : Math.max(MIN_AIR_RESISTANCE, getDimensionAtmosphere(dimension).pressure());
    }

    public static void clearDimensionAirResistance(ResourceKey<Level> dimension) {
        DIMENSION_AIR_RESISTANCE_MAP.remove(dimension);
    }

    /** 水生生物和蝾螈会在通用呼吸逻辑之外处理离水后的气息消耗。 */
    public static boolean requiresBreathing(LivingEntity entity) {
        // 铁傀儡通过重写气息消耗逻辑实现水中生存，没有声明水下呼吸能力。
        if (entity instanceof IronGolem) return false;
        return entity.canDrownInFluidType(NeoForgeMod.WATER_TYPE.value())
            || entity instanceof WaterAnimal || entity instanceof Axolotl;
    }

    public static boolean isSuffocating(LivingEntity entity) {
        if (!entity.isAlive() || entity.isSpectator()
            || entity instanceof Player player && player.isCreative()) {
            return false;
        }
        if (getAtmosphere(entity.level(), BlockPos.containing(entity.getEyePosition())).gasAmount(Atmosphere.OXYGEN) > 0.0
            || !requiresBreathing(entity)) {
            return false;
        }
        return !entity.isEyeInFluid(FluidTags.WATER)
            || entity.canDrownInFluidType(NeoForgeMod.WATER_TYPE.value()) && !MobEffectUtil.hasWaterBreathing(entity);
    }

    @SubscribeEvent
    public static void onLivingBreathe(LivingBreatheEvent event) {
        if (isSuffocating(event.getEntity())) event.setCanBreathe(false);
    }

    /** 根据维度的空气阻力系数调整原版速度衰减倍率。 */
    public static double drag(Level level, double vanillaDrag) {
        double airResistance = getDimensionAirResistance(level);
        if (airResistance == DEFAULT_AIR_RESISTANCE) return vanillaDrag;
        return Math.pow(vanillaDrag, airResistance);
    }

    public static float drag(Level level, float vanillaDrag) {
        return (float) drag(level, (double) vanillaDrag);
    }

    public static double drag(Entity entity, double vanillaDrag) {
        if (entity instanceof Player player && player.isCreative() && player.getAbilities().flying) return vanillaDrag;
        return drag(entity.level(), vanillaDrag);
    }

    public static float drag(Entity entity, float vanillaDrag) {
        if (entity instanceof Player player && player.isCreative() && player.getAbilities().flying) return vanillaDrag;
        return drag(entity.level(), vanillaDrag);
    }

    public static boolean isCreativeFlying(Entity entity) {
        return entity instanceof Player player && player.getAbilities().flying;
    }

    /** 被动升力最多抵消重力，不能将重力转化为向上的推力。 */
    public static double elytraLift(Entity entity, double vanillaLift) {
        if (isCreativeFlying(entity)) return vanillaLift;
        return Math.clamp(vanillaLift * getDimensionAirResistance(entity.level()), 0.0, 1.0);
    }

    /** 稀薄大气会降低鞘翅将下落或前进速度转化为升力的效率。 */
    public static double elytraResponse(Entity entity, double vanillaResponse) {
        if (isCreativeFlying(entity)) return vanillaResponse;
        return vanillaResponse * Math.min(1.0, getDimensionAirResistance(entity.level()));
    }
}
