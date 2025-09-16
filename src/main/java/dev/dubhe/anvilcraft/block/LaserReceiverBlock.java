package dev.dubhe.anvilcraft.block;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.api.hammer.IHammerChangeable;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.entity.LaserReceiverBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

public class LaserReceiverBlock extends BaseLaserBlock implements IHammerRemovable, IHammerChangeable {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public static final VoxelShape UP_SHAPE = Stream.of(
        Block.box(0, 0, 0, 16, 4, 16),
        Block.box(3, 4, 3, 13, 12, 13),
        Block.box(5, 12, 5, 11, 16, 11),
        Block.box(0, 5, 5, 16, 11, 11),
        Block.box(5, 5, 0, 11, 11, 16)
    ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    public static final VoxelShape DOWN_SHAPE = Stream.of(
        Block.box(0, 5, 5, 16, 11, 11),
        Block.box(5, 5, 0, 11, 11, 16),
        Block.box(5, 0, 5, 11, 4, 11),
        Block.box(0, 12, 0, 16, 16, 16),
        Block.box(3, 4, 3, 13, 12, 13)
    ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    public static final VoxelShape NORTH_SHAPE = Stream.of(
        Block.box(0, 5, 5, 16, 11, 11),
        Block.box(5, 0, 5, 11, 16, 11),
        Block.box(5, 5, 0, 11, 11, 4),
        Block.box(0, 0, 12, 16, 16, 16),
        Block.box(3, 3, 4, 13, 13, 12)
    ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    public static final VoxelShape SOUTH_SHAPE = Stream.of(
        Block.box(0, 5, 5, 16, 11, 11),
        Block.box(5, 0, 5, 11, 16, 11),
        Block.box(5, 5, 12, 11, 11, 16),
        Block.box(0, 0, 0, 16, 16, 4),
        Block.box(3, 3, 4, 13, 13, 12)
    ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    public static final VoxelShape EAST_SHAPE = Stream.of(
        Block.box(5, 5, 0, 11, 11, 16),
        Block.box(5, 0, 5, 11, 16, 11),
        Block.box(12, 5, 5, 16, 11, 11),
        Block.box(0, 0, 0, 4, 16, 16),
        Block.box(4, 3, 3, 12, 13, 13)
    ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    public static final VoxelShape WEST_SHAPE = Stream.of(
        Block.box(5, 5, 0, 11, 11, 16),
        Block.box(5, 0, 5, 11, 16, 11),
        Block.box(0, 5, 5, 4, 11, 11),
        Block.box(12, 0, 0, 16, 16, 16),
        Block.box(4, 3, 3, 12, 13, 13)
    ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();

    @Override
    protected MapCodec<? extends BaseLaserBlock> codec() {
        return simpleCodec(LaserReceiverBlock::new);
    }

    public LaserReceiverBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.getStateDefinition().any()
            .setValue(FACING, Direction.UP)
            .setValue(ACTIVE, false));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case UP -> UP_SHAPE;
            case DOWN -> DOWN_SHAPE;
            case NORTH -> NORTH_SHAPE;
            case SOUTH -> SOUTH_SHAPE;
            case EAST -> EAST_SHAPE;
            case WEST -> WEST_SHAPE;
        };
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clickedFace = context.getClickedFace();
        return this.defaultBlockState().setValue(FACING, clickedFace);
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getSignal(level, pos, direction);
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        if (state.getValue(FACING).getOpposite() != direction) {
            if (level.getBlockEntity(pos) instanceof LaserReceiverBlockEntity laserReceiverBlockEntity) {
                int laserLevel = laserReceiverBlockEntity.getLaserLevel();
                return Math.min(laserLevel, 15);
            }
        }
        return 0;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ACTIVE);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.LASER_RECEIVER.create(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(blockEntityType, ModBlockEntities.LASER_RECEIVER.get(), (level1, blockPos, blockState, blockEntity) -> blockEntity.tick(level));
    }

    @Override
    public boolean change(Player player, BlockPos blockPos, Level level, ItemStack anvilHammer) {
        BlockState state = level.getBlockState(blockPos);
        level.setBlockAndUpdate(blockPos, state.cycle(FACING));
        return true;
    }

    @Override
    public @Nullable Property<?> getChangeableProperty(BlockState blockState) {
        return FACING;
    }
}
