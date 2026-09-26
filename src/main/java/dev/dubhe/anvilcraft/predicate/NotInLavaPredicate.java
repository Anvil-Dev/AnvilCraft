package dev.dubhe.anvilcraft.predicate;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.init.entity.ModEntitySubPredicates;
import net.minecraft.advancements.critereon.EntitySubPredicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/// 生物是否不在岩浆里
public record NotInLavaPredicate() implements EntitySubPredicate {
    public static final MapCodec<NotInLavaPredicate> CODEC = MapCodec.unit(NotInLavaPredicate::new);

    @Override
    public MapCodec<NotInLavaPredicate> codec() {
        return ModEntitySubPredicates.NOT_IN_LAVA.get();
    }

    @Override
    public boolean matches(Entity entity, ServerLevel level, @Nullable Vec3 position) {
        return !entity.isInLava();
    }
}
