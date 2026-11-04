package dev.dubhe.anvilcraft.recipe.anvil.collision;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** 在爆炸阻力计算时执行配方转换，使原版和优化后的射线计算共用转换与成品保护规则。 */
public final class BlockTransformDamageCalculator extends ExplosionDamageCalculator {
    private final ExplosionDamageCalculator original;
    private final Multimap<Block, BlockTransform> transforms = MultimapBuilder.hashKeys().hashSetValues().build();
    private final Map<BlockTransform, Integer> counts = new HashMap<>();
    private final Set<BlockPos> transformed = new HashSet<>();

    public BlockTransformDamageCalculator(ExplosionDamageCalculator original, Collection<BlockTransform> transforms) {
        this.original = original;
        this.addTransforms(transforms);
    }

    /** 向当前爆炸添加转换规则，保留已经执行的转换数量。 */
    public void addTransforms(Collection<BlockTransform> transforms) {
        for (BlockTransform transform : transforms) {
            transform.inputBlock().getBlocks().unwrap().ifLeft(tag ->
                BuiltInRegistries.BLOCK.getTagOrEmpty(tag).forEach(holder -> this.transforms.put(holder.value(), transform))
            );
            transform.inputBlock().getBlocks().unwrap().ifRight(holders ->
                holders.forEach(holder -> this.transforms.put(holder.value(), transform))
            );
        }
    }

    @Override
    public Optional<Float> getBlockExplosionResistance(
        Explosion explosion, BlockGetter level, BlockPos pos, BlockState state, FluidState fluid
    ) {
        if (level instanceof ServerLevel serverLevel && !this.transformed.contains(pos)) {
            Collection<BlockTransform> candidates = this.transforms.get(state.getBlock());
            if (!candidates.isEmpty()) {
                BlockTransform transform = new ArrayList<>(candidates).get(serverLevel.getRandom().nextInt(candidates.size()));
                if (this.counts.getOrDefault(transform, 0) < transform.maxCount() && transform.progress(serverLevel, pos)) {
                    // 优化后的射线可能复用可变坐标，已转换位置必须独立保存。
                    this.transformed.add(pos.immutable());
                    this.counts.merge(transform, 1, Integer::sum);
                }
            }
        }
        if (this.transformed.contains(pos)) {
            state = level.getBlockState(pos);
            fluid = state.getFluidState();
        }
        return this.original.getBlockExplosionResistance(explosion, level, pos, state, fluid);
    }

    @Override
    public boolean shouldBlockExplode(Explosion explosion, BlockGetter level, BlockPos pos, BlockState state, float power) {
        return !this.transformed.contains(pos) && this.original.shouldBlockExplode(explosion, level, pos, state, power);
    }

    @Override
    public boolean shouldDamageEntity(Explosion explosion, Entity entity) {
        return this.original.shouldDamageEntity(explosion, entity);
    }

    @Override
    public float getKnockbackMultiplier(Entity entity) {
        return this.original.getKnockbackMultiplier(entity);
    }

    @Override
    public float getEntityDamageAmount(Explosion explosion, Entity entity, float exposure) {
        return this.original.getEntityDamageAmount(explosion, entity, exposure);
    }
}
