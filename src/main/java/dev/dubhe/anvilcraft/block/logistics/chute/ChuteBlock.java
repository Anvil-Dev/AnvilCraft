package dev.dubhe.anvilcraft.block.logistics.chute;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.api.hammer.HammerRotateBehavior;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.better.BetterBaseEntityBlock;
import dev.dubhe.anvilcraft.block.entity.ChuteBlockEntity;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
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
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;

import java.util.stream.Stream;

public class ChuteBlock extends BetterBaseEntityBlock implements HammerRotateBehavior, IHammerRemovable {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING_HOPPER;
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

    /// 溜槽方块
    ///
    /// @param properties 方块属性
    public ChuteBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(
            this.stateDefinition.any()
                .setValue(ChuteBlock.FACING, Direction.DOWN)
                .setValue(ChuteBlock.ENABLED, Boolean.TRUE)
        );
    }

    public static <T> boolean isChuteBlock(T obj) {
        if (obj instanceof BlockState state) {
            return state.is(ModBlocks.CHUTE.get())
                || state.is(ModBlocks.SIMPLE_CHUTE.get())
                || state.is(ModBlocks.MAGNETIC_CHUTE.get())
                || state.is(ModBlocks.SIMPLE_MAGNETIC_CHUTE.get());
        }
        if (obj instanceof Block block) {
            return block == ModBlocks.CHUTE.get()
                || block == ModBlocks.SIMPLE_CHUTE.get()
                || block == ModBlocks.MAGNETIC_CHUTE.get()
                || block == ModBlocks.SIMPLE_MAGNETIC_CHUTE.get();
        }
        return false;
    }

    @Nullable
    public static Direction getFacing(BlockState state) {
        if (state.hasProperty(ChuteBlock.FACING)) {
            return state.getValue(ChuteBlock.FACING);
        }
        if (state.hasProperty(MagneticChuteBlock.FACING)) {
            return state.getValue(MagneticChuteBlock.FACING);
        }
        return null;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockBehaviour.simpleCodec(ChuteBlock::new);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ChuteBlockEntity.createBlockEntity(ModBlockEntities.CHUTE.get(), pos, state);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction dir = context.getClickedFace().getOpposite();
        if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) dir = dir.getOpposite();
        Direction facing = dir.getAxis() == Direction.Axis.Y ? Direction.DOWN : dir;
        BlockState result = this.getState(context.getLevel(), context.getClickedPos(), facing);
        Player player = context.getPlayer();
        if (result == null && player != null) {
            player.sendOverlayMessage(Component.translatable("message.anvilcraft.chute.cannot_place"));
        }
        return result;
    }

    @Override
    public boolean change(Player player, BlockPos pos, Level level, ItemStack anvilHammer) {
        BlockState oldState = level.getBlockState(pos);
        Direction oldFacing = oldState.getValue(ChuteBlock.FACING);
        Direction newFacing = switch (oldFacing) {
            case WEST -> Direction.DOWN;
            case DOWN -> Direction.NORTH;
            default -> oldFacing.getClockWise();
        };
        BlockState facingState = level.getBlockState(pos.relative(newFacing));
        if (ChuteBlock.isChuteBlock(facingState) && ChuteBlock.getFacing(facingState) == newFacing.getOpposite()) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            level.levelEvent(2001, pos, Block.getId(oldState));
            Block.dropResources(oldState, level, pos);
            return true;
        }
        HammerRotateBehavior.DEFAULT.change(player, pos, level, anvilHammer);
        return true;
    }

    @Override
    public Property<?> getChangeableProperty(BlockState blockState) {
        return ChuteBlock.FACING;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(ChuteBlock.FACING, rotation.rotate(state.getValue(ChuteBlock.FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return this.rotate(state, mirror.getRotation(state.getValue(ChuteBlock.FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ChuteBlock.FACING, ChuteBlock.ENABLED);
    }

    @Override
    protected BlockState updateShape(
        BlockState state,
        LevelReader level,
        ScheduledTickAccess ticks,
        BlockPos pos,
        Direction directionToNeighbour,
        BlockPos neighbourPos,
        BlockState neighbourState,
        RandomSource random
    ) {
        if (level.isClientSide()) return state;
        Block neighborBlock = neighbourState.getBlock();
        if (ChuteBlock.isChuteBlock(neighborBlock)) {
            BlockState newState = this.getState(level, pos, state.getValue(ChuteBlock.FACING));
            if (newState != null && newState != state) state = newState;
        }
        state = this.checkPoweredState(level, pos, state);
        return state;
    }

    private BlockState checkPoweredState(LevelReader level, BlockPos pos, BlockState state) {
        boolean flag = !level.hasNeighborSignal(pos);
        if (flag == state.getValue(ChuteBlock.ENABLED)) return state;
        return state.setValue(ChuteBlock.ENABLED, flag);
    }

    @Override
    public void tick(
        BlockState state,
        ServerLevel level,
        BlockPos pos,
        RandomSource random) {
        if (!state.getValue(ChuteBlock.ENABLED) && !level.hasNeighborSignal(pos)) {
            level.setBlock(pos, state.cycle(ChuteBlock.ENABLED), 2);
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
        return BaseEntityBlock.createTickerHelper(
            type,
            ModBlockEntities.CHUTE.get(),
            (_, _, _, be) -> be.tick()
        );
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext collisionContext) {
        return switch (state.getValue(ChuteBlock.FACING)) {
            case NORTH -> ChuteBlock.AABB_N;
            case SOUTH -> ChuteBlock.AABB_S;
            case WEST -> ChuteBlock.AABB_W;
            case EAST -> ChuteBlock.AABB_E;
            default -> ChuteBlock.AABB;
        };
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
        // 手持铁砧锤时由 change 方法处理旋转/爆炸，不打开 GUI
        if (player.getItemInHand(hand).is(ModItemTags.ANVIL_HAMMER)) {
            return InteractionResult.PASS;
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
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        level.updateNeighbourForOutputSignal(pos, this);
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
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof ChuteBlockEntity chuteBlockEntity) {
            return chuteBlockEntity.getRedstoneSignal();
        }
        return 0;
    }

    @Nullable
    BlockState getState(LevelReader level, BlockPos pos, Direction facing) {
        boolean success = false;
        boolean tall = false;
        BlockState result = this.defaultBlockState()
            .setValue(ChuteBlock.FACING, facing)
            .setValue(ChuteBlock.ENABLED, !level.hasNeighborSignal(pos));
        // 遍历六个方向 获取指向自己的溜槽
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            BlockState neighborState = level.getBlockState(neighborPos);
            if (ChuteBlock.isChuteBlock(neighborState)) {
                if (ChuteBlock.getFacing(neighborState) == dir.getOpposite()) {
                    success = true;
                    if (dir == Direction.UP) {
                        tall = !neighborState.is(ModBlocks.MAGNETIC_CHUTE.get());
                    } else if (dir == Direction.DOWN) {
                        if (facing == Direction.DOWN) {
                            return null;
                        }
                    } else {
                        if (facing.getOpposite() == ChuteBlock.getFacing(neighborState)) {
                            facing = facing.getOpposite();
                        }
                        BlockState backState = level.getBlockState(pos.relative(facing));
                        if (ChuteBlock.isChuteBlock(backState) && ChuteBlock.getFacing(backState) == facing.getOpposite()) {
                            return null;
                        }

                    }
                }

            }
        }
        if (success) {
            result = ModBlocks.SIMPLE_CHUTE.getDefaultState()
                .setValue(SimpleChuteBlock.FACING, facing)
                .setValue(SimpleChuteBlock.TALL, tall)
                .setValue(SimpleChuteBlock.ENABLED, !level.hasNeighborSignal(pos))
                .setValue(SimpleChuteBlock.WATERLOGGED, level.getFluidState(pos).getType() == Fluids.WATER);
        }
        return result;
    }
}

