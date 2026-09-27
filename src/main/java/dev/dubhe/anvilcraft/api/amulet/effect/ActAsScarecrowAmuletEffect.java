package dev.dubhe.anvilcraft.api.amulet.effect;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import dev.dubhe.anvilcraft.api.amulet.ctx.AmuletEffectContext;
import dev.dubhe.anvilcraft.api.entity.IImpersonator;
import dev.dubhe.anvilcraft.init.item.ModAmuletEffectContextKeys;
import dev.dubhe.anvilcraft.mixin.accessor.AvoidEntityGoalAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.item.ItemStack;

public record ActAsScarecrowAmuletEffect<T extends LivingEntity>(IImpersonator<T> impersonator) implements IAmuletEffect {
    private static final Table<Class<? extends Entity>, IImpersonator<?>, Boolean> CACHE = Tables.synchronizedTable(
        HashBasedTable.create()
    );

    @Override
    public void trigger(LivingEntity entity, ItemStack amulet, AmuletEffectContext ctx) {
        Class<? extends LivingEntity> mask = ctx.get(ModAmuletEffectContextKeys.LIVING_ENTITY_CLASS).orElse(null);
        if (mask != null) {
            if (!this.impersonator().isValidMask(mask)) return;

            ctx.set(ModAmuletEffectContextKeys.MASK_VALID, true);
            if (!ctx.getOrDefault(ModAmuletEffectContextKeys.SIMULATE, false)) {
                ctx.set(ModAmuletEffectContextKeys.TO_AVOID_ENTITY, this.impersonator().impersonate(entity));
            }
            return;
        }

        Mob target = ctx.get(ModAmuletEffectContextKeys.TARGETING_MOB).orElse(null);
        if (target == null) return;

        Class<? extends Mob> clazz = target.getClass();
        if (Boolean.TRUE.equals(CACHE.get(clazz, this.impersonator()))) {
            ctx.set(ModAmuletEffectContextKeys.MASK_VALID, true);
            ctx.set(ModAmuletEffectContextKeys.IGNORE_MOB, true);
            return;
        }

        for (WrappedGoal goal : target.goalSelector.getAvailableGoals()) {
            if (!(goal.getGoal() instanceof AvoidEntityGoal<?> avoid)) continue;
            if (!this.impersonator().isValidMask(((AvoidEntityGoalAccessor<?>) avoid).getAvoidClass())) continue;

            ctx.set(ModAmuletEffectContextKeys.MASK_VALID, true);
            ctx.set(ModAmuletEffectContextKeys.IGNORE_MOB, true);
            CACHE.put(clazz, this.impersonator(), true);
            return;
        }
        CACHE.put(clazz, this.impersonator(), false);
    }
}
