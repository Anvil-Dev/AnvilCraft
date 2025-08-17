package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.block.heatable.HeatableBlock;
import dev.dubhe.anvilcraft.block.heatable.NormalBlock;
import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.item.MultitoolItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
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
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class GunpowderBlock extends Block {
    public GunpowderBlock(Properties properties) {
        super(properties);
    }

    public void explosion(Level level, BlockPos pos) {
        if (level.isClientSide) {
            return;
        }
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 11);
        level.explode(null,
            null,
            new ExplosionDamageCalculator() {
                @Override
                public Optional<Float> getBlockExplosionResistance(Explosion explosion, BlockGetter reader, BlockPos pos, BlockState state, FluidState fluid) {
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
            4.0f,
            false,
            Level.ExplosionInteraction.BLOCK);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (stack.is(Items.FLINT_AND_STEEL)
            || stack.is(Items.FIRE_CHARGE)
            || (stack.is(ModItems.MULTITOOL_ITEM)
            && MultitoolItem.getMode(stack) == MultitoolItem.FLINT_AND_STEEL_MODE)) {
            explosion(level, pos);
            Item item = stack.getItem();
            if (stack.is(Items.FLINT_AND_STEEL)
                || (stack.is(ModItems.MULTITOOL_ITEM)
                && MultitoolItem.getMode(stack) == MultitoolItem.FLINT_AND_STEEL_MODE)) {
                stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
            } else {
                stack.consume(1, player);
            }
            player.awardStat(Stats.ITEM_USED.get(item));
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (!level.isClientSide()) {
            BlockState block = level.getBlockState(pos.relative(direction));
            if (block.getBlock() instanceof BaseFireBlock
                || block.is(Blocks.LAVA)
                || ((block.getBlock() instanceof HeatableBlock) && !(block.getBlock() instanceof NormalBlock))) {
                explosion((Level) level, pos);
            }
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide()) {
            for (Direction direction : Direction.values()) {
                BlockState block = level.getBlockState(pos.relative(direction));
                if (block.getBlock() instanceof BaseFireBlock
                    || block.is(Blocks.LAVA)
                    || ((block.getBlock() instanceof HeatableBlock) && !(block.getBlock() instanceof NormalBlock))) {
                    explosion(level, pos);
                }
            }
        }
    }

    @Override
    public void onCaughtFire(BlockState state, Level level, BlockPos pos, @Nullable Direction direction, @Nullable LivingEntity igniter) {
        explosion(level, pos);
    }

    @Override
    protected void onProjectileHit(Level level, BlockState state, BlockHitResult hit, Projectile projectile) {
        if (level.isClientSide) {
            return;
        }
        BlockPos pos = hit.getBlockPos();
        if (projectile.isOnFire() && projectile.mayInteract(level, pos)) {
            explosion(level, pos);
        }
    }

    @Override
    public void wasExploded(Level level, BlockPos pos, Explosion explosion) {
        if (level.isClientSide) {
            return;
        }
        explosion(level, pos);
    }

    @Override
    public boolean canDropFromExplosion(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
        return false;
    }
}
