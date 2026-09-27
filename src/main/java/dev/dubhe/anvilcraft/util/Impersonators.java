package dev.dubhe.anvilcraft.util;

import dev.dubhe.anvilcraft.api.entity.IImpersonator;
import dev.dubhe.anvilcraft.util.dummy.DummyArmadillo;
import dev.dubhe.anvilcraft.util.dummy.DummyCat;
import dev.dubhe.anvilcraft.util.dummy.DummyWolf;
import lombok.experimental.UtilityClass;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.armadillo.Armadillo;
import org.jspecify.annotations.Nullable;

import java.util.function.Function;

@UtilityClass
public class Impersonators {
    public static final IImpersonator<Armadillo> ARMADILLO = Impersonators.of(Armadillo.class, DummyArmadillo::fromEntity);
    public static final IImpersonator<Cat> CAT = Impersonators.of(Cat.class, DummyCat::fromEntity);
    public static final IImpersonator<Wolf> WOLF = Impersonators.of(Wolf.class, DummyWolf::fromEntity);

    public static <T extends LivingEntity> IImpersonator<T> of(Class<T> clazz, Function<LivingEntity, T> factory) {
        return new IImpersonator<>() {
            @Override
            public boolean isValidMask(@Nullable Class<?> mask) {
                return mask != null && clazz.isAssignableFrom(mask);
            }

            @Override
            public T impersonate(LivingEntity entity) {
                return factory.apply(entity);
            }
        };
    }
}
