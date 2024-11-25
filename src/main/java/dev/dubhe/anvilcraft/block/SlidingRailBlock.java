package dev.dubhe.anvilcraft.block;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.MovingPistonBlock;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.timers.TimerCallback;
import net.minecraft.world.level.timers.TimerQueue;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class SlidingRailBlock extends Block {
    public static final VoxelShape OUTSIDE = Block.box(0, 0, 0, 16, 16, 16);
    public static final VoxelShape AABB_X = Stream.of(
        Block.box(0, 6, 11, 16, 12, 14),
        Block.box(0, 0, 0, 16, 6, 16),
        Block.box(0, 12, 0, 16, 16, 5),
        Block.box(0, 12, 11, 16, 16, 16),
        Block.box(0, 6, 2, 16, 12, 5)
    ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    public static final VoxelShape AABB_Z =
        Stream.of(
            Block.box(2, 6, 0, 5, 12, 16),
            Block.box(0, 0, 0, 16, 6, 16),
            Block.box(11, 12, 0, 16, 16, 16),
            Block.box(0, 12, 0, 5, 16, 16),
            Block.box(11, 6, 0, 14, 12, 16)
        ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;
    public static final HashMap<BlockPos, PistonPushInfo> MOVING_PISTON_MAP = new HashMap<>();

    public SlidingRailBlock(Properties properties) {
        super(properties);
        registerDefaultState(getStateDefinition().any().setValue(AXIS, Direction.Axis.X));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
            .setValue(AXIS, context.getHorizontalDirection().getOpposite().getAxis());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    @Override
    protected VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return OUTSIDE;
    }

    @Override
    public VoxelShape getShape(
        BlockState blockState,
        BlockGetter blockGetter,
        BlockPos blockPos,
        CollisionContext collisionContext
    ) {
        return switch (blockState.getValue(AXIS)) {
            case X:
                yield AABB_X;
            case Z:
            default:
                yield AABB_Z;
        };
    }

    @Override
    public void onNeighborChange(
        BlockState state,
        LevelReader level,
        BlockPos pos,
        BlockPos neighbor
    ) {
        if (level.getBlockState(neighbor).is(Blocks.MOVING_PISTON)) {

            Direction dir = level.getBlockState(neighbor).getValue(FACING);
            if (dir == Direction.UP || dir == Direction.DOWN) {
                if (MOVING_PISTON_MAP.containsKey(pos)) MOVING_PISTON_MAP.remove(pos);
                return;
            }
            PistonPushInfo ppi = new PistonPushInfo(neighbor, dir);
            if (MOVING_PISTON_MAP.containsKey(pos)) {
                MOVING_PISTON_MAP.get(pos).fromPos = neighbor;
            } else MOVING_PISTON_MAP.put(pos, ppi);


        }
    }

    @Override
    protected void neighborChanged(
        BlockState state,
        Level level,
        BlockPos pos,
        Block block,
        BlockPos fromPos,
        boolean isMoving
    ) {
        if (level.isClientSide) return;
        BlockState blockState = level.getBlockState(fromPos);
        if (!MOVING_PISTON_MAP.containsKey(pos)) return;
        if (blockState.is(Blocks.MOVING_PISTON)) return;
        level.scheduleTick(pos, this, 2);

    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!MOVING_PISTON_MAP.containsKey(pos)) return;
        if (!MOVING_PISTON_MAP.get(pos).extending && MOVING_PISTON_MAP.get(pos).isSourcePiston) {
            MOVING_PISTON_MAP.remove(pos);
            return;
        } else if (!MOVING_PISTON_MAP.get(pos).extending) {
            MOVING_PISTON_MAP.get(pos).direction = MOVING_PISTON_MAP.get(pos).direction.getOpposite();
        }
        BlockPos fromPos = MOVING_PISTON_MAP.get(pos).fromPos;
        pushBlock(fromPos, level, MOVING_PISTON_MAP.get(pos).direction);
        MOVING_PISTON_MAP.remove(pos);
    }

    /**
     * 滑轨推动上方方块
     *
     * @param pos       推动的方块位置
     * @param level     世界
     * @param direction 推动方向
     */
    public static void pushBlock(BlockPos pos, Level level, Direction direction) {
        moveBlocks(level, pos, direction);
    }

    private static void moveBlocks(Level level, BlockPos pos, Direction facing) {
        PistonStructureResolver pistonstructureresolver = new PistonStructureResolver(level, pos.relative(facing.getOpposite()), facing, true);
        if (!pistonstructureresolver.resolve()) return;
        Map<BlockPos, BlockState> map = Maps.newHashMap();
        List<BlockPos> list = pistonstructureresolver.getToPush();
        List<BlockState> list1 = Lists.newArrayList();

        for (BlockPos blockPos1 : list) {
            BlockState blockstate = level.getBlockState(blockPos1);
            list1.add(blockstate);
            map.put(blockPos1, blockstate);
        }

        List<BlockPos> list2 = pistonstructureresolver.getToDestroy();
        BlockState[] ablockstate = new BlockState[list.size() + list2.size()];
        Direction direction = facing;//facing.getOpposite();
        int i = 0;

        for (int j = list2.size() - 1; j >= 0; j--) {
            BlockPos blockPos2 = list2.get(j);
            BlockState blockstate1 = level.getBlockState(blockPos2);
            BlockEntity blockentity = blockstate1.hasBlockEntity() ? level.getBlockEntity(blockPos2) : null;
            dropResources(blockstate1, level, blockPos2, blockentity);
            blockstate1.onDestroyedByPushReaction(level, blockPos2, direction, level.getFluidState(blockPos2));
            if (!blockstate1.is(BlockTags.FIRE)) {
                level.addDestroyBlockEffect(blockPos2, blockstate1);
            }

            ablockstate[i++] = blockstate1;
        }

        for (int k = list.size() - 1; k >= 0; k--) {
            BlockPos blockpos3 = list.get(k);
            blockpos3 = blockpos3.relative(direction);
            map.remove(blockpos3);
            BlockState blockstate8 = Blocks.MOVING_PISTON.defaultBlockState().setValue(FACING, facing);
            level.setBlock(blockpos3, blockstate8, 68);
            level.setBlockEntity(
                MovingPistonBlock.newMovingBlockEntity(
                    blockpos3,
                    blockstate8,
                    list1.get(k),
                    facing,
                    true,
                    false
                )
            );
            ablockstate[i++] = level.getBlockState(blockpos3);
        }

        BlockState blockState3 = Blocks.AIR.defaultBlockState();

        for (BlockPos blockpos4 : map.keySet()) {
            level.setBlock(blockpos4, blockState3, 82);
        }

        for (Map.Entry<BlockPos, BlockState> entry : map.entrySet()) {
            BlockPos blockpos5 = entry.getKey();
            BlockState blockstate2 = entry.getValue();
            blockstate2.updateIndirectNeighbourShapes(level, blockpos5, 2);
            blockState3.updateNeighbourShapes(level, blockpos5, 2);
            blockState3.updateIndirectNeighbourShapes(level, blockpos5, 2);
        }

        i = 0;

        for (int l = list2.size() - 1; l >= 0; l--) {
            BlockState blockstate7 = ablockstate[i++];
            BlockPos blockpos6 = list2.get(l);
            blockstate7.updateIndirectNeighbourShapes(level, blockpos6, 2);
            level.updateNeighborsAt(blockpos6, blockstate7.getBlock());
        }

        for (int i1 = list.size() - 1; i1 >= 0; i1--) {
            level.updateNeighborsAt(list.get(i1), ablockstate[i++].getBlock());
        }

    }

    public record PushBlockData(BlockPos blockPos, Level level, Direction direction) {
    }

    public static class PushBlockTimeCallback implements TimerCallback<MinecraftServer> {
        private final PushBlockData pushBlockData;

        public PushBlockTimeCallback(PushBlockData pushBlockData) {
            this.pushBlockData = pushBlockData;
        }

        @Override
        public void handle(
            MinecraftServer obj,
            TimerQueue<MinecraftServer> manager,
            long gameTime
        ) {
            pushBlock(pushBlockData.blockPos, pushBlockData.level, pushBlockData.direction);
        }
    }

    public static class PistonPushInfo {
        public BlockPos fromPos;
        public Direction direction;
        public boolean extending;
        public boolean isSourcePiston;

        public PistonPushInfo(BlockPos blockPos, Direction direction) {
            this.fromPos = blockPos;
            this.direction = direction;
            this.extending = false;
            this.isSourcePiston = false;
        }
    }
}
