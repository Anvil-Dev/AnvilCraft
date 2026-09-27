package dev.dubhe.anvilcraft.predicate;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.init.entity.ModEntitySubPredicates;
import net.minecraft.advancements.critereon.EntitySubPredicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/// 生物是否不在水里
public record NotInWaterPredicate() implements EntitySubPredicate {
    public static final MapCodec<NotInWaterPredicate> CODEC = MapCodec.unit(NotInWaterPredicate::new);

    @Override
    public MapCodec<NotInWaterPredicate> codec() {
        return ModEntitySubPredicates.NOT_IN_WATER.get();
    }

    @Override
    public boolean matches(Entity entity, ServerLevel level, @Nullable Vec3 position) {
        return !entity.isInWater();
    }
}
