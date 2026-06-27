package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.entity.CorruptedBeaconBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BeaconBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class CorruptedBeaconBlock extends BeaconBlock implements IHammerRemovable {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    @Override
    public @Nullable Integer getBeaconColorMultiplier(
        BlockState state, LevelReader level, BlockPos pos, BlockPos beaconPos) {
        return 0xffffff;
    }

    public CorruptedBeaconBlock(Properties properties) {
        super(properties);
        BlockState defaultState = this.defaultBlockState();
        if (defaultState.equals(this.stateDefinition.any())) {
            this.registerDefaultState(this.stateDefinition.any().setValue(LIT, false));
        } else {
            this.registerDefaultState(defaultState.setValue(LIT, false));
        }
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return Objects.requireNonNull(super.getStateForPlacement(context)).setValue(LIT, false);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LIT);
    }

    @Override
    protected InteractionResult useWithoutItem(
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        BlockHitResult hitResult
    ) {
        return InteractionResult.PASS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CorruptedBeaconBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
        Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return CorruptedBeaconBlock.createTickerHelper(
            blockEntityType,
            ModBlockEntities.CORRUPTED_BEACON.get(),
            CorruptedBeaconBlockEntity::tick
        );
    }

    @Override
    protected void neighborChanged(
        BlockState state,
        Level level,
        BlockPos pos,
        Block neighborBlock,
        BlockPos neighborPos,
        boolean movedByPiston
    ) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (!level.getBlockState(pos).is(ModBlocks.CORRUPTED_BEACON)) return;

        if (level.getBlockEntity(pos) instanceof CorruptedBeaconBlockEntity entity) {
            CorruptedBeaconBlockEntity.work(level, pos, state, entity, true);
        }
    }
}
