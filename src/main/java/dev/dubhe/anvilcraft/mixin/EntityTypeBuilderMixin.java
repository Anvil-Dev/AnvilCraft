package dev.dubhe.anvilcraft.mixin;

import com.google.common.collect.ImmutableSet;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.entity.ThrownHeavyHalberdEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Predicate;
import java.util.function.ToIntFunction;

@Mixin(EntityType.Builder.class)
public class EntityTypeBuilderMixin<T extends Entity> {
    @WrapOperation(
        method = "build",
        at = @At(
            value = "NEW",
            target = "(Lnet/minecraft/world/entity/EntityType$EntityFactory;"
                     + "Lnet/minecraft/world/entity/MobCategory;"
                     + "ZZZZLcom/google/common/collect/ImmutableSet;"
                     + "Lnet/minecraft/world/entity/EntityDimensions;"
                     + "FIILnet/minecraft/world/flag/FeatureFlagSet;"
                     + "Ljava/util/function/Predicate;"
                     + "Ljava/util/function/ToIntFunction;"
                     + "Ljava/util/function/ToIntFunction;)"
                     + "Lnet/minecraft/world/entity/EntityType;"
        )
    )
    private EntityType<T> buildHeavyHalberd(
        EntityType.EntityFactory<T> factory,
        MobCategory category,
        boolean serialize,
        boolean summon,
        boolean fireImmune,
        boolean canSpawnFarFromPlayer,
        ImmutableSet<Block> immuneTo,
        EntityDimensions dimensions,
        float spawnDistanceWeight,
        int clientTrackingRange,
        int updateInterval,
        FeatureFlagSet requiredFeatures,
        Predicate<EntityType<T>> validRide,
        ToIntFunction<EntityType<T>> interfaceScale,
        ToIntFunction<EntityType<T>> shadowRadius,
        Operation<EntityType<T>> original
    ) {
        if (Util.instanceOfAny(factory, ThrownHeavyHalberdEntity.Factory.class)) {
            ThrownHeavyHalberdEntity.Factory<? extends ThrownHeavyHalberdEntity> factory1 = Util.cast(factory);
            Predicate<EntityType<?>> validRide1 = Util.cast(validRide);
            ToIntFunction<EntityType<?>> interfaceScale1 = Util.cast(interfaceScale);
            ToIntFunction<EntityType<?>> shadowRadius1 = Util.cast(shadowRadius);
            return Util.cast(new ThrownHeavyHalberdEntity.HeavyHalberdType<>(
                factory1,
                category,
                serialize,
                summon,
                fireImmune,
                canSpawnFarFromPlayer,
                immuneTo,
                dimensions,
                spawnDistanceWeight,
                clientTrackingRange,
                updateInterval,
                requiredFeatures,
                validRide1,
                interfaceScale1,
                shadowRadius1
            ));
        }
        return original.call(
            factory,
            category,
            serialize,
            summon,
            fireImmune,
            canSpawnFarFromPlayer,
            immuneTo,
            dimensions,
            spawnDistanceWeight,
            clientTrackingRange,
            updateInterval,
            requiredFeatures,
            validRide,
            interfaceScale,
            shadowRadius
        );
    }
}