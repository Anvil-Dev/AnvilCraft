package dev.dubhe.anvilcraft.api.laser;

import dev.dubhe.anvilcraft.util.BlockMiningEffect;

import java.util.List;
import java.util.function.Function;

public final class LaserComponentTypes {
    public static final ILaserComponentType<LaserStrengthComponent, Integer> STRENGTH = new Type<>(
        LaserStrengthComponent::new,
        instances -> new LaserStrengthComponent((int) Math.min(Integer.MAX_VALUE,
            instances.stream().mapToLong(LaserStrengthComponent::strength).sum())),
        1000
    );
    public static final ILaserComponentType<LaserTypeComponent, Boolean> LASER_TYPE = new Type<>(
        LaserTypeComponent::new,
        instances -> new LaserTypeComponent(instances.stream().anyMatch(LaserTypeComponent::gamma)),
        900
    );
    public static final ILaserComponentType<LaserMiningComponent, LaserMiningComponent> MINING = new Type<>(
        Function.identity(),
        instances -> {
            BlockMiningEffect effect = instances.getFirst().effect();
            boolean compatible = instances.stream().allMatch(instance -> instance.effect().equals(effect));
            return new LaserMiningComponent(
                compatible ? effect : BlockMiningEffect.NORMAL,
                compatible && instances.stream().anyMatch(LaserMiningComponent::specialTargets)
            );
        },
        800
    );
    public static final ILaserComponentType<GammaLaserBehavior, Boolean> GAMMA_BEHAVIOR = new Type<>(
        ignored -> new GammaLaserBehavior(), ignored -> new GammaLaserBehavior(), 200
    );
    public static final ILaserComponentType<LaserDamageBehavior, Boolean> DAMAGE_BEHAVIOR = new Type<>(
        ignored -> new LaserDamageBehavior(), ignored -> new LaserDamageBehavior(), 400
    );
    public static final ILaserComponentType<LaserHitBehavior, Boolean> HIT_BEHAVIOR = new Type<>(
        ignored -> new LaserHitBehavior(), ignored -> new LaserHitBehavior(), 300
    );

    private LaserComponentTypes() {
    }

    private record Type<T extends ILaserComponent, E>(
        Function<E, T> factory, Function<List<T>, T> merger, int priority
    ) implements ILaserComponentType<T, E> {
        @Override
        public T createInstance(E componentEnvironment) {
            return factory.apply(componentEnvironment);
        }

        @Override
        public T mergeIncoming(List<T> instances) {
            if (instances.isEmpty()) throw new IllegalArgumentException("Cannot merge empty laser components");
            return merger.apply(instances);
        }
    }
}
