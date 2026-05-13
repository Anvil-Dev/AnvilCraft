package dev.dubhe.anvilcraft.block.logistics.chute;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.api.hammer.HammerRotateBehavior;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.better.BetterBaseEntityBlock;
import dev.dubhe.anvilcraft.block.entity.MagneticChuteBlockEntity;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.network.MachineEnableFilterPacket;
import dev.dubhe.anvilcraft.network.MachineOutputDirectionPacket;
import dev.dubhe.anvilcraft.network.SlotDisableChangePacket;
import dev.dubhe.anvilcraft.network.SlotFilterChangePacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.GameType;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;

public class MagneticChuteBlock extends BetterBaseEntityBlock implements HammerRotateBehavior, IHammerRemovable {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    public static final BooleanProperty ENABLED = BlockStateProperties.ENABLED;

    public static final VoxelShape SHAPE_UP = Shapes.or(
        Block.box(4, 8, 4, 12, 16, 12),
        Block.box(0, 0, 0, 16, 8, 16)
    );
    public static final VoxelShape SHAPE_DOWN = Shapes.or(
        Block.box(4, 0, 4, 12, 8, 12),
        Block.box(0, 8, 0, 16, 16, 16)
    );
    public static final VoxelShape SHAPE_W = Shapes.or(
        Block.box(0, 4, 4, 8, 12, 12),
        Block.box(8, 0, 0, 16, 16, 16)
    );
    public static final VoxelShape SHAPE_E = Shapes.or(
        Block.box(8, 4, 4, 16, 12, 12),
        Block.box(0, 0, 0, 8, 16, 16)
    );
    public static final VoxelShape SHAPE_S = Shapes.or(
        Block.box(4, 4, 8, 12, 12, 16),
        Block.box(0, 0, 0, 16, 16, 8)
    );
    public static final VoxelShape SHAPE_N = Shapes.or(
        Block.box(4, 4, 0, 12, 12, 8),
        Block.box(0, 0, 8, 16, 16, 16)
    );

    /**
     * 溜槽方块
     *
     * @param properties 方块属性
     */
    public MagneticChuteBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(
            this.stateDefinition.any().setValue(FACING, Direction.DOWN).setValue(ENABLED, true));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(MagneticChuteBlock::new);
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
        return new MagneticChuteBlockEntity(ModBlockEntities.MAGNETIC_CHUTE.get(), pos, state);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        level.updateNeighbourForOutputSignal(pos, this);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState blockState) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof MagneticChuteBlockEntity magneticChuteBlockEntity) {
            return magneticChuteBlockEntity.getRedstoneSignal();
        }
        return 0;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        Direction facing = context.getNearestLookingDirection();
        if (player != null && player.isShiftKeyDown()) facing = facing.getOpposite();
        BlockState neighborState = level.getBlockState(pos.relative(facing));
        boolean cannotPlace = facing == Direction.UP
            && (neighborState.is(ModBlocks.SIMPLE_CHUTE) || neighborState.is(ModBlocks.CHUTE))
            && neighborState.getValue(FACING_HOPPER) == Direction.DOWN;
        if (cannotPlace) {
            if (player != null) player.sendOverlayMessage(Component.translatable("message.anvilcraft.chute.cannot_place"));
            return null;
        }
        return this.defaultBlockState()
            .setValue(FACING, facing)
            .setValue(ENABLED, !context.getLevel().hasNeighborSignal(context.getClickedPos()));
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
        builder.add(FACING, ENABLED);
    }

    @Override
    protected void neighborChanged(
        BlockState state,
        Level level,
        BlockPos pos,
        Block block,
        @Nullable Orientation orientation,
        boolean movedByPiston
    ) {
        this.checkPoweredState(level, pos, state);
    }

    private void checkPoweredState(Level level, BlockPos pos, BlockState state) {
        boolean flag = !level.hasNeighborSignal(pos);
        if (flag != state.getValue(ENABLED)) {
            level.setBlock(pos, state.setValue(ENABLED, flag), 2);
        }
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(
        Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(
            blockEntityType,
            ModBlockEntities.MAGNETIC_CHUTE.get(),
            ((_, _, _, be) -> be.tick()));
    }

    @Override
    public InteractionResult use(
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        BlockHitResult hit
    ) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof MagneticChuteBlockEntity entity) {
            if (player.getItemInHand(hand).is(ModItems.DISK.get())) {
                return entity.useDisk(level, player, hand, player.getItemInHand(hand), hit);
            }
            if (player instanceof ServerPlayer serverPlayer) {
                if (serverPlayer.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) return InteractionResult.PASS;
                ModMenuTypes.open(serverPlayer, entity, pos);
                PacketDistributor.sendToPlayer(serverPlayer, new MachineOutputDirectionPacket(entity.getDirection()));
                PacketDistributor.sendToPlayer(serverPlayer, new MachineEnableFilterPacket(entity.isFilterEnabled()));
                for (int i = 0; i < entity.getFilteredItems().size(); i++) {
                    PacketDistributor.sendToPlayer(
                        serverPlayer,
                        new SlotDisableChangePacket(
                            i, entity.getItemHandler().getDisabled().get(i)));
                    PacketDistributor.sendToPlayer(serverPlayer, new SlotFilterChangePacket(i, entity.getFilter(i)));
                }
            }
        }
        return InteractionResult.SUCCESS;
    }
}
