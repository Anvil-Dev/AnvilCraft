package dev.dubhe.anvilcraft.block.storage;

import dev.dubhe.anvilcraft.block.heatable.HeatableBlock;
import dev.dubhe.anvilcraft.block.heatable.NormalBlock;
import dev.dubhe.anvilcraft.item.tool.MultitoolItem;
import dev.dubhe.anvilcraft.item.tool.MultitoolMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

public class GunpowderBlock extends Block {
    public GunpowderBlock(Properties properties) {
        super(properties);
    }

    public void explosion(Level level, BlockPos pos) {
        if (level.isClientSide()) {
            return;
        }
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 11);
        level.explode(null,
            null,
            new ExplosionDamageCalculator() {
                @Override
                public Optional<Float> getBlockExplosionResistance(
                    Explosion explosion,
                    BlockGetter reader,
                    BlockPos pos,
                    BlockState state,
                    FluidState fluid
                ) {
                    return Optional.of(Float.MAX_VALUE);
                }

                @Override
                public boolean shouldDamageEntity(Explosion explosion, Entity entity) {
                    return false;
                }
            },
            pos.getX(),
            pos.getY(),
            pos.getZ(),
            4.0F,
            false,
            Level.ExplosionInteraction.BLOCK);
    }

    @Override
    protected InteractionResult useItemOn(
        ItemStack stack,
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        BlockHitResult hitResult
    ) {
        if (
            stack.is(Items.FLINT_AND_STEEL)
            || stack.is(Items.FIRE_CHARGE)
            || MultitoolItem.isActingAs(stack, MultitoolMode.FLINT_AND_STEEL)
        ) {
            this.explosion(level, pos);
            Item item = stack.getItem();
            if (
                stack.is(Items.FLINT_AND_STEEL)
                || MultitoolItem.isActingAs(stack, MultitoolMode.FLINT_AND_STEEL)
            ) {
                stack.hurtAndBreak(1, player, hand);
            } else {
                stack.consume(1, player);
            }
            player.awardStat(Stats.ITEM_USED.get(item));
            return InteractionResult.SUCCESS;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    @Override
    protected BlockState updateShape(
        BlockState state,
        LevelReader level,
        ScheduledTickAccess ticks,
        BlockPos pos,
        Direction direction,
        BlockPos neighborPos,
        BlockState neighborState,
        RandomSource random
    ) {
        if (level instanceof Level actualLevel && !actualLevel.isClientSide()) {
            BlockState block = level.getBlockState(pos.relative(direction));
            if (block.getBlock() instanceof BaseFireBlock
                || block.is(Blocks.LAVA)
                || ((block.getBlock() instanceof HeatableBlock) && !(block.getBlock() instanceof NormalBlock))) {
                this.explosion(actualLevel, pos);
            }
        }
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide()) {
            for (Direction direction : Direction.values()) {
                BlockState block = level.getBlockState(pos.relative(direction));
                if (block.getBlock() instanceof BaseFireBlock
                    || block.is(Blocks.LAVA)
                    || ((block.getBlock() instanceof HeatableBlock) && !(block.getBlock() instanceof NormalBlock))) {
                    this.explosion(level, pos);
                }
            }
        }
    }

    @Override
    public boolean onCaughtFire(
        BlockState state,
        Level level,
        BlockPos pos,
        @Nullable Direction direction,
        @Nullable LivingEntity igniter
    ) {
        this.explosion(level, pos);
        return super.onCaughtFire(state, level, pos, direction, igniter);
    }

    @Override
    protected void onProjectileHit(Level level, BlockState state, BlockHitResult hit, Projectile projectile) {
        if (level.isClientSide()) {
            return;
        }
        BlockPos pos = hit.getBlockPos();
        if (projectile.isOnFire() && projectile.mayInteract((ServerLevel) level, pos)) {
            this.explosion(level, pos);
        }
    }

    @Override
    public void wasExploded(ServerLevel level, BlockPos pos, Explosion explosion) {
        this.explosion(level, pos);
    }

    @Override
    public boolean canDropFromExplosion(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
        return false;
    }
}
