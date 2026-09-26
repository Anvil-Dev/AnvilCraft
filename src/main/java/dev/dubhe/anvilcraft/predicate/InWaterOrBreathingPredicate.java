package dev.dubhe.anvilcraft.predicate;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.init.entity.ModEntitySubPredicates;
import dev.dubhe.anvilcraft.item.EquipmentAbilities;
import net.minecraft.advancements.critereon.EntitySubPredicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/// 生物是否在水里或戴着能呼吸的头盔
public record InWaterOrBreathingPredicate() implements EntitySubPredicate {
    public static final MapCodec<InWaterOrBreathingPredicate> CODEC = MapCodec.unit(InWaterOrBreathingPredicate::new);

    @Override
    public MapCodec<InWaterOrBreathingPredicate> codec() {
        return ModEntitySubPredicates.IN_WATER_OR_BREATHING.get();
    }

    @Override
    public boolean matches(Entity entity, ServerLevel level, @Nullable Vec3 position) {
        return entity.isInWater()
            || (entity instanceof LivingEntity living && EquipmentAbilities.canBreathe(living));
    }
}
