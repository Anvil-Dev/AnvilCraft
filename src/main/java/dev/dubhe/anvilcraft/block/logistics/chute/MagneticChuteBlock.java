package dev.dubhe.anvilcraft.block.logistics.chute;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.api.hammer.HammerRotateBehavior;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.better.BetterBaseEntityBlock;
import dev.dubhe.anvilcraft.block.entity.MagneticChuteBlockEntity;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
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
    public static final BooleanProperty HEAD = BooleanProperty.create("head");

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

    /// 溜槽方块
    ///
    /// @param properties 方块属性
    public MagneticChuteBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(
            this.stateDefinition.any().setValue(MagneticChuteBlock.FACING, Direction.DOWN).setValue(MagneticChuteBlock.ENABLED, true)
                .setValue(MagneticChuteBlock.HEAD, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockBehaviour.simpleCodec(MagneticChuteBlock::new);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext collisionContext) {
        return switch (state.getValue(MagneticChuteBlock.FACING)) {
            case NORTH -> MagneticChuteBlock.SHAPE_N;
            case SOUTH -> MagneticChuteBlock.SHAPE_S;
            case WEST -> MagneticChuteBlock.SHAPE_W;
            case EAST -> MagneticChuteBlock.SHAPE_E;
            case DOWN -> MagneticChuteBlock.SHAPE_DOWN;
            case UP -> MagneticChuteBlock.SHAPE_UP;
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

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        Direction facing = context.getNearestLookingDirection();
        if (player != null && player.isShiftKeyDown()) facing = facing.getOpposite();
        // 点击某溜槽的输出口面放置时，新方块顺延其输出方向
        Direction clickedFace = context.getClickedFace();
        BlockState behindState = level.getBlockState(pos.relative(clickedFace.getOpposite()));
        if ((behindState.is(ModBlocks.MAGNETIC_CHUTE.get()) || behindState.is(ModBlocks.SIMPLE_MAGNETIC_CHUTE.get()))
            && ChuteBlock.getFacing(behindState) == clickedFace) {
            facing = clickedFace;
        }
        // 嘴对嘴：输入侧与输出侧两端都有溜槽输出对着自己（夹心），禁止放置
        BlockState inputState = level.getBlockState(pos.relative(facing.getOpposite()));
        BlockState outputState = level.getBlockState(pos.relative(facing));
        if (ChuteBlock.isChuteBlock(inputState) && ChuteBlock.getFacing(inputState) == facing
            && ChuteBlock.isChuteBlock(outputState) && ChuteBlock.getFacing(outputState) == facing.getOpposite()) {
            if (player != null) player.sendOverlayMessage(Component.translatable("message.anvilcraft.chute.cannot_place"));
            return null;
        }
        // 被任意溜槽指向时，直接以简易磁性溜槽形态放置
        if (SimpleMagneticChuteBlock.isPointedByChute(level, pos)) {
            return ModBlocks.SIMPLE_MAGNETIC_CHUTE.getDefaultState()
                .setValue(SimpleMagneticChuteBlock.FACING, facing)
                .setValue(SimpleMagneticChuteBlock.ENABLED, !level.hasNeighborSignal(pos))
                .setValue(SimpleMagneticChuteBlock.WATERLOGGED, level.getFluidState(pos).getType() == Fluids.WATER);
        }
        return this.defaultBlockState()
            .setValue(MagneticChuteBlock.FACING, facing)
            .setValue(MagneticChuteBlock.ENABLED, !context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }

    @Override
    public boolean change(Player player, BlockPos pos, Level level, ItemStack anvilHammer) {
        BlockState oldState = level.getBlockState(pos);
        Direction oldFacing = oldState.getValue(MagneticChuteBlock.FACING);
        Direction newFacing = switch (oldFacing) {
            case WEST -> Direction.UP;
            case UP -> Direction.DOWN;
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
        return MagneticChuteBlock.FACING;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(MagneticChuteBlock.FACING, rotation.rotate(state.getValue(MagneticChuteBlock.FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return this.rotate(state, mirror.getRotation(state.getValue(MagneticChuteBlock.FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MagneticChuteBlock.FACING, MagneticChuteBlock.ENABLED, MagneticChuteBlock.HEAD);
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
        if (!level.isClientSide()) {
            // 被任意溜槽指向时，降级为简易磁性溜槽
            if (SimpleMagneticChuteBlock.isPointedByChute(level, pos)) {
                level.setBlockAndUpdate(pos, ModBlocks.SIMPLE_MAGNETIC_CHUTE.getDefaultState()
                    .setValue(SimpleMagneticChuteBlock.FACING, state.getValue(MagneticChuteBlock.FACING))
                    .setValue(SimpleMagneticChuteBlock.ENABLED, !level.hasNeighborSignal(pos))
                    .setValue(SimpleMagneticChuteBlock.WATERLOGGED, level.getFluidState(pos).getType() == Fluids.WATER)
                    .setValue(SimpleMagneticChuteBlock.HEAD, false));
                return;
            }
            // 上方有朝下的普通溜槽/简易溜槽时，附加连接头模型
            BlockState aboveState = level.getBlockState(pos.above());
            boolean hasHead = (aboveState.is(ModBlocks.CHUTE.get()) || aboveState.is(ModBlocks.SIMPLE_CHUTE.get()))
                && aboveState.getValue(ChuteBlock.FACING) == Direction.DOWN;
            if (state.getValue(MagneticChuteBlock.HEAD) != hasHead) {
                level.setBlockAndUpdate(pos, state.setValue(MagneticChuteBlock.HEAD, hasHead));
                return;
            }
        }
        this.checkPoweredState(level, pos, state);
    }

    private void checkPoweredState(Level level, BlockPos pos, BlockState state) {
        boolean flag = !level.hasNeighborSignal(pos);
        if (flag != state.getValue(MagneticChuteBlock.ENABLED)) {
            level.setBlock(pos, state.setValue(MagneticChuteBlock.ENABLED, flag), 2);
        }
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(
        Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide()) {
            return null;
        }
        return BaseEntityBlock.createTickerHelper(
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
        // 手持铁砧锤时由 change 方法处理旋转/爆炸，不打开 GUI
        if (player.getItemInHand(hand).is(ModItemTags.ANVIL_HAMMER)) {
            return InteractionResult.PASS;
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
