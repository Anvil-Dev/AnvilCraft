package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.api.depository.IItemDepository;
import dev.dubhe.anvilcraft.api.depository.ItemDepositoryHelper;
import dev.dubhe.anvilcraft.api.hammer.IHammerChangeable;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.state.Orientation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
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
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SeaPickleBlock;
import net.minecraft.world.level.block.TurtleEggBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
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

import javax.annotation.Nonnull;
import java.util.List;

import static dev.dubhe.anvilcraft.api.entity.player.AnvilCraftBlockPlacer.anvilCraftBlockPlacer;

public class BlockPlacerBlock extends Block implements IHammerRemovable, IHammerChangeable {

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
    public BlockPlacerBlock(Properties properties) {
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
    public void tick(
            @NotNull BlockState state,
            @NotNull ServerLevel level,
            @NotNull BlockPos pos,
            @NotNull RandomSource random
    ) {
        super.tick(state, level, pos, random);
        if (!state.getValue(TRIGGERED)) return;
        if (!level.hasNeighborSignal(pos))
            level.setBlock(pos, state.setValue(TRIGGERED, false), 2);
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
        boolean triggered = state.getValue(TRIGGERED);
        BlockState changedState = state.setValue(TRIGGERED, !triggered);
        if (triggered != level.hasNeighborSignal(pos)) {
            level.setBlock(pos, changedState, 2);
            if (triggered) {
                return;
            }
            tryPlaceBlock(1, level, pos, state.getValue(ORIENTATION));
        }
    }

    @Override
    public @Nonnull RenderShape getRenderShape(@Nonnull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    @SuppressWarnings("deprecation")
    public @NotNull VoxelShape getShape(
            @NotNull BlockState state, @NotNull BlockGetter level,
            @NotNull BlockPos pos, @NotNull CollisionContext context
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
        orientation = orientation.opposite();
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
    @Deprecated
    public void placeBlock(
            int distance,
            Level level,
            BlockPos blockPos,
            Orientation orientation
    ) {
        // 判断是放置位置是否不能放置方块
        Direction direction = orientation.getDirection();
        BlockState blockState = level.getBlockState(blockPos.relative(direction, distance));
        if (canNotBePlaced(level, blockState)) {
            // 不能放置方块，方法直接结束
            return;
        }
        // 获取放置方块类型
        BlockItem placeItem = null;
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
            for (ItemEntity entity : itemEntityList) {
                if (entity.getItem().getItem() instanceof BlockItem blockItem) {
                    itemEntity = entity;
                    itemStack = entity.getItem();
                    placeItem = blockItem;
                    break;
                }
            }
            if (itemEntity == null || itemStack == null) {
                return;
            }
        }
        if (placeItem == null) {
            return;
        }
        // 检查海龟蛋，海泡菜，蜡烛是否可以被放置
        if ((blockState.is(Blocks.TURTLE_EGG)
                || blockState.is(Blocks.SEA_PICKLE)
                || (blockState.getBlock() instanceof CandleBlock))
                && blockState.getBlock() != placeItem.getBlock()) {
            return;
        }
        BlockPos placePos = blockPos.relative(direction, distance);
        // 放置方块
        if (anvilCraftBlockPlacer.placeBlock(level, placePos, orientation, placeItem) == InteractionResult.FAIL) {
            return;
        }
        // 清除消耗的物品
        if (itemDepository != null) {
            // 处理细雪桶：无法直接操作容器或实体身上的物品栏
            itemDepository.extract(slot, 1, false);
        } else if (itemEntity != null && itemStack != null) {
            int count = itemStack.getCount();
            // 处理细雪桶
            if (itemStack.is(Items.POWDER_SNOW_BUCKET)) {
                itemEntity.setItem(new ItemStack(Items.BUCKET, count));
            } else {
                itemStack.setCount(count - 1);
                itemEntity.setItem(itemStack);
            }
        }
    }

    /**
     * 本方法是{@link BlockPlacerBlock#placeBlock(int, Level, BlockPos, Orientation)}的重新实现，旧的放置方块的方法中有个问题，
     * 即放置细雪桶时，不会返还空桶。由于旧代码的限制，修复这个问题较为困难，因为它不能对容器或实体的物品栏直接进行修改，只能修改物品堆对象，
     * 然而物品堆不是完全可变的，内部包含的物品被final修饰无法修改，为了实现返还空桶的功能，必须直接对物品栏进行操作，所以在本方法中，
     * 当需要放置某个方块时，直接在方块或实体的物品栏中获取对应物品，对于掉落物实体，也封装成一个物品栏，以便在需要时能够方便地进行修改，
     * 实现替换物品的功能。此外，本方法还可以放置物品展示框里的物品
     *
     * @param distance    放置距离
     * @param level       放置世界
     * @param blockPos    方块放置器的位置
     * @param orientation 方块放置器的朝向
     * @author cdqtzrc
     */
    public void tryPlaceBlock(int distance, Level level, BlockPos blockPos, Orientation orientation) {
        // 判断是放置位置是否不能放置方块
        Direction direction = orientation.getDirection();
        BlockState blockState = level.getBlockState(blockPos.relative(direction, distance));
        if (canNotBePlaced(level, blockState)) {
            // 不能放置方块，方法直接结束
            return;
        }
        BlockItem placeItem = null;
        Container inventory = this.getInventory(level, blockPos.relative(direction.getOpposite()));
        if (inventory.isEmpty()) {
            return;
        }
        int index;
        for (index = 0; index < inventory.getContainerSize(); index++) {
            if (inventory.getItem(index).getItem() instanceof BlockItem blockItem) {
                placeItem = blockItem;
                break;
            }
        }
        if (placeItem == null) {
            return;
        }
        // 检查海龟蛋，海泡菜，蜡烛是否可以被放置
        if ((blockState.is(Blocks.TURTLE_EGG)
                || blockState.is(Blocks.SEA_PICKLE)
                || (blockState.getBlock() instanceof CandleBlock))
                && blockState.getBlock() != placeItem.getBlock()) {
            return;
        }
        BlockPos placePos = blockPos.relative(direction, distance);
        // 放置方块
        if (anvilCraftBlockPlacer.placeBlock(level, placePos, orientation, placeItem) == InteractionResult.FAIL) {
            return;
        }
        // 清除消耗的物品
        ItemStack itemStack = inventory.getItem(index);
        if (itemStack.is(Items.POWDER_SNOW_BUCKET)) {
            inventory.setItem(index, new ItemStack(Items.BUCKET, itemStack.getCount()));
        } else if (inventory instanceof ItemFrameWrapperContainer) {
            // 正常情况下物品展示框中物品的堆叠数量永远为1
            inventory.setItem(index, ItemStack.EMPTY);
        } else {
            itemStack.shrink(1);
        }
    }

    /**
     * @param level    方块放置器所在的世界
     * @param blockPos 方块放置器背后的方块坐标
     * @return 从方块放置器背后的方块或实体中获取一个物品栏
     */
    private Container getInventory(Level level, BlockPos blockPos) {
        BlockEntity blockEntity = level.getBlockEntity(blockPos);
        // 如果方块是个容器，直接将方块的物品栏返回
        if (blockEntity instanceof Container container) {
            return container;
        }
        // 获取方块放置器背后的容器实体
        List<ContainerEntity> entities = level.getEntitiesOfClass(Entity.class, new AABB(blockPos)).stream()
                .filter(entity -> entity instanceof ContainerEntity)
                .map(entity -> (ContainerEntity) entity)
                .toList();
        // 如果背后有容器实体，将随机一个实体（转化为容器对象后）返回
        if (!entities.isEmpty()) {
            return entities.get(level.getRandom().nextInt(0, entities.size()));
        }
        // 如果背后没有容器实体，就查找方块后面的掉落物并封装成一个物品栏
        AABB aabb = new AABB(blockPos);
        List<ItemEntity> itemEntities
                = level.getEntities(EntityTypeTest.forClass(ItemEntity.class), aabb, Entity::isAlive);
        if (!itemEntities.isEmpty()) {
            return new ItemEntityWrapperContainer(itemEntities);
        }
        // 从放置器背后的物品展示框获取物品栏
        List<ItemFrame> itemFrames = level.getEntities(EntityTypeTest.forClass(ItemFrame.class), aabb, Entity::isAlive);
        if (itemFrames.isEmpty()) {
            // 如果放置器背后没有物品展示框，返回空物品栏
            return new SimpleContainer();
        }
        return new ItemFrameWrapperContainer(itemFrames);
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
    public boolean change(Player player, BlockPos blockPos, @NotNull Level level,
                          ItemStack anvilHammer) {
        BlockState state = defaultBlockState();
        state = state.setValue(ORIENTATION, level.getBlockState(blockPos).getValue(ORIENTATION).next());
        level.setBlockAndUpdate(blockPos, state);
        return true;
    }

    private abstract static class AbstractWrapperContainer extends SimpleContainer {
        protected final List<? extends Entity> list;

        private AbstractWrapperContainer(List<? extends Entity> list) {
            super(list.size());
            this.list = list;
            this.init();
        }

        protected abstract void init();

        protected abstract void setItemStack(int slotIndex, ItemStack itemStack);

        @Override
        public @NotNull ItemStack removeItem(int slot, int amount) {
            ItemStack itemStack = super.removeItem(slot, amount);
            this.setItemStack(slot, this.getItem(slot));
            return itemStack;
        }

        @Override
        public @NotNull ItemStack removeItemNoUpdate(int slot) {
            ItemStack itemStack = super.removeItemNoUpdate(slot);
            this.setItemStack(slot, this.getItem(slot));
            return itemStack;
        }

        @Override
        public void setItem(int slot, @NotNull ItemStack stack) {
            super.setItem(slot, stack);
            this.setItemStack(slot, this.getItem(slot));
        }

        @Override
        public void clearContent() {
            super.clearContent();
            for (int index = 0; index < this.getContainerSize(); index++) {
                this.setItemStack(index, ItemStack.EMPTY);
            }
        }
    }

    private static class ItemEntityWrapperContainer extends AbstractWrapperContainer {
        private ItemEntityWrapperContainer(List<ItemEntity> list) {
            super(list);
        }

        @Override
        protected void init() {
            for (int index = 0; index < this.getContainerSize(); index++) {
                this.setItem(index, ((ItemEntity) list.get(index)).getItem());
            }
        }

        @Override
        protected void setItemStack(int slotIndex, ItemStack itemStack) {
            ((ItemEntity) this.list.get(slotIndex)).setItem(itemStack);
        }
    }

    private static class ItemFrameWrapperContainer extends AbstractWrapperContainer {
        public ItemFrameWrapperContainer(List<ItemFrame> list) {
            super(list);
        }

        @Override
        protected void init() {
            for (int index = 0; index < this.getContainerSize(); index++) {
                this.setItem(index, ((ItemFrame) list.get(index)).getItem());
            }
        }

        @Override
        protected void setItemStack(int slotIndex, ItemStack itemStack) {
            ((ItemFrame) this.list.get(slotIndex)).setItem(itemStack);
        }
    }
}
