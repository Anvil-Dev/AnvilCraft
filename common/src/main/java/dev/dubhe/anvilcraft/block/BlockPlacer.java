package dev.dubhe.anvilcraft.block;

import static dev.dubhe.anvilcraft.api.entity.player.AnvilCraftBlockPlacer.anvilCraftBlockPlacer;

import dev.dubhe.anvilcraft.api.depository.IItemDepository;
import dev.dubhe.anvilcraft.api.depository.ItemDepositoryHelper;
import dev.dubhe.anvilcraft.api.hammer.IHammerChangeable;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.state.Orientation;
import java.util.List;
import javax.annotation.Nonnull;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BlockPlacer extends Block implements IHammerRemovable, IHammerChangeable {

    public static final VoxelShape NORTH_UP_SHAPE = Shapes.or(
        Block.box(0, 13, 4, 16, 16, 16),
        Block.box(0, 3, 6, 16, 13, 16),
        Block.box(0, 0, 4, 16, 3, 16)
    );
    public static final VoxelShape SOUTH_UP_SHAPE = Shapes.or(
        Block.box(0, 13, 0, 16, 16, 12),
        Block.box(0, 3, 0, 16, 13, 10),
        Block.box(0, 0, 0, 16, 3, 12)
    );
    public static final VoxelShape WEST_UP_SHAPE = Shapes.or(
        Block.box(4, 13, 0, 16, 16, 16),
        Block.box(6, 3, 0, 16, 13, 16),
        Block.box(4, 0, 0, 16, 3, 16)
    );
    public static final VoxelShape EAST_UP_SHAPE = Shapes.or(
        Block.box(0, 13, 0, 12, 16, 16),
        Block.box(0, 3, 0, 10, 13, 16),
        Block.box(0, 0, 0, 12, 3, 16)
    );
    public static final VoxelShape UP_NORTH_SHAPE = Shapes.or(
        Block.box(0, 0, 13, 16, 12, 16),
        Block.box(0, 0, 3, 16, 10, 13),
        Block.box(0, 0, 0, 16, 12, 3)
    );
    public static final VoxelShape UP_SOUTH_SHAPE = Shapes.or(
        Block.box(0, 0, 13, 16, 12, 16),
        Block.box(0, 0, 3, 16, 10, 13),
        Block.box(0, 0, 0, 16, 12, 3)
    );
    public static final VoxelShape UP_WEST_SHAPE = Shapes.or(
        Block.box(13, 0, 0, 16, 12, 16),
        Block.box(3, 0, 0, 13, 10, 16),
        Block.box(0, 0, 0, 3, 12, 16)
    );
    public static final VoxelShape UP_EAST_SHAPE = Shapes.or(
        Block.box(13, 0, 0, 16, 12, 16),
        Block.box(3, 0, 0, 13, 10, 16),
        Block.box(0, 0, 0, 3, 12, 16)
    );
    public static final VoxelShape DOWN_NORTH_SHAPE = Shapes.or(
        Block.box(0, 4, 13, 16, 16, 16),
        Block.box(0, 6, 3, 16, 16, 13),
        Block.box(0, 4, 0, 16, 16, 3)
    );
    public static final VoxelShape DOWN_SOUTH_SHAPE = Shapes.or(
        Block.box(0, 4, 13, 16, 16, 16),
        Block.box(0, 6, 3, 16, 16, 13),
        Block.box(0, 4, 0, 16, 16, 3)
    );
    public static final VoxelShape DOWN_WEST_SHAPE = Shapes.or(
        Block.box(0, 4, 0, 3, 16, 16),
        Block.box(3, 6, 0, 13, 16, 16),
        Block.box(13, 4, 0, 16, 16, 16)
    );
    public static final VoxelShape DOWN_EAST_SHAPE = Shapes.or(
        Block.box(0, 4, 0, 3, 16, 16),
        Block.box(3, 6, 0, 13, 16, 16),
        Block.box(13, 4, 0, 16, 16, 16)
    );
    public static final EnumProperty<Orientation> ORIENTATION =
        EnumProperty.create("orientation", Orientation.class);
    public static final BooleanProperty TRIGGERED = BlockStateProperties.TRIGGERED;

    /**
    * @param properties 方块属性
    */
    public BlockPlacer(Properties properties) {
        super(properties);
        this.registerDefaultState(
            this.stateDefinition.any()
                .setValue(ORIENTATION, Orientation.NORTH_UP)
                .setValue(TRIGGERED, false)
        );
    }

    @Override
    protected void createBlockStateDefinition(
        @NotNull StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ORIENTATION).add(TRIGGERED);
    }

    @Override
    public void neighborChanged(
        @NotNull BlockState state,
        Level level,
        @NotNull BlockPos pos,
        @NotNull Block neighborBlock,
        @NotNull BlockPos neighborPos,
        boolean movedByPiston
    ) {
        if (level.isClientSide) {
            return;
        }
        boolean bl = state.getValue(TRIGGERED);
        BlockState changedState = state.setValue(TRIGGERED, !bl);
        if (bl != level.hasNeighborSignal(pos)) {
            if (bl) {
                level.setBlock(pos, changedState, 2);
            } else {
                level.setBlock(pos, changedState, 2);
                placeBlock(1, level, pos, state.getValue(ORIENTATION));
            }
        }
    }

    @Override
    public @Nonnull RenderShape getRenderShape(@Nonnull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    @SuppressWarnings("deprecation")
    public @NotNull VoxelShape getShape(
        @NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context
    ) {
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
    @Nullable
    public BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
        Orientation orientation;
        Direction horizontalDirection = context.getHorizontalDirection();
        if (context.getNearestLookingDirection() == Direction.UP) {
            switch (horizontalDirection) {
                default -> orientation = Orientation.UP_NORTH;
                case SOUTH -> orientation = Orientation.UP_SOUTH;
                case WEST -> orientation = Orientation.UP_WEST;
                case EAST -> orientation = Orientation.UP_EAST;
            }
        } else if (context.getNearestLookingDirection() == Direction.DOWN) {
            switch (horizontalDirection) {
                default -> orientation = Orientation.DOWN_NORTH;
                case SOUTH -> orientation = Orientation.DOWN_SOUTH;
                case WEST -> orientation = Orientation.DOWN_WEST;
                case EAST -> orientation = Orientation.DOWN_EAST;
            }
        } else {
            switch (horizontalDirection) {
                default -> orientation = Orientation.NORTH_UP;
                case SOUTH -> orientation = Orientation.SOUTH_UP;
                case WEST -> orientation = Orientation.WEST_UP;
                case EAST -> orientation = Orientation.EAST_UP;
            }
        }
        Player player = context.getPlayer();
        if (player != null && !player.isShiftKeyDown()) {
            orientation = orientation.opposite();
        }
        return defaultBlockState().setValue(ORIENTATION, orientation);
    }

    /**
     * 放置方块
     *
     * @param distance 放置距离
     * @param level 放置世界
     * @param blockPos 放置位置
     * @param orientation 放置方向
     */
    public void placeBlock(
        int distance,
        Level level,
        BlockPos blockPos,
        Orientation orientation
    ) {
        // 判断是放置位置是否为可替换方块
        Direction direction = orientation.getDirection();
        if (!(level.getBlockState(blockPos.relative(direction, distance)).is(BlockTags.REPLACEABLE))) return;
        // 获取放置方块类型
        BlockItem placeItem = null;
        if (!(level instanceof ServerLevel)) return;
        IItemDepository itemDepository =
            ItemDepositoryHelper.getItemDepository(
                level,
                blockPos.relative(direction.getOpposite()),
                direction
            );
        int slot;
        for (slot = 0; itemDepository != null && slot < itemDepository.getSlots(); slot++) {
            ItemStack blockItemStack = itemDepository.extract(slot, 1, true);
            if (!blockItemStack.isEmpty() && blockItemStack.getItem() instanceof BlockItem blockItem) {
                placeItem = blockItem;
                break;
            }
        }
        ItemEntity itemEntity = null;
        ItemStack itemStack = null;
        if (itemDepository == null) {
            AABB aabb = new AABB(blockPos.relative(direction.getOpposite()));
            List<ItemEntity> itemEntityList =
                level.getEntities(EntityTypeTest.forClass(ItemEntity.class), aabb, Entity::isAlive);
            if (itemEntityList.isEmpty()) {
                return;
            }
            itemEntity = itemEntityList.get(0);
            itemStack = itemEntity.getItem();
            if (!(itemStack.getItem() instanceof BlockItem blockItem)) {
                return;
            }
            placeItem = blockItem;
        }
        if (placeItem == null) return;
        BlockPos placePos = blockPos.relative(direction, distance);
        // 放置方块
        if (anvilCraftBlockPlacer.placeBlock(level, placePos, orientation, placeItem)
            == InteractionResult.FAIL) return;
        // 清除消耗的物品
        if (itemDepository != null) {
            itemDepository.extract(slot, 1, false);
        } else if (itemEntity != null && itemStack != null) {
            itemStack.setCount(itemStack.getCount() - 1);
            itemEntity.setItem(itemStack);
        }
    }

    @Override
    public boolean change(Player player, BlockPos blockPos, @NotNull Level level,
        ItemStack anvilHammer) {
        BlockState state = defaultBlockState();
        state.setValue(ORIENTATION, level.getBlockState(blockPos).getValue(ORIENTATION).next());
        level.setBlockAndUpdate(blockPos, state);
        return true;
    }
}
