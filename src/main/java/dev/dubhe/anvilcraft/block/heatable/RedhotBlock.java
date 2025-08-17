package dev.dubhe.anvilcraft.block.heatable;

import dev.dubhe.anvilcraft.block.entity.heatable.HeatableBlockEntity;
import dev.dubhe.anvilcraft.block.piston.IMoveableEntityBlock;
import dev.dubhe.anvilcraft.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

public class RedhotBlock extends HeatableBlock implements IMoveableEntityBlock {
    private final float steppingDamage;
    private final int breadthDepth;

    public RedhotBlock(Properties properties) {
        this(properties, 1, 4);
    }

    protected RedhotBlock(Properties properties, float steppingDamage, int breadthDepth) {
        super(properties);
        this.steppingDamage = steppingDamage;
        this.breadthDepth = breadthDepth;
    }

    @Override
    protected boolean hasBlockEntity() {
        return true;
    }

    @Override
    public HeatableBlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.REDHOT_BLOCK.create(pos, state);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!entity.isSteppingCarefully()
            && entity instanceof LivingEntity) {
            entity.hurt(level.damageSources().hotFloor(), this.steppingDamage);
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextDouble() > 0.5) return;
        this.tryAbsorbWater(level, pos);
    }

    protected void tryAbsorbWater(Level level, BlockPos pos) {
        if (!this.removeWaterBreadthFirstSearch(level, pos)) return;
        level.playSound(null, pos, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 1F, 1F);
    }

    private boolean removeWaterBreadthFirstSearch(Level level, BlockPos pos) {
        return BlockPos.breadthFirstTraversal(
            pos, this.breadthDepth, (int) Math.pow(2, this.breadthDepth) + 1,
            (posx, consumer) -> {
                for (Direction direction : Direction.values()) {
                    consumer.accept(posx.relative(direction));
                }
            },
            posx -> {
                if (posx.equals(pos)) return true;
                BlockState state = level.getBlockState(posx);
                FluidState fluidState = level.getFluidState(posx);
                if (!fluidState.is(Fluids.WATER)) return false;
                if (state.getBlock() instanceof BucketPickup bucketpickup
                    && !bucketpickup.pickupBlock(null, level, posx, state).isEmpty()
                ) return true;

                if (state.getBlock() instanceof LiquidBlock) {
                    level.setBlock(posx, Blocks.AIR.defaultBlockState(), 3);
                    return true;
                }

                if (!state.is(Blocks.KELP)
                    && !state.is(Blocks.KELP_PLANT)
                    && !state.is(Blocks.SEAGRASS)
                    && !state.is(Blocks.TALL_SEAGRASS)
                ) return false;

                BlockEntity blockentity = state.hasBlockEntity() ? level.getBlockEntity(posx) : null;
                dropResources(state, level, posx, blockentity);
                level.setBlock(posx, Blocks.AIR.defaultBlockState(), 3);

                return true;
            }
        ) > 1;
    }
}
