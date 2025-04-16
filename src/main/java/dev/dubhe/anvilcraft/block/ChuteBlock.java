package dev.dubhe.anvilcraft.block;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.api.hammer.HammerRotateBehavior;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.better.BetterBaseEntityBlock;
import dev.dubhe.anvilcraft.block.entity.ChuteBlockEntity;
import dev.dubhe.anvilcraft.block.entity.SimpleChuteBlockEntity;
import dev.dubhe.anvilcraft.init.ModBlockEntities;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.network.MachineEnableFilterPacket;
import dev.dubhe.anvilcraft.network.MachineOutputDirectionPacket;
import dev.dubhe.anvilcraft.network.SlotDisableChangePacket;
import dev.dubhe.anvilcraft.network.SlotFilterChangePacket;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
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
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.stream.Stream;

import static dev.dubhe.anvilcraft.api.itemhandler.ItemHandlerUtil.exportToTarget;
import static dev.dubhe.anvilcraft.block.SimpleChuteBlock.TALL;
import static dev.dubhe.anvilcraft.block.SimpleChuteBlock.WATERLOGGED;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ChuteBlock extends BetterBaseEntityBlock implements HammerRotateBehavior, IHammerRemovable {
    public static final DirectionProperty FACING = BlockStateProperties.FACING_HOPPER;
    public static final BooleanProperty ENABLED = BlockStateProperties.ENABLED;

    public static final VoxelShape AABB =
        Shapes.join(
            Block.box(0, 12, 0, 16, 16, 16),
            Block.box(2, 0, 2, 14, 12, 14),
            BooleanOp.OR
        );
    public static final VoxelShape AABB_W = Stream.of(
            Block.box(2, 8, 2, 14, 12, 14),
            Block.box(0, 4, 4, 12, 12, 12),
            Block.box(0, 12, 0, 16, 16, 16)
        ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR))
        .get();
    public static final VoxelShape AABB_E = Stream.of(
            Block.box(2, 8, 2, 14, 12, 14),
            Block.box(4, 4, 4, 16, 12, 12),
            Block.box(0, 12, 0, 16, 16, 16)
        ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR))
        .get();
    public static final VoxelShape AABB_S = Stream.of(
            Block.box(2, 8, 2, 14, 12, 14),
            Block.box(4, 4, 4, 12, 12, 16),
            Block.box(0, 12, 0, 16, 16, 16)
        ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR))
        .get();
    public static final VoxelShape AABB_N = Stream.of(
            Block.box(2, 8, 2, 14, 12, 14),
            Block.box(4, 4, 0, 12, 12, 12),
            Block.box(0, 12, 0, 16, 16, 16)
        ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR))
        .get();

    /**
     * 溜槽方块
     *
     * @param properties 方块属性
     */
    public ChuteBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(
            this.stateDefinition.any()
                .setValue(FACING, Direction.DOWN)
                .setValue(ENABLED, Boolean.TRUE)
        );
    }

    public static <T> boolean isChuteBlock(T obj) {
        if (obj instanceof BlockState state) {
            return state.is(ModBlocks.CHUTE.get())
                || state.is(ModBlocks.SIMPLE_CHUTE.get())
                || state.is(ModBlocks.MAGNETIC_CHUTE.get());
        }
        if (obj instanceof Block block) {
            return block == ModBlocks.CHUTE.get()
                || block == ModBlocks.SIMPLE_CHUTE.get()
                || block == ModBlocks.MAGNETIC_CHUTE.get();
        }
        return false;
    }

    @Nullable
    public static Direction getFacing(BlockState state) {
        if (state.hasProperty(FACING)) {
            return state.getValue(FACING);
        }
        if (state.hasProperty(MagneticChuteBlock.FACING)) {
            return state.getValue(MagneticChuteBlock.FACING);
        }
        return null;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(ChuteBlock::new);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ChuteBlockEntity.createBlockEntity(ModBlockEntities.CHUTE.get(), pos, state);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction dir = context.getClickedFace().getOpposite();
        Direction facing = dir.getAxis() == Direction.Axis.Y ? Direction.DOWN : dir;
        BlockState result = getState(context.getLevel(), context.getClickedPos(), facing);
        Player player = context.getPlayer();
        if (result == null && player != null) {
            player.displayClientMessage(Component.translatable("message.anvilcraft.chute.cannot_place"), true);
        }
        return result;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ENABLED);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        if (level.isClientSide) return;
        BlockState neighborState = level.getBlockState(neighborPos);
        Block neighborBlock1 = neighborState.getBlock();
        if (isChuteBlock(neighborBlock) || isChuteBlock(neighborBlock1)) {
            BlockState newState = getState(level, pos, state.getValue(FACING));
            if (newState != null && newState != state)
                level.setBlockAndUpdate(pos, newState);
        }
        this.checkPoweredState(level, pos, state);
    }

    private void checkPoweredState(Level level, BlockPos pos, BlockState state) {
        boolean flag = !level.hasNeighborSignal(pos);
        if (flag != state.getValue(ENABLED)) {
            level.setBlock(pos, state.setValue(ENABLED, flag), 2);
        }
    }

    @Override
    public void tick(
        BlockState state,
        ServerLevel level,
        BlockPos pos,
        RandomSource random) {
        if (!state.getValue(ENABLED) && !level.hasNeighborSignal(pos)) {
            level.setBlock(pos, state.cycle(ENABLED), 2);
        }
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
        Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(
            type,
            ModBlockEntities.CHUTE.get(),
            (level1, blockPos, blockState, blockEntity) -> blockEntity.tick()
        );
    }

    @Override
    public VoxelShape getShape(
        BlockState blockState,
        BlockGetter blockGetter,
        BlockPos blockPos,
        CollisionContext collisionContext) {
        return switch (blockState.getValue(FACING)) {
            case NORTH -> AABB_N;
            case SOUTH -> AABB_S;
            case WEST -> AABB_W;
            case EAST -> AABB_E;
            default -> AABB;
        };
    }

    @SuppressWarnings({"DuplicatedCode", "UnreachableCode"})
    @Override
    public InteractionResult use(
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof ChuteBlockEntity entity) {
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
                            i,
                            entity.getItemHandler().getDisabled().get(i)
                        )
                    );
                    PacketDistributor.sendToPlayer(
                        serverPlayer,
                        new SlotFilterChangePacket(
                            i,
                            entity.getFilter(i)
                        )
                    );
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof ChuteBlockEntity oldEntity) {
                IItemHandler oldHandler = oldEntity.getItemHandler();
                if (newState.is(ModBlocks.SIMPLE_CHUTE.get())) {
                    level.removeBlockEntity(pos);
                    level.setBlock(pos, newState, 2);
                    IItemHandler newHandler = null;
                    if (level.getBlockEntity(pos) instanceof SimpleChuteBlockEntity newEntity) {
                        newHandler = newEntity.getItemHandler();
                    }
                    exportToTarget(oldHandler, 64, stack -> true, newHandler);
                } else level.removeBlockEntity(pos);
                Vec3 vec3 = oldEntity.getBlockPos().getCenter();
                for (int slot = 0; slot < oldHandler.getSlots(); slot++) {
                    Containers.dropItemStack(level, vec3.x, vec3.y, vec3.z, oldHandler.getStackInSlot(slot));
                }
                level.updateNeighbourForOutputSignal(pos, this);
            }
        }

    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState blockState) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState blockState, Level level, BlockPos blockPos) {
        BlockEntity blockEntity = level.getBlockEntity(blockPos);
        if (blockEntity instanceof ChuteBlockEntity chuteBlockEntity) {
            return chuteBlockEntity.getRedstoneSignal();
        }
        return 0;
    }

    @Nullable
    BlockState getState(Level level, BlockPos pos, Direction facing) {
        boolean success = false;
        boolean tall = false;
        BlockState result = this.defaultBlockState()
            .setValue(FACING, facing)
            .setValue(ENABLED, !level.hasNeighborSignal(pos));
        //遍历六个方向 获取指向自己的溜槽
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            BlockState neighborState = level.getBlockState(neighborPos);
            if (isChuteBlock(neighborState)) {
                if (getFacing(neighborState) == dir.getOpposite()) {
                    success = true;
                    if (dir == Direction.UP) {
                        tall = !neighborState.is(ModBlocks.MAGNETIC_CHUTE.get());
                    } else if (dir == Direction.DOWN) {
                        if (facing == Direction.DOWN) {
                            return null;
                        }
                    } else {
                        if (facing.getOpposite() == getFacing(neighborState)) {
                            facing = facing.getOpposite();
                        }
                        BlockState backState = level.getBlockState(pos.relative(facing));
                        if (isChuteBlock(backState) && getFacing(backState) == facing.getOpposite()) {
                            return null;
                        }

                    }
                }

            }
        }
        if (success)
            result = ModBlocks.SIMPLE_CHUTE.getDefaultState()
                .setValue(FACING, facing)
                .setValue(TALL, tall)
                .setValue(ENABLED, !level.hasNeighborSignal(pos))
                .setValue(WATERLOGGED, level.getFluidState(pos).getType() == Fluids.WATER);
        return result;
    }
}

