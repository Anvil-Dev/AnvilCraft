package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.api.hammer.IHammerChangeable;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.state.Orientation;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SeaPickleBlock;
import net.minecraft.world.level.block.TurtleEggBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

import static dev.dubhe.anvilcraft.api.entity.player.AnvilCraftBlockPlacer.anvilCraftBlockPlacer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BlockPlacerBlock extends Block implements IHammerRemovable, IHammerChangeable {

    public static final VoxelShape NORTH_UP_SHAPE =
        Shapes.or(Block.box(0, 13, 4, 16, 16, 16), Block.box(0, 3, 6, 16, 13, 16), Block.box(0, 0, 4, 16, 3, 16));
    public static final VoxelShape SOUTH_UP_SHAPE =
        Shapes.or(Block.box(0, 13, 0, 16, 16, 12), Block.box(0, 3, 0, 16, 13, 10), Block.box(0, 0, 0, 16, 3, 12));
    public static final VoxelShape WEST_UP_SHAPE =
        Shapes.or(Block.box(4, 13, 0, 16, 16, 16), Block.box(6, 3, 0, 16, 13, 16), Block.box(4, 0, 0, 16, 3, 16));
    public static final VoxelShape EAST_UP_SHAPE =
        Shapes.or(Block.box(0, 13, 0, 12, 16, 16), Block.box(0, 3, 0, 10, 13, 16), Block.box(0, 0, 0, 12, 3, 16));
    public static final VoxelShape UP_NORTH_SHAPE =
        Shapes.or(Block.box(0, 0, 13, 16, 12, 16), Block.box(0, 0, 3, 16, 10, 13), Block.box(0, 0, 0, 16, 12, 3));
    public static final VoxelShape UP_SOUTH_SHAPE =
        Shapes.or(Block.box(0, 0, 13, 16, 12, 16), Block.box(0, 0, 3, 16, 10, 13), Block.box(0, 0, 0, 16, 12, 3));
    public static final VoxelShape UP_WEST_SHAPE =
        Shapes.or(Block.box(13, 0, 0, 16, 12, 16), Block.box(3, 0, 0, 13, 10, 16), Block.box(0, 0, 0, 3, 12, 16));
    public static final VoxelShape UP_EAST_SHAPE =
        Shapes.or(Block.box(13, 0, 0, 16, 12, 16), Block.box(3, 0, 0, 13, 10, 16), Block.box(0, 0, 0, 3, 12, 16));
    public static final VoxelShape DOWN_NORTH_SHAPE =
        Shapes.or(Block.box(0, 4, 13, 16, 16, 16), Block.box(0, 6, 3, 16, 16, 13), Block.box(0, 4, 0, 16, 16, 3));
    public static final VoxelShape DOWN_SOUTH_SHAPE =
        Shapes.or(Block.box(0, 4, 13, 16, 16, 16), Block.box(0, 6, 3, 16, 16, 13), Block.box(0, 4, 0, 16, 16, 3));
    public static final VoxelShape DOWN_WEST_SHAPE =
        Shapes.or(Block.box(0, 4, 0, 3, 16, 16), Block.box(3, 6, 0, 13, 16, 16), Block.box(13, 4, 0, 16, 16, 16));
    public static final VoxelShape DOWN_EAST_SHAPE =
        Shapes.or(Block.box(0, 4, 0, 3, 16, 16), Block.box(3, 6, 0, 13, 16, 16), Block.box(13, 4, 0, 16, 16, 16));
    public static final EnumProperty<Orientation> ORIENTATION = EnumProperty.create("orientation", Orientation.class);
    public static final BooleanProperty TRIGGERED = BlockStateProperties.TRIGGERED;

    /**
     * @param properties 方块属性
     */
    public BlockPlacerBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition
            .any()
            .setValue(ORIENTATION, Orientation.NORTH_UP)
            .setValue(TRIGGERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ORIENTATION).add(TRIGGERED);
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
        if (!level.hasNeighborSignal(pos)) {
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
        boolean triggered = blockState.getValue(TRIGGERED);
        BlockState changedState = blockState.setValue(TRIGGERED, !triggered);
        if (triggered != level.hasNeighborSignal(blockPos)) {
            level.setBlock(blockPos, changedState, 2);
            if (triggered) {
                return;
            }
            placeBlock(1, level, blockPos, blockState.getValue(ORIENTATION));
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
        return switch (state.getValue(ORIENTATION)) {
            case NORTH_UP -> NORTH_UP_SHAPE;
            case SOUTH_UP -> SOUTH_UP_SHAPE;
            case WEST_UP -> WEST_UP_SHAPE;
            case EAST_UP -> EAST_UP_SHAPE;
            case UP_NORTH -> UP_NORTH_SHAPE;
            case UP_SOUTH -> UP_SOUTH_SHAPE;
            case UP_WEST -> UP_WEST_SHAPE;
            case UP_EAST -> UP_EAST_SHAPE;
            case DOWN_NORTH -> DOWN_NORTH_SHAPE;
            case DOWN_SOUTH -> DOWN_SOUTH_SHAPE;
            case DOWN_WEST -> DOWN_WEST_SHAPE;
            case DOWN_EAST -> DOWN_EAST_SHAPE;
        };
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Orientation orientation;
        Direction horizontalDirection = context.getHorizontalDirection();
        if (context.getNearestLookingDirection() == Direction.UP) {
            switch (horizontalDirection) {
                case SOUTH -> orientation = Orientation.UP_SOUTH;
                case WEST -> orientation = Orientation.UP_WEST;
                case EAST -> orientation = Orientation.UP_EAST;
                default -> orientation = Orientation.UP_NORTH;
            }
        } else if (context.getNearestLookingDirection() == Direction.DOWN) {
            switch (horizontalDirection) {
                case SOUTH -> orientation = Orientation.DOWN_SOUTH;
                case WEST -> orientation = Orientation.DOWN_WEST;
                case EAST -> orientation = Orientation.DOWN_EAST;
                default -> orientation = Orientation.DOWN_NORTH;
            }
        } else {
            switch (horizontalDirection) {
                case SOUTH -> orientation = Orientation.SOUTH_UP;
                case WEST -> orientation = Orientation.WEST_UP;
                case EAST -> orientation = Orientation.EAST_UP;
                default -> orientation = Orientation.NORTH_UP;
            }
        }
        if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) {
            orientation = orientation.opposite();
        }
        return defaultBlockState().setValue(ORIENTATION, orientation);
    }

    /**
     * 放置方块
     *
     * @param distance    放置距离
     * @param level       放置世界
     * @param blockPos    放置位置
     * @param orientation 放置方向
     */
    public void placeBlock(int distance, Level level, BlockPos blockPos, Orientation orientation) {
        // 判断是放置位置是否不能放置方块
        Direction direction = orientation.getDirection();
        BlockState blockState = level.getBlockState(blockPos.relative(direction, distance));
        if (canNotBePlaced(level, blockState)) {
            // 不能放置方块，方法直接结束
            return;
        }
        // 获取放置方块类型
        ItemStack placeItem = null;
        IItemHandler itemHandler = level.getCapability(
            Capabilities.ItemHandler.BLOCK,
            blockPos.relative(direction.getOpposite()),
            direction
        );
        if (itemHandler == null) {
            AABB aabb = new AABB(blockPos.relative(direction.getOpposite()));
            List<ContainerEntity> entities =
                level.getEntitiesOfClass(
                        Entity.class,
                        aabb,
                        e -> (e instanceof ContainerEntity ce) && !ce.isEmpty()
                    ).stream()
                    .map(it -> (ContainerEntity) it)
                    .toList();
            if (!entities.isEmpty()) {
                itemHandler = ((Entity) entities.getFirst()).getCapability(
                    Capabilities.ItemHandler.ENTITY,
                    null
                );
            }
        }
        int slot;
        for (slot = 0; itemHandler != null && slot < itemHandler.getSlots(); slot++) {
            ItemStack blockItemStack = itemHandler.extractItem(slot, 1, true);
            if (!blockItemStack.isEmpty() && blockItemStack.getItem() instanceof BlockItem) {
                placeItem = blockItemStack;
                break;
            }
        }

        ItemEntity itemEntity = null;
        // 从放置器背后的掉落物中获取物品
        if (itemHandler == null) {
            AABB aabb = new AABB(blockPos.relative(direction.getOpposite()));
            List<ItemEntity> entities =
                level.getEntities(
                    EntityTypeTest.forClass(ItemEntity.class),
                    aabb,
                    Entity::isAlive
                );
            if (entities.isEmpty()) {
                return;
            }
            for (ItemEntity entity : entities) {
                if (entity.getItem().getItem() instanceof BlockItem) {
                    itemEntity = entity;
                    placeItem = entity.getItem();
                    break;
                }
            }
            if (itemEntity == null) {
                return;
            }
        }
        if (placeItem == null) {
            return;
        }
        // 检查海龟蛋，海泡菜，蜡烛是否可以被放置
        BlockItem blockItem = (BlockItem) placeItem.getItem();
        boolean isInvalidBlock = blockState.is(Blocks.TURTLE_EGG)
            || blockState.is(Blocks.SEA_PICKLE)
            || (blockState.getBlock() instanceof CandleBlock);
        boolean blockMismatch = blockState.getBlock() != blockItem.getBlock();
        if (isInvalidBlock && blockMismatch) {
            return;
        }
        BlockPos placePos = blockPos.relative(direction, distance);
        // 放置方块
        if (anvilCraftBlockPlacer.placeBlock(
            level,
            placePos,
            orientation,
            blockItem,
            placeItem) == InteractionResult.FAIL
        ) {
            return;
        }
        // 清除消耗的物品
        if (itemHandler == null) {
            if (itemEntity == null) {
                return;
            }
            int count = itemEntity.getItem().getCount();
            // 处理细雪桶
            if (itemEntity.getItem().is(Items.POWDER_SNOW_BUCKET)) {
                itemEntity.setItem(new ItemStack(Items.BUCKET, count));
            } else {
                itemEntity.getItem().shrink(1);
            }
        } else {
            if (itemHandler.getStackInSlot(slot).is(Items.POWDER_SNOW_BUCKET)) {
                itemHandler.insertItem(slot, new ItemStack(Items.BUCKET), false);
            }
            itemHandler.extractItem(slot, 1, false);
        }
    }

    /**
     * @param level      放置世界
     * @param blockState 方块放置器前面方块的方块状态
     * @return 当前位置是否不能放置方块
     */
    private boolean canNotBePlaced(Level level, BlockState blockState) {
        if (level instanceof ServerLevel) {
            // 可替换方块
            if (blockState.is(BlockTags.REPLACEABLE)) {
                return false;
            }
            // 海龟蛋
            if (blockState.is(Blocks.TURTLE_EGG) && blockState.getValue(TurtleEggBlock.EGGS) < 4) {
                return false;
            }
            // 海泡菜
            if (blockState.is(Blocks.SEA_PICKLE) && blockState.getValue(SeaPickleBlock.PICKLES) < 4) {
                return false;
            }
            // 蜡烛
            return !(blockState.getBlock() instanceof CandleBlock) || blockState.getValue(CandleBlock.CANDLES) >= 4;
        }
        return true;
    }

    @Override
    public boolean change(Player player, BlockPos blockPos, Level level, ItemStack anvilHammer) {
        BlockState state = defaultBlockState();
        state = state.setValue(
            ORIENTATION,
            level.getBlockState(blockPos).getValue(ORIENTATION).next()
        );
        level.setBlockAndUpdate(blockPos, state);
        return true;
    }

    @Override
    public @Nullable Property<?> getChangeableProperty(BlockState blockState) {
        return ORIENTATION;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(ORIENTATION, state.getValue(ORIENTATION).rotate(rotation));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(ORIENTATION, state.getValue(ORIENTATION).mirror(mirror));
    }
}
