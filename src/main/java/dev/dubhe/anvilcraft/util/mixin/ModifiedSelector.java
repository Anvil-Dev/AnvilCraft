package dev.dubhe.anvilcraft.util.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import org.jspecify.annotations.Nullable;

import java.util.function.BiPredicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public interface ModifiedSelector extends TargetingConditions.Selector {
    static ModifiedSelector toModified(
        TargetingConditions.Selector selector,
        @Nullable Supplier<BiPredicate<LivingEntity, ServerLevel>> extra
    ) {
        if (selector instanceof ModifiedSelector modified) return modified;
        if (extra == null) {
            return selector::test;
        } else {
            return (entity, level) -> selector.test(entity, level) && extra.get().test(entity, level);
        }
    }

    static ModifiedSelector toModified(
        TargetingConditions.Selector selector,
        @Nullable UnaryOperator<TargetingConditions.Selector> extra
    ) {
        if (selector instanceof ModifiedSelector modified) return modified;
        if (extra == null) {
            return selector::test;
        } else {
            return extra.apply(selector)::test;
        }
    }
}
