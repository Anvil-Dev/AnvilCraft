package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.api.hammer.HammerRotateBehavior;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.entity.RubyPrismBlockEntity;
import dev.dubhe.anvilcraft.block.piston.IMoveableEntityBlock;
import dev.dubhe.anvilcraft.init.ModBlockEntities;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.mojang.serialization.MapCodec;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault// 
public class RubyPrismBlock extends BaseLaserBlock implements IHammerRemovable, HammerRotateBehavior, IMoveableEntityBlock {
    public static final VoxelShape UP_MODEL =
        Shapes.or(Block.box(0, 0, 0, 16, 4, 16), Block.box(2, 4, 2, 14, 14, 14), Block.box(4, 14, 4, 12, 16, 12));
    public static final VoxelShape DOWN_MODEL =
        Shapes.or(Block.box(0, 12, 0, 16, 16, 16), Block.box(2, 2, 2, 14, 12, 14), Block.box(4, 0, 4, 12, 2, 12));
    public static final VoxelShape NORTH_MODEL =
        Shapes.or(Block.box(0, 0, 12, 16, 16, 16), Block.box(2, 2, 2, 14, 14, 12), Block.box(4, 4, 0, 12, 12, 2));
    public static final VoxelShape SOUTH_MODEL =
        Shapes.or(Block.box(0, 0, 0, 16, 16, 4), Block.box(2, 2, 4, 14, 14, 14), Block.box(4, 4, 14, 12, 12, 16));
    public static final VoxelShape WEST_MODEL =
        Shapes.or(Block.box(12, 0, 0, 16, 16, 16), Block.box(2, 2, 2, 12, 14, 14), Block.box(0, 4, 4, 2, 12, 12));
    public static final VoxelShape EAST_MODEL =
        Shapes.or(Block.box(0, 0, 0, 4, 16, 16), Block.box(4, 2, 2, 14, 14, 14), Block.box(14, 4, 4, 16, 12, 12));
    public static final DirectionProperty FACING = DirectionalBlock.FACING;

    /**
     * 方块状态注册
     */
    public RubyPrismBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.DOWN));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(RubyPrismBlock::new);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return RubyPrismBlockEntity.createBlockEntity(ModBlockEntities.RUBY_PRISM.get(), pos, state);
    }

    @Override
    public VoxelShape getShape(
        BlockState state,
        BlockGetter level,
        BlockPos pos,
        CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case UP -> UP_MODEL;
            case DOWN -> DOWN_MODEL;
            case NORTH -> NORTH_MODEL;
            case SOUTH -> SOUTH_MODEL;
            case WEST -> WEST_MODEL;
            case EAST -> EAST_MODEL;
        };
    }

    @Override
    public @Nonnull RenderShape getRenderShape(@Nonnull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getNearestLookingDirection());
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
        Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return createTickerHelper(
            type, ModBlockEntities.RUBY_PRISM.get(), (level1, pos, state1, entity) -> entity.tick(level1));
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }
}
