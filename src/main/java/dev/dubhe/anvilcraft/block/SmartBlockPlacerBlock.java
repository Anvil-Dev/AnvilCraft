package dev.dubhe.anvilcraft.block;

import com.mojang.serialization.MapCodec;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class SmartBlockPlacerBlock extends BetterBaseEntityBlock implements IHammerRemovable {
    public static final BooleanProperty UPSIDE_DOWN = BooleanProperty.create("upside_down");
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty OVERLOAD = IPowerComponent.OVERLOAD;

    private static final VoxelShape SHAPE_FLOOR = Shapes.or(
        Block.box(0, 0, 0, 16, 4, 16),
        Block.box(2, 4, 2, 14, 8, 14),
        Block.box(4, 8, 4, 12, 16, 12),
        Block.box(4, 4, 14, 12, 10, 16)
    );

    private static final VoxelShape SHAPE_CEILING = Shapes.or(
        Block.box(0, 12, 0, 16, 16, 16),
        Block.box(2, 0, 2, 14, 12, 14)
    );

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
        boolean upsideDown = state.getValue(UPSIDE_DOWN);
        return upsideDown ? SHAPE_CEILING : SHAPE_FLOOR;
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
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(POWERED) && !level.hasNeighborSignal(pos)) {
            level.setBlock(pos, state.cycle(POWERED), 2);
        }
    }
}