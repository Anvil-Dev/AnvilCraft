package dev.dubhe.anvilcraft.block;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.api.hammer.HammerRotateBehavior;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.api.itemhandler.FilteredItemStackHandler;
import dev.dubhe.anvilcraft.block.better.BetterBaseEntityBlock;
import dev.dubhe.anvilcraft.block.entity.BaseChuteBlockEntity;
import dev.dubhe.anvilcraft.block.entity.OverflowChuteBlockEntity;
import dev.dubhe.anvilcraft.init.ModSoundEvents;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
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
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * 溢流溜槽：在磁性溜槽的基础上，允许在除出入口以外的方向开口作为溢流口。
 *
 * <p>出口可输出时物品走出口，出口堵住时改从溢流口抛出（不带动量）。</p>
 */
public class OverflowChuteBlock extends BetterBaseEntityBlock implements HammerRotateBehavior, IHammerRemovable {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty ENABLED = BlockStateProperties.ENABLED;

    public static final BooleanProperty OVERFLOW_DOWN = BooleanProperty.create("overflow_down");
    public static final BooleanProperty OVERFLOW_UP = BooleanProperty.create("overflow_up");
    public static final BooleanProperty OVERFLOW_NORTH = BooleanProperty.create("overflow_north");
    public static final BooleanProperty OVERFLOW_SOUTH = BooleanProperty.create("overflow_south");
    public static final BooleanProperty OVERFLOW_WEST = BooleanProperty.create("overflow_west");
    public static final BooleanProperty OVERFLOW_EAST = BooleanProperty.create("overflow_east");

    public static final VoxelShape SHAPE_UP = Shapes.join(
        Block.box(3, 8, 3, 13, 16, 13),
        Block.box(2, 0, 2, 14, 8, 14),
        BooleanOp.OR
    );
    public static final VoxelShape SHAPE_DOWN = Shapes.join(
        Block.box(3, 0, 3, 13, 8, 13),
        Block.box(2, 8, 2, 14, 16, 14),
        BooleanOp.OR
    );
    public static final VoxelShape SHAPE_W = Shapes.join(
        Block.box(0, 3, 3, 8, 13, 13),
        Block.box(8, 2, 2, 16, 14, 14),
        BooleanOp.OR
    );
    public static final VoxelShape SHAPE_E = Shapes.join(
        Block.box(8, 3, 3, 16, 13, 13),
        Block.box(0, 2, 2, 8, 14, 14),
        BooleanOp.OR
    );
    public static final VoxelShape SHAPE_S = Shapes.join(
        Block.box(3, 3, 8, 13, 13, 16),
        Block.box(2, 2, 0, 14, 14, 8),
        BooleanOp.OR
    );
    public static final VoxelShape SHAPE_N = Shapes.join(
        Block.box(3, 3, 0, 13, 13, 8),
        Block.box(2, 2, 8, 14, 14, 16),
        BooleanOp.OR
    );

    public OverflowChuteBlock(Properties properties) {
        super(properties);
        BlockState state = this.stateDefinition.any()
            .setValue(FACING, Direction.DOWN)
            .setValue(ENABLED, true);
        for (Direction direction : Direction.values()) {
            state = state.setValue(overflowProperty(direction), false);
        }
        this.registerDefaultState(state);
    }

    /**
     * 取得某个方向对应的溢流口方块状态。
     *
     * @param direction 方向
     * @return 该方向的溢流口属性
     */
    public static BooleanProperty overflowProperty(Direction direction) {
        return switch (direction) {
            case DOWN -> OVERFLOW_DOWN;
            case UP -> OVERFLOW_UP;
            case NORTH -> OVERFLOW_NORTH;
            case SOUTH -> OVERFLOW_SOUTH;
            case WEST -> OVERFLOW_WEST;
            case EAST -> OVERFLOW_EAST;
        };
    }

    /**
     * 判断该状态是否在指定方向开启了溢流口。
     *
     * @param state     方块状态
     * @param direction 方向
     * @return 若为溢流溜槽且该方向溢流口开启则返回 {@code true}
     */
    public static boolean hasOverflowPort(BlockState state, Direction direction) {
        return state.getBlock() instanceof OverflowChuteBlock
               && state.getValue(overflowProperty(direction));
    }

    /**
     * 清除出入口方向上无效的溢流口。
     *
     * <p>出入口方向不可能有溢流口，但方块状态本身允许存在（避免读取到错误状态时崩溃），
     * 因此在运行时统一清除为 false。</p>
     *
     * @param state 方块状态
     * @return 清除后的方块状态，无需改动时返回原对象
     */
    public static BlockState sanitizeOverflowPorts(BlockState state) {
        Direction output = state.getValue(FACING);
        BlockState result = state;
        for (Direction direction : new Direction[]{output, output.getOpposite()}) {
            BooleanProperty property = overflowProperty(direction);
            if (result.getValue(property)) {
                result = result.setValue(property, false);
            }
        }
        return result;
    }

    /**
     * 红石只关闭朝向主出口的输出，溢流口与输入不受影响。
     *
     * @param level 世界
     * @param pos   方块位置
     * @param state 方块状态
     */
    private void checkPoweredState(Level level, BlockPos pos, BlockState state) {
        boolean enabled = !level.hasNeighborSignal(pos);
        if (enabled != state.getValue(ENABLED)) {
            level.setBlock(pos, state.setValue(ENABLED, enabled), 2);
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
        if (!level.isClientSide) {
            this.checkPoweredState(level, pos, state);
        }
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(OverflowChuteBlock::new);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext collisionContext) {
        return switch (state.getValue(FACING)) {
            case NORTH -> SHAPE_N;
            case SOUTH -> SHAPE_S;
            case WEST -> SHAPE_W;
            case EAST -> SHAPE_E;
            case DOWN -> SHAPE_DOWN;
            case UP -> SHAPE_UP;
        };
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new OverflowChuteBlockEntity(ModBlockEntities.OVERFLOW_CHUTE.get(), pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(
        Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(
            blockEntityType,
            ModBlockEntities.OVERFLOW_CHUTE.get(),
            ((level1, blockPos, blockState, blockEntity) -> blockEntity.tick()));
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getNearestLookingDirection();
        Player player = context.getPlayer();
        if (player != null && player.isShiftKeyDown()) facing = facing.getOpposite();
        // 前方溜槽朝自己输出时顺延其方向，与普通溜槽的放置行为一致，避免嘴对嘴
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState frontState = level.getBlockState(pos.relative(facing));
        if (ChuteBlock.outputsToward(frontState, facing.getOpposite())) {
            facing = facing.getOpposite();
        }
        // 嘴对嘴：输入侧与输出侧两端都有溜槽输出对着自己（夹心），禁止放置
        BlockState inputState = level.getBlockState(pos.relative(facing.getOpposite()));
        BlockState outputState = level.getBlockState(pos.relative(facing));
        if (ChuteBlock.outputsToward(inputState, facing)
            && ChuteBlock.outputsToward(outputState, facing.getOpposite())) {
            if (player != null) {
                player.displayClientMessage(
                    Component.translatable("message.anvilcraft.chute.cannot_place"),
                    true
                );
            }
            return null;
        }
        return this.defaultBlockState()
            .setValue(FACING, facing)
            .setValue(ENABLED, !level.hasNeighborSignal(pos));
    }

    /**
     * 溢流溜槽没有 GUI，铁砧锤的单击不应打开界面。
     */
    @Nullable
    @Override
    protected MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return null;
    }

    /**
     * 手持铁砧锤单击非出入口方向时，开闭该方向的溢流口。
     *
     * <p>调整朝向由铁砧锤长按的轮盘完成，两者互不冲突。</p>
     */
    @Override
    public InteractionResult use(
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        BlockHitResult hit
    ) {
        if (!player.getItemInHand(hand).is(ModItemTags.ANVIL_HAMMER)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        Direction direction = hit.getDirection();
        Direction output = state.getValue(FACING);
        if (direction == output || direction == output.getOpposite()) {
            return InteractionResult.SUCCESS;
        }
        BooleanProperty property = overflowProperty(direction);
        boolean opened = !state.getValue(property);
        // 开启溢流口会让本方块朝该方向输出，若该方向的溜槽也朝自己输出就成了嘴对嘴
        if (opened && ChuteBlock.outputsToward(level.getBlockState(pos.relative(direction)), direction.getOpposite())) {
            player.displayClientMessage(
                Component.translatable("message.anvilcraft.chute.cannot_place"),
                true
            );
            return InteractionResult.SUCCESS;
        }
        level.setBlockAndUpdate(pos, state.setValue(property, opened));
        level.playSound(
            null,
            pos,
            ModSoundEvents.ANVIL_HAMMER_ROTATE_BLOCK.get(),
            SoundSource.BLOCKS,
            1.0f,
            opened ? 1.2f : 0.8f
        );
        return InteractionResult.SUCCESS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof BaseChuteBlockEntity entity) {
                Vec3 vec3 = entity.getBlockPos().getCenter();
                FilteredItemStackHandler depository = entity.getItemHandler();
                for (int slot = 0; slot < depository.getSlots(); slot++) {
                    Containers.dropItemStack(level, vec3.x, vec3.y, vec3.z, depository.getStackInSlot(slot));
                }
                level.updateNeighbourForOutputSignal(pos, this);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState blockState) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState blockState, Level level, BlockPos blockPos) {
        if (level.getBlockEntity(blockPos) instanceof OverflowChuteBlockEntity entity) {
            return entity.getRedstoneSignal();
        }
        return 0;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return this.rotate(state, mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(
            FACING,
            ENABLED,
            OVERFLOW_DOWN,
            OVERFLOW_UP,
            OVERFLOW_NORTH,
            OVERFLOW_SOUTH,
            OVERFLOW_WEST,
            OVERFLOW_EAST
        );
    }
}
