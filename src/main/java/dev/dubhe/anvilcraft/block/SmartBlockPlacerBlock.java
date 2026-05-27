package dev.dubhe.anvilcraft.block;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.util.ShapeUtil;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.api.power.IPowerComponent;
import dev.dubhe.anvilcraft.block.better.BetterBaseEntityBlock;
import dev.dubhe.anvilcraft.block.entity.SmartBlockPlacerBlockEntity;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class SmartBlockPlacerBlock extends BetterBaseEntityBlock implements IHammerRemovable {
    public static final BooleanProperty UPSIDE_DOWN = BooleanProperty.create("upside_down");
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty OVERLOAD = IPowerComponent.OVERLOAD;

    // 基础碰撞箱（朝北，正放）
    private static final VoxelShape SHAPE_NORTH = ShapeUtil.merge(
        Block.box(0, 0, 0, 16, 4, 16),      // 底座
        Block.box(2, 4, 2, 14, 8, 14),      // 中间连接
        Block.box(4, 8, 4, 12, 16, 12),     // 机械臂主体
        Block.box(4, 4, 14, 12, 10, 16)     // 输入口（南侧）
    );

    // 使用 ShapeUtil.rotate 自动生成其他水平朝向
    private static final VoxelShape SHAPE_WEST = ShapeUtil.rotate(Direction.Axis.Y, 90, SHAPE_NORTH);
    private static final VoxelShape SHAPE_SOUTH = ShapeUtil.rotate(Direction.Axis.Y, 180, SHAPE_NORTH);
    private static final VoxelShape SHAPE_EAST = ShapeUtil.rotate(Direction.Axis.Y, 270, SHAPE_NORTH);

    // 倒挂状态：使用 Axis.X 旋转 180 度实现 Y 轴翻转
    private static final VoxelShape SHAPE_NORTH_UPSIDE = ShapeUtil.rotate(Direction.Axis.X, 180, SHAPE_NORTH);
    private static final VoxelShape SHAPE_WEST_UPSIDE = ShapeUtil.rotate(Direction.Axis.X, 180, SHAPE_WEST);
    private static final VoxelShape SHAPE_SOUTH_UPSIDE = ShapeUtil.rotate(Direction.Axis.X, 180, SHAPE_SOUTH);
    private static final VoxelShape SHAPE_EAST_UPSIDE = ShapeUtil.rotate(Direction.Axis.X, 180, SHAPE_EAST);

    public SmartBlockPlacerBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH)
            .setValue(UPSIDE_DOWN, false)
            .setValue(POWERED, false)
            .setValue(OVERLOAD, true));
    }

    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected MapCodec<? extends BetterBaseEntityBlock> codec() {
        return Block.simpleCodec(SmartBlockPlacerBlock::new);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(HorizontalDirectionalBlock.FACING, UPSIDE_DOWN, POWERED, OVERLOAD);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getClickedFace();
        boolean upsideDown = facing == Direction.DOWN;
        Level level = context.getLevel();

        // 获取玩家的水平朝向
        Direction horizontalFacing = context.getHorizontalDirection().getOpposite();

        // 潜行时与玩家朝向相反，否则与玩家朝向相同
        if (context.getPlayer() != null && !context.getPlayer().isShiftKeyDown()) {
            horizontalFacing = horizontalFacing.getOpposite();
        }

        return this.defaultBlockState()
            .setValue(HorizontalDirectionalBlock.FACING, horizontalFacing)
            .setValue(UPSIDE_DOWN, upsideDown)
            .setValue(POWERED, level.hasNeighborSignal(context.getClickedPos()))
            .setValue(OVERLOAD, true);
    }

    @Override
    public VoxelShape getShape(
        BlockState state,
        BlockGetter level,
        BlockPos pos,
        CollisionContext context
    ) {
        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        boolean upsideDown = state.getValue(UPSIDE_DOWN);
        
        return switch (facing) {
            case SOUTH -> upsideDown ? SHAPE_SOUTH_UPSIDE : SHAPE_SOUTH;
            case WEST -> upsideDown ? SHAPE_WEST_UPSIDE : SHAPE_WEST;
            case EAST -> upsideDown ? SHAPE_EAST_UPSIDE : SHAPE_EAST;
            default -> upsideDown ? SHAPE_NORTH_UPSIDE : SHAPE_NORTH;
        };
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SmartBlockPlacerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
        Level level,
        BlockState state,
        BlockEntityType<T> type
    ) {
        if (level.isClientSide()) {
            return (level1, pos, state1, entity) -> {
                if (entity instanceof SmartBlockPlacerBlockEntity be) {
                    be.tickClient();
                }
            };
        } else {
            return (level1, pos, state1, entity) -> {
                if (entity instanceof SmartBlockPlacerBlockEntity be) {
                    be.tickServer(level1, pos);
                }
            };
        }
    }

    @Override
    protected InteractionResult useWithoutItem(
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        BlockHitResult hitResult
    ) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof SmartBlockPlacerBlockEntity placerEntity) {
            if (player.getItemInHand(InteractionHand.MAIN_HAND).is(ModItems.DISK.get())
                || player.getItemInHand(InteractionHand.OFF_HAND).is(ModItems.DISK.get())) {
                InteractionHand hand = player.getItemInHand(InteractionHand.MAIN_HAND).is(ModItems.DISK.get())
                    ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
                return placerEntity.useDisk(level, player, hand, player.getItemInHand(hand), hitResult);
            }
            if (player instanceof ServerPlayer serverPlayer) {
                var menuProvider = state.getMenuProvider(level, pos);
                if (menuProvider != null) {
                    ModMenuTypes.open(serverPlayer, menuProvider, pos);
                }
            }
        }
        return InteractionResult.sidedSuccess(false);
    }

    @Override
    public void neighborChanged(
        BlockState state,
        Level level,
        BlockPos pos,
        Block neighborBlock,
        BlockPos neighborPos,
        boolean movedByPiston
    ) {
        if (level.isClientSide) {
            return;
        }
        level.setBlock(pos, state.setValue(POWERED, level.hasNeighborSignal(pos)), 2);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            if (!level.isClientSide) {
                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (blockEntity instanceof SmartBlockPlacerBlockEntity placerEntity) {
                    // 掉落Disk物品栏中的物品
                    for (int i = 0; i < placerEntity.getDiskInventory().getContainerSize(); i++) {
                        ItemStack stack = placerEntity.getDiskInventory().getItem(i);
                        if (!stack.isEmpty()) {
                            Vec3 vec3 = pos.getCenter();
                            net.minecraft.world.entity.item.ItemEntity itemEntity = new net.minecraft.world.entity.item.ItemEntity(
                                level,
                                vec3.x,
                                vec3.y,
                                vec3.z,
                                stack
                            );
                            itemEntity.setDefaultPickUpDelay();
                            level.addFreshEntity(itemEntity);
                        }
                    }
                    
                    // 掉落书物品栏中的物品（输入书，如果有的话）
                    for (int i = 0; i < placerEntity.getBookInventory().getContainerSize(); i++) {
                        ItemStack stack = placerEntity.getBookInventory().getItem(i);
                        if (!stack.isEmpty()) {
                            Vec3 vec3 = pos.getCenter();
                            net.minecraft.world.entity.item.ItemEntity itemEntity = new net.minecraft.world.entity.item.ItemEntity(
                                level,
                                vec3.x,
                                vec3.y,
                                vec3.z,
                                stack
                            );
                            itemEntity.setDefaultPickUpDelay();
                            level.addFreshEntity(itemEntity);
                        }
                    }
                }
            }
            super.onRemove(state, level, pos, newState, movedByPiston);
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(POWERED) && !level.hasNeighborSignal(pos)) {
            level.setBlock(pos, state.cycle(POWERED), 2);
        }
    }
    
    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }
    
    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (level.isClientSide) {
            return 0;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof SmartBlockPlacerBlockEntity placerEntity) {
            return placerEntity.getComparatorOutput();
        }
        return 0;
    }
}