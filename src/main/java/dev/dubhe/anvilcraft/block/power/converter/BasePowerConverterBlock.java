package dev.dubhe.anvilcraft.block.power.converter;

import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.api.power.IPowerComponent;
import dev.dubhe.anvilcraft.block.better.BetterBaseEntityBlock;
import dev.dubhe.anvilcraft.block.entity.PowerConverterBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public abstract class BasePowerConverterBlock extends BetterBaseEntityBlock implements IHammerRemovable {
    private final int inputPower;

    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty OVERLOAD = IPowerComponent.OVERLOAD;

    /// 基本电源转换器模块
    public BasePowerConverterBlock(Properties properties, int inputPower) {
        super(properties);
        registerDefaultState(
            stateDefinition.any()
                .setValue(FACING, Direction.DOWN)
                .setValue(POWERED, false)
                .setValue(OVERLOAD, true)
        );
        this.inputPower = inputPower;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED, OVERLOAD);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return this.rotate(state, mirror.getRotation(state.getValue(FACING)));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
            .setValue(FACING, context.getClickedFace().getOpposite())
            .setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }

    @Override
    protected void neighborChanged(
        BlockState state,
        Level level,
        BlockPos pos,
        Block neighborBlock,
        @Nullable Orientation orientation,
        boolean movedByPiston
    ) {
        if (level.isClientSide()) return;
        boolean powered = level.hasNeighborSignal(pos);
        if (state.getValue(POWERED) != powered) {
            level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_CLIENTS);
        }
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
        Level level,
        BlockState state,
        BlockEntityType<T> type
    ) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(
            type,
            ModBlockEntities.POWER_CONVERTER.get(),
            (level1, blockPos, blockState, blockEntity) -> blockEntity.tick()
        );
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PowerConverterBlockEntity(ModBlockEntities.POWER_CONVERTER.get(), pos, state, this.inputPower);
    }

    @Override
    public abstract VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context);

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
    }
}
