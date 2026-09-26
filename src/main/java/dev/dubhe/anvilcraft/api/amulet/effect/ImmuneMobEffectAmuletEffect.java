package dev.dubhe.anvilcraft.api.amulet.effect;

import dev.dubhe.anvilcraft.api.amulet.ctx.AmuletEffectContext;
import dev.dubhe.anvilcraft.init.item.ModAmuletEffectContextKeys;
import net.minecraft.advancements.critereon.EntityFlagsPredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/// 在满足给定谓词时免疫并移除指定药水效果的护符效果
public record ImmuneMobEffectAmuletEffect(
    HolderSet<MobEffect> immune,
    Optional<EntityPredicate> condition
) implements IAmuletEffect {
    /// 佩戴即免疫
    public static ImmuneMobEffectAmuletEffect of(Holder<MobEffect> effect) {
        return new ImmuneMobEffectAmuletEffect(HolderSet.direct(effect), Optional.empty());
    }

    /// 在给定谓词成立时免疫
    public static ImmuneMobEffectAmuletEffect of(Holder<MobEffect> effect, EntityPredicate condition) {
        return new ImmuneMobEffectAmuletEffect(HolderSet.direct(effect), Optional.of(condition));
    }

    /// 佩戴者潜行时免疫
    public static ImmuneMobEffectAmuletEffect whileSneaking(Holder<MobEffect> effect) {
        return ImmuneMobEffectAmuletEffect.of(
            effect,
            EntityPredicate.Builder.entity()
                .flags(EntityFlagsPredicate.Builder.flags().setCrouching(true))
                .build()
        );
    }

    @Override
    public void trigger(LivingEntity entity, ItemStack amulet, AmuletEffectContext ctx) {
        if (!(entity instanceof ServerPlayer serverPlayer) || !this.matches(serverPlayer)) {
            return;
        }
        Optional<MobEffectInstance> instance = ctx.get(ModAmuletEffectContextKeys.MOB_EFFECT);
        if (instance.isPresent()) {
            // 查询上下文里没有 ENABLED，本效果无从得知提供它的护符是否已被包覆者顶替，
            // 只能按自身条件直接写回免疫结果
            if (this.immune.contains(instance.get().getEffect())) {
                ctx.set(ModAmuletEffectContextKeys.IMMUNE_MOB_EFFECT, true);
            }
            return;
        }
        if (!ctx.get(ModAmuletEffectContextKeys.ENABLED).orElse(false)) {
            return;
        }
        for (Holder<MobEffect> effect : this.immune) {
            entity.removeEffect(effect);
        }
    }

    private boolean matches(ServerPlayer player) {
        return this.condition.isEmpty() || this.condition.get().matches(player, player);
    }
}
