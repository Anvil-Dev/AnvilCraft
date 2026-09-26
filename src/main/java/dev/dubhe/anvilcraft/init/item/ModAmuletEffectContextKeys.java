package dev.dubhe.anvilcraft.init.item;

import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.amulet.ctx.AmuletEffectContextKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

public class ModAmuletEffectContextKeys {
    // 参数：由护符管理器在触发护符效果前提供
    /// 护符是否处于启用状态
    public static final AmuletEffectContextKey<Boolean> ENABLED = AmuletEffectContextKey.ofBool(AnvilCraft.of("enabled"));
    /// 待判定的伤害源
    public static final AmuletEffectContextKey<DamageSource> DAMAGE_SOURCE = AmuletEffectContextKey.of(
        AnvilCraft.of("damage_source"),
        DamageSource.class
    );
    /// 待判定的药水效果
    public static final AmuletEffectContextKey<MobEffectInstance> MOB_EFFECT = AmuletEffectContextKey.of(
        AnvilCraft.of("mob_effect"),
        MobEffectInstance.class
    );
    /// 玩家是否处于进食中
    public static final AmuletEffectContextKey<Boolean> CONSUMING_FOOD = AmuletEffectContextKey.ofBool(AnvilCraft.of("consuming_food"));
    /// 待判定的生物类型
    public static final AmuletEffectContextKey<EntityType<?>> MOB_TYPE = ModAmuletEffectContextKeys.ofEntityType(AnvilCraft.of("mob_type"));
    /// 待判定的交互目标
    public static final AmuletEffectContextKey<Entity> INTERACT_TARGET = AmuletEffectContextKey.of(
        AnvilCraft.of("interact_target"),
        Entity.class
    );

    // 返回值：由护符效果在触发时写入
    /// 是否免疫待判定的伤害源
    public static final AmuletEffectContextKey<Boolean> IMMUNE_DAMAGE = AmuletEffectContextKey.ofBool(AnvilCraft.of("immune_damage"));
    /// 是否免疫待判定的药水效果
    public static final AmuletEffectContextKey<Boolean> IMMUNE_MOB_EFFECT = AmuletEffectContextKey.ofBool(
        AnvilCraft.of("immune_mob_effect")
    );
    /// 村民交易的折扣率，为 0 时表示无折扣
    public static final AmuletEffectContextKey<Float> DISCOUNT_RATE = AmuletEffectContextKey.ofFloat(AnvilCraft.of("discount_rate"));
    /// 待判定的生物是否无视佩戴者
    public static final AmuletEffectContextKey<Boolean> IGNORE_MOB = AmuletEffectContextKey.ofBool(AnvilCraft.of("ignore_mob"));
    /// 是否已处理待判定的交互
    public static final AmuletEffectContextKey<Boolean> HANDLE_INTERACT = AmuletEffectContextKey.ofBool(
        AnvilCraft.of("handle_interact")
    );
    /// 是否免疫击退
    public static final AmuletEffectContextKey<Boolean> IMMUNE_KNOCKBACK = AmuletEffectContextKey.ofBool(
        AnvilCraft.of("immune_knockback")
    );
    /// 是否免疫振动
    public static final AmuletEffectContextKey<Boolean> IMMUNE_VIBRATION = AmuletEffectContextKey.ofBool(
        AnvilCraft.of("immune_vibration")
    );
    /// 是否无视天体引力
    public static final AmuletEffectContextKey<Boolean> IGNORE_GRAVITY = AmuletEffectContextKey.ofBool(AnvilCraft.of("ignore_gravity"));
    /// 是否免疫异常物品带来的负面效果
    public static final AmuletEffectContextKey<Boolean> IMMUNE_ABNORMAL_ITEM = AmuletEffectContextKey.ofBool(
        AnvilCraft.of("immune_abnormal_item")
    );

    private static AmuletEffectContextKey<EntityType<?>> ofEntityType(ResourceLocation id) {
        return AmuletEffectContextKey.of(id, Util.cast(EntityType.class));
    }
}
