package dev.dubhe.anvilcraft.util.mixin;

import net.minecraft.world.entity.LivingEntity;

import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import javax.annotation.Nullable;

public interface ModifiedSelector extends Predicate<LivingEntity> {
    static ModifiedSelector toModified(
        Predicate<LivingEntity> selector,
        @Nullable Supplier<Predicate<LivingEntity>> extra
    ) {
        if (selector instanceof ModifiedSelector modified) return modified;
        if (extra == null) {
            return selector::test;
        } else {
            return entity -> selector.test(entity) && extra.get().test(entity);
        }
    }

    static ModifiedSelector toModified(
        Predicate<LivingEntity> selector,
        @Nullable UnaryOperator<Predicate<LivingEntity>> extra
    ) {
        if (selector instanceof ModifiedSelector modified) return modified;
        if (extra == null) {
            return selector::test;
        } else {
            return extra.apply(selector)::test;
        }
    }
}
