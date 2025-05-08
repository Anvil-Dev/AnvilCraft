package dev.dubhe.anvilcraft.block;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.hammer.HammerRotateBehavior;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.init.ModBlockTags;
import dev.dubhe.anvilcraft.util.AabbUtil;
import dev.dubhe.anvilcraft.util.AnvilUtil;
import dev.dubhe.anvilcraft.util.BreakBlockUtil;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

import static dev.dubhe.anvilcraft.api.entity.player.AnvilCraftBlockPlacer.anvilCraftBlockPlacer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BlockDevourerBlock extends DirectionalBlock implements HammerRotateBehavior, IHammerRemovable {

    public static final VoxelShape NORTH_SHAPE = Block.box(0, 0, 8, 16, 16, 16);
    public static final VoxelShape SOUTH_SHAPE = Block.box(0, 0, 0, 16, 16, 8);
    public static final VoxelShape WEST_SHAPE = Block.box(8, 0, 0, 16, 16, 16);
    public static final VoxelShape EAST_SHAPE = Block.box(0, 0, 0, 8, 16, 16);
    public static final VoxelShape UP_SHAPE = Block.box(0, 0, 0, 16, 8, 16);
    public static final VoxelShape DOWN_SHAPE = Block.box(0, 8, 0, 16, 16, 16);
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty TRIGGERED = BlockStateProperties.TRIGGERED;

    /**
     * @param properties 方块属性
     */
    public BlockDevourerBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(
            this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(TRIGGERED, false)
        );
    }

    @Override
    protected MapCodec<? extends DirectionalBlock> codec() {
        return simpleCodec(BlockDevourerBlock::new);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING).add(TRIGGERED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return this.defaultBlockState()
                .setValue(FACING, context.getNearestLookingDirection().getOpposite());
        }
        if (player.isShiftKeyDown()) {
            return this.defaultBlockState()
                .setValue(FACING, context.getNearestLookingDirection().getOpposite());
        } else {
            return this.defaultBlockState().setValue(FACING, context.getNearestLookingDirection());
        }
    }

    @Override
    protected void onPlace(BlockState state,
                           Level level,
                           BlockPos pos,
                           BlockState oldState,
                           boolean movedByPiston) {
        if (!level.isClientSide) {
            checkIfTriggered(level, state, pos);
        }
    }

    @Override
    public void tick(
        BlockState state,
        ServerLevel level,
        BlockPos pos,
        RandomSource random) {
        super.tick(state, level, pos, random);
        if (!state.getValue(TRIGGERED)) return;
        if (!BlockPlacerBlock.hasNeighborSignal(level, pos, state.getValue(FACING))) {
            level.setBlock(pos, state.setValue(TRIGGERED, false), 2);
        }
    }

    @Override
    public void neighborChanged(
        BlockState state,
        Level level,
        BlockPos pos,
        Block neighborBlock,
        BlockPos neighborPos,
        boolean movedByPiston) {
        if (!level.isClientSide) {
            checkIfTriggered(level, state, pos);
        }
    }

    private void checkIfTriggered(Level level, BlockState blockState, BlockPos blockPos) {
        boolean bl = blockState.getValue(TRIGGERED);
        if (bl != BlockPlacerBlock.hasNeighborSignal(level, blockPos, blockState.getValue(FACING))) {
            BlockState changedState = blockState.setValue(TRIGGERED, !bl);
            level.setBlock(blockPos, changedState, 2);
            if (!bl) {
                devourBlock((ServerLevel) level, blockPos, blockState.getValue(FACING), 1);
            }
        }
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(
        BlockState state,
        BlockGetter level,
        BlockPos pos,
        CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case DOWN -> DOWN_SHAPE;
            case UP -> UP_SHAPE;
            case NORTH -> NORTH_SHAPE;
            case SOUTH -> SOUTH_SHAPE;
            case WEST -> WEST_SHAPE;
            case EAST -> EAST_SHAPE;
        };
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
    }

    public void devourBlock(ServerLevel level, BlockPos devourerPos, Direction devourerDirection, int range) {
        this.devourBlock(level, devourerPos, devourerDirection, range, null);
    }

    /**
     * 破坏方块
     *
     * @param level             世界
     * @param devourerPos       破坏器坐标
     * @param devourerDirection 破坏方向
     * @param range             破坏半径(正方形)
     * @param anvil             砸到方块吞噬器的铁砧
     */
    @SuppressWarnings({"unreachable", "unused"})
    public void devourBlock(
        ServerLevel level,
        BlockPos devourerPos,
        Direction devourerDirection,
        int range,
        @Nullable Block anvil) {
        BlockPos outputPos = devourerPos.relative(devourerDirection.getOpposite());
        BlockPos devourCenterPos = devourerPos.relative(devourerDirection);
        IItemHandler itemHandler = level.getCapability(
            Capabilities.ItemHandler.BLOCK,
            devourerPos.relative(devourerDirection.getOpposite()),
            devourerDirection.getOpposite()
        );
        Vec3 center = outputPos.getCenter();
        AABB aabb = new AABB(center.add(-0.125, -0.125, -0.125), center.add(0.125, 0.125, 0.125));
        final List<BlockPos> devourBlockPosList;
        AABB devourBlockBoundingBox;
        switch (devourerDirection) {
            case DOWN, UP -> devourBlockBoundingBox = AabbUtil.create(
                devourCenterPos.relative(Direction.NORTH, range).relative(Direction.WEST, range),
                devourCenterPos.relative(Direction.SOUTH, range).relative(Direction.EAST, range));
            case NORTH, SOUTH -> devourBlockBoundingBox = AabbUtil.create(
                devourCenterPos.relative(Direction.UP, range).relative(Direction.WEST, range),
                devourCenterPos.relative(Direction.DOWN, range).relative(Direction.EAST, range));
            case WEST, EAST -> devourBlockBoundingBox = AabbUtil.create(
                devourCenterPos.relative(Direction.UP, range).relative(Direction.NORTH, range),
                devourCenterPos.relative(Direction.DOWN, range).relative(Direction.SOUTH, range));
            default -> devourBlockBoundingBox = new AABB(devourCenterPos);
        }
        boolean insertEnabled = itemHandler != null;
        boolean dropOriginalPlace = !level.noCollision(aabb);
        devourBlockPosList = BlockPos.betweenClosedStream(devourBlockBoundingBox)
            .map(BlockPos::immutable)
            .toList();

        final List<BlockPos> chainDevourBlockPosList = new ArrayList<>();
        final List<BlockPos> filteredBlockPosList = new ArrayList<>();
        for (BlockPos devourBlockPos : devourBlockPosList) {
            if (
                AnvilCraft.config.blockDevourerUpwardChainDevouring
                    && devourBlockPos.getY() == devourBlockBoundingBox.maxY
            ) {
                for (BlockPos chainDevourBlockPos : BlockPos.betweenClosed(
                    devourBlockPos.above(), devourBlockPos.above(AnvilCraft.config.blockDevourerUpwardChainDevouringDistance)
                )) {
                    if (level.getBlockState(chainDevourBlockPos).is(ModBlockTags.BLOCK_DEVOURER_CHAIN_DEVOURING)) {
                        chainDevourBlockPosList.add(chainDevourBlockPos.immutable());
                    } else {
                        break;
                    }
                }
            }

            devourSingleBlockInternalLogic(
                level, anvil, devourBlockPos, filteredBlockPosList, insertEnabled, itemHandler, dropOriginalPlace, center
            );
        }
        for (BlockPos devourBlockPos : chainDevourBlockPosList) {
            devourSingleBlockInternalLogic(
                level, anvil, devourBlockPos, filteredBlockPosList, insertEnabled, itemHandler, dropOriginalPlace, center
            );
        }
    }

    private static void devourSingleBlockInternalLogic(
        ServerLevel level, @Nullable Block anvil, BlockPos devourBlockPos, List<BlockPos> filteredBlockPosList, boolean insertEnabled,
        @Nullable IItemHandler itemHandler, boolean dropOriginalPlace, Vec3 center
    ) {
        if (filteredBlockPosList.contains(devourBlockPos)) return;
        BlockState devourBlockState = level.getBlockState(devourBlockPos);
        if (devourBlockState.isAir()) return;
        if (devourBlockState.getBlock().defaultDestroyTime() < 0) return;
        if (devourBlockState.is(ModBlockTags.BLOCK_DEVOURER_PROBABILITY_DROPPING)
            && level.random.nextDouble() > 0.05) {
            level.destroyBlock(devourBlockPos, false);
            return;
        }

        if (devourBlockState.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
            Direction toAnother = devourBlockState.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF).getDirectionToOther();
            BlockPos anotherHalfPos = devourBlockPos.relative(toAnother);
            level.destroyBlock(anotherHalfPos, false);
            filteredBlockPosList.add(anotherHalfPos);
        }

        List<ItemStack> dropList = switch (anvil) {
            case RoyalAnvilBlock ignore -> BreakBlockUtil.dropSilkTouch(level, devourBlockPos);
            case EmberAnvilBlock ignore -> BreakBlockUtil.dropSmelt(level, devourBlockPos);
            case null, default -> BreakBlockUtil.drop(level, devourBlockPos);
        };

        for (ItemStack itemStack : dropList) {
            if (insertEnabled) {
                ItemStack outItemStack = ItemHandlerHelper.insertItem(itemHandler, itemStack, true);
                if (outItemStack.isEmpty()) {
                    itemStack = ItemHandlerHelper.insertItem(itemHandler, itemStack, false);
                }
            }
            if (itemStack.isEmpty()) continue;
            if (dropOriginalPlace) {
                Block.popResource(level, devourBlockPos, itemStack);
            } else {
                AnvilUtil.dropItems(List.of(itemStack), level, center);
            }
        }

        if (!(devourBlockState.getBlock() instanceof DoublePlantBlock)) {
            devourBlockState.getBlock().playerWillDestroy(level, devourBlockPos, devourBlockState, anvilCraftBlockPlacer.getPlayer());
        }
        level.destroyBlock(devourBlockPos, false);
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
