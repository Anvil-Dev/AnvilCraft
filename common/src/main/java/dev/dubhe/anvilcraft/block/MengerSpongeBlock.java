package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.init.ModFluidTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SpongeBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.NotNull;

public class MengerSpongeBlock extends SpongeBlock {

    private static final Direction[] ALL_DIRECTIONS = Direction.values();
    private BlockState state;

    public MengerSpongeBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void tryAbsorbWater(@NotNull Level level, @NotNull BlockPos pos) {
        if (this.removeFluidBreadthFirstSearch(level, pos)) {
            level.levelEvent(2001, pos, Block.getId(this.state));
        }
    }

    private boolean removeFluidBreadthFirstSearch(Level level, BlockPos pos) {
        return BlockPos.breadthFirstTraversal(pos, 6, 65, (posx, consumer) -> {

            for (Direction direction : ALL_DIRECTIONS) {
                consumer.accept(posx.relative(direction));
            }

        }, (checkedPos) -> {
            if (checkedPos.equals(pos)) {
                return true;
            } else {
                BlockState blockState = level.getBlockState(checkedPos);
                FluidState fluidState = level.getFluidState(checkedPos);
                if (!fluidState.is(ModFluidTags.MENGER_SPONGE_CAN_ABSORB)) {
                    return false;
                } else {
                    Block block = blockState.getBlock();
                    if (block instanceof BucketPickup bucketPickup) {
                        if (!bucketPickup.pickupBlock(level, checkedPos, blockState).isEmpty()) {
                            this.state = blockState;
                            return true;
                        }
                    }

                    if (blockState.getBlock() instanceof LiquidBlock) {
                        level.setBlock(checkedPos, Blocks.AIR.defaultBlockState(), 3);
                    } else {
                        if (!blockState.is(Blocks.KELP)
                            && !blockState.is(Blocks.KELP_PLANT)
                            && !blockState.is(Blocks.SEAGRASS)
                            && !blockState.is(Blocks.TALL_SEAGRASS)) {
                            return false;
                        }

                        BlockEntity blockEntity = blockState.hasBlockEntity() ? level.getBlockEntity(checkedPos) : null;
                        dropResources(blockState, level, checkedPos, blockEntity);
                        level.setBlock(checkedPos, Blocks.AIR.defaultBlockState(), 3);
                    }
                    this.state = blockState;
                    return true;
                }
            }
        }) > 1;
    }
}
