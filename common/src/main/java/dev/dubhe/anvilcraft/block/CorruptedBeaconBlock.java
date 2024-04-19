package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.block.entity.CorruptedBeaconBlockEntity;
import dev.dubhe.anvilcraft.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CorruptedBeaconBlock extends BeaconBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public CorruptedBeaconBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(LIT, false));
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
        return this.defaultBlockState().setValue(LIT, false);
    }

    @Override
    protected void createBlockStateDefinition(@NotNull StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    public @NotNull InteractionResult use(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull Player player,
        @NotNull InteractionHand hand,
        @NotNull BlockHitResult hit
    ) {
        return InteractionResult.PASS;
    }

    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new CorruptedBeaconBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
        @NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> blockEntityType
    ) {
        return CorruptedBeaconBlock.createTickerHelper(
            blockEntityType, ModBlockEntities.CORRUPTED_BEACON.get(), CorruptedBeaconBlockEntity::tick
        );
    }
}
