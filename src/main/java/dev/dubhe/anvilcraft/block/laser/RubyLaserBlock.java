package dev.dubhe.anvilcraft.block.laser;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.api.hammer.HammerRotateBehavior;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.api.power.IPowerComponent;
import dev.dubhe.anvilcraft.api.power.IPowerComponent.Switch;
import dev.dubhe.anvilcraft.block.entity.RubyLaserBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
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
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class RubyLaserBlock extends BaseLaserBlock implements IHammerRemovable, HammerRotateBehavior {
    public static final VoxelShape UP_MODEL =
        Shapes.or(Block.box(4, 3, 4, 12, 13, 12), Block.box(5, 13, 5, 11, 16, 11), Block.box(3, 0, 3, 13, 3, 13));
    public static final VoxelShape DOWN_MODEL =
        Shapes.or(Block.box(4, 3, 4, 12, 13, 12), Block.box(5, 0, 5, 11, 3, 11), Block.box(3, 13, 3, 13, 16, 13));
    public static final VoxelShape NORTH_MODEL =
        Shapes.or(Block.box(4, 4, 3, 12, 12, 13), Block.box(5, 5, 0, 11, 11, 3), Block.box(3, 3, 13, 13, 13, 16));
    public static final VoxelShape SOUTH_MODEL =
        Shapes.or(Block.box(4, 4, 3, 12, 12, 13), Block.box(5, 5, 13, 11, 11, 16), Block.box(3, 3, 0, 13, 13, 3));
    public static final VoxelShape WEST_MODEL =
        Shapes.or(Block.box(3, 4, 4, 13, 12, 12), Block.box(0, 5, 5, 3, 11, 11), Block.box(13, 3, 3, 16, 13, 13));
    public static final VoxelShape EAST_MODEL =
        Shapes.or(Block.box(3, 4, 4, 13, 12, 12), Block.box(13, 5, 5, 16, 11, 11), Block.box(0, 3, 3, 3, 13, 13));
    public static final EnumProperty<Direction> FACING = DirectionalBlock.FACING;
    public static final BooleanProperty OVERLOAD = IPowerComponent.OVERLOAD;
    public static final EnumProperty<IPowerComponent.Switch> SWITCH = IPowerComponent.SWITCH;

    /// 方块状态注册
    public RubyLaserBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition
            .any()
            .setValue(RubyLaserBlock.FACING, Direction.DOWN)
            .setValue(RubyLaserBlock.OVERLOAD, true)
            .setValue(RubyLaserBlock.SWITCH, Switch.ON));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockBehaviour.simpleCodec(RubyLaserBlock::new);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(RubyLaserBlock.FACING).add(RubyLaserBlock.OVERLOAD).add(RubyLaserBlock.SWITCH);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return RubyLaserBlockEntity.createBlockEntity(ModBlockEntities.RUBY_LASER.get(), pos, state);
    }

    @Override
    public VoxelShape getShape(
        BlockState state,
        BlockGetter level,
        BlockPos pos,
        CollisionContext context) {
        return switch (state.getValue(RubyLaserBlock.FACING)) {
            case UP -> RubyLaserBlock.UP_MODEL;
            case DOWN -> RubyLaserBlock.DOWN_MODEL;
            case NORTH -> RubyLaserBlock.NORTH_MODEL;
            case SOUTH -> RubyLaserBlock.SOUTH_MODEL;
            case WEST -> RubyLaserBlock.WEST_MODEL;
            case EAST -> RubyLaserBlock.EAST_MODEL;
        };
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(RubyLaserBlock.FACING, context.getNearestLookingDirection());
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
        Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return BaseEntityBlock.createTickerHelper(
            type, ModBlockEntities.RUBY_LASER.get(), (level1, pos, state1, entity) -> entity.tick(level1));
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(RubyLaserBlock.FACING, rot.rotate(state.getValue(RubyLaserBlock.FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(RubyLaserBlock.FACING, mirror.mirror(state.getValue(RubyLaserBlock.FACING)));
    }
}
