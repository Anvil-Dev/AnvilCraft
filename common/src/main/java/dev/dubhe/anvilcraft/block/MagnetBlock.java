package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.entity.AscendingBlockEntity;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MagnetBlock extends Block {
    public static final BooleanProperty LIT = RedstoneTorchBlock.LIT;

    public MagnetBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(LIT, false));
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
        return this.defaultBlockState().setValue(LIT, context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onPlace(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull BlockState oldState,
        boolean movedByPiston
    ) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        this.attract(state, level, pos);
    }

    @Override
    protected void createBlockStateDefinition(@NotNull StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull Block neighborBlock,
        @NotNull BlockPos neighborPos,
        boolean movedByPiston
    ) {
        if (level.isClientSide) {
            return;
        }
        this.attract(state, level, pos);
        boolean bl = state.getValue(LIT);
        if (bl != level.hasNeighborSignal(pos)) {
            if (bl) {
                level.scheduleTick(pos, this, 4);
            } else {
                level.setBlock(pos, state.cycle(LIT), 2);
            }
        }
    }

    private void attract(BlockState state, @NotNull Level level, @NotNull BlockPos magnetPos) {
        if (level.isClientSide()) return;
        if (!(state.getBlock() instanceof MagnetBlock) || state.getValue(LIT)) return;
        if (level.getBlockState(magnetPos.below()).is(BlockTags.ANVIL)) return;
        int distance = AnvilCraft.config.magnetAttractsDistance;
        BlockPos currentPos = magnetPos;
        checkAnvil:
        for (int i = 0; i < distance; i++) {
            currentPos = currentPos.below();
            BlockState state1 = level.getBlockState(currentPos);
            if (state1.is(BlockTags.ANVIL)) {
                //level.setBlock(currentPos, state1.getFluidState().createLegacyBlock(), 3);
                AscendingBlockEntity.ascend(level, currentPos, state1);
                break;
            }

            List<AscendingBlockEntity> ascendingBlockEntities = level.getEntitiesOfClass(
                    AscendingBlockEntity.class,
                    new AABB(currentPos)
            );
            if (!ascendingBlockEntities.isEmpty()) return;

            List<FallingBlockEntity> entities = level.getEntitiesOfClass(
                    FallingBlockEntity.class,
                    new AABB(currentPos)
            );
            for (FallingBlockEntity entity : entities) {
                if (entity instanceof AscendingBlockEntity) continue;
                BlockState state2 = entity.getBlockState();
                if (state2.is(BlockTags.ANVIL)) {
                    entity.remove(Entity.RemovalReason.DISCARDED);
                    AscendingBlockEntity.ascend(level, currentPos, state2);
                    break checkAnvil;
                }
            }
            if (!level.isEmptyBlock(currentPos)) return;
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void tick(
        @NotNull BlockState state, @NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull RandomSource random
    ) {
        if (state.getValue(LIT) && !level.hasNeighborSignal(pos)) {
            level.setBlock(pos, state.cycle(LIT), 2);
        }
    }
}
