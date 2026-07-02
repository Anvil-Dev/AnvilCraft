package dev.dubhe.anvilcraft.predicate;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.dubhe.anvilcraft.init.entity.ModEntitySubPredicates;
import net.minecraft.advancements.critereon.EntitySubPredicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public record FallingBlockPredicate(BlockStatePredicate block) implements EntitySubPredicate {
    public static final MapCodec<FallingBlockPredicate> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
        BlockStatePredicate.CODEC
            .fieldOf("block")
            .forGetter(FallingBlockPredicate::block)
    ).apply(inst, FallingBlockPredicate::new));

    @Override
    public MapCodec<FallingBlockPredicate> codec() {
        return ModEntitySubPredicates.FALLING_BLOCK.get();
    }

    @Override
    public boolean matches(Entity entity, ServerLevel level, @Nullable Vec3 position) {
        if (!(entity instanceof FallingBlockEntity fallingBlock)) return false;
        return this.block().testWithoutEntity(fallingBlock.getBlockState());
    }
}
