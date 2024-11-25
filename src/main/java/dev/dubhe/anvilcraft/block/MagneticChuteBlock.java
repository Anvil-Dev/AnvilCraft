package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.api.hammer.HammerRotateBehavior;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.api.itemhandler.FilteredItemStackHandler;
import dev.dubhe.anvilcraft.block.better.BetterBaseEntityBlock;
import dev.dubhe.anvilcraft.block.entity.BaseChuteBlockEntity;
import dev.dubhe.anvilcraft.block.entity.MagneticChuteBlockEntity;
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
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.network.PacketDistributor;

import com.mojang.serialization.MapCodec;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

import static dev.dubhe.anvilcraft.block.ChuteBlock.ENABLED;
import static dev.dubhe.anvilcraft.block.ChuteBlock.hasChuteFacing;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class MagneticChuteBlock extends BetterBaseEntityBlock implements HammerRotateBehavior, IHammerRemovable {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public static final VoxelShape SHAPE_UP =
        Shapes.join(Block.box(4, 8, 4, 12, 16, 12), Block.box(0, 0, 0, 16, 8, 16), BooleanOp.OR);
    public static final VoxelShape SHAPE_DOWN =
        Shapes.join(Block.box(4, 0, 4, 12, 8, 12), Block.box(0, 8, 0, 16, 16, 16), BooleanOp.OR);
    public static final VoxelShape SHAPE_W =
        Shapes.join(Block.box(0, 4, 4, 8, 12, 12), Block.box(8, 0, 0, 16, 16, 16), BooleanOp.OR);
    public static final VoxelShape SHAPE_E =
        Shapes.join(Block.box(8, 4, 4, 16, 12, 12), Block.box(0, 0, 0, 8, 16, 16), BooleanOp.OR);
    public static final VoxelShape SHAPE_S =
        Shapes.join(Block.box(4, 4, 8, 12, 12, 16), Block.box(0, 0, 0, 16, 16, 8), BooleanOp.OR);
    public static final VoxelShape SHAPE_N =
        Shapes.join(Block.box(4, 4, 0, 12, 12, 8), Block.box(0, 0, 8, 16, 16, 16), BooleanOp.OR);

    /**
     * 溜槽方块
     *
     * @param properties 方块属性
     */
    public MagneticChuteBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(
            this.stateDefinition.any().setValue(FACING, Direction.DOWN).setValue(POWERED, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(MagneticChuteBlock::new);
    }

    @Override
    public VoxelShape getShape(
        BlockState blockState,
        BlockGetter blockGetter,
        BlockPos blockPos,
        CollisionContext collisionContext
    ) {
        return switch (blockState.getValue(FACING)) {
            case NORTH -> SHAPE_N;
            case SOUTH -> SHAPE_S;
            case WEST -> SHAPE_W;
            case EAST -> SHAPE_E;
            case DOWN -> SHAPE_DOWN;
            case UP -> SHAPE_UP;
        };
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
    public void onRemove(
        BlockState state,
        Level level,
        BlockPos pos,
        BlockState newState,
        boolean movedByPiston) {
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
        BlockState facingState = level.getBlockState(pos.relative(state.getValue(FACING)));
        if (facingState.is(ModBlocks.SIMPLE_CHUTE.get())
            && !newState.is(ModBlocks.SIMPLE_CHUTE.get())
            && !hasChuteFacing(level, pos.relative(state.getValue(FACING)))) {
            BlockState newBlockState = ModBlocks.CHUTE.getDefaultState();
            newBlockState = newBlockState
                .setValue(ChuteBlock.FACING, facingState.getValue(SimpleChuteBlock.FACING))
                .setValue(ENABLED, facingState.getValue(SimpleChuteBlock.ENABLED));
            level.setBlockAndUpdate(pos.relative(state.getValue(FACING)), newBlockState);
        }
        BlockState downState = level.getBlockState(pos.relative(Direction.DOWN));

        if (state.getValue(FACING) == Direction.DOWN
            && downState.is(ModBlocks.SIMPLE_CHUTE.get())
            && !newState.is(ModBlocks.SIMPLE_CHUTE.get())) {
            BlockState newBlockState = ModBlocks.SIMPLE_CHUTE.getDefaultState();
            newBlockState = newBlockState
                .setValue(ChuteBlock.FACING, downState.getValue(determineProperty(downState)))
                .setValue(ENABLED, downState.getValue(ENABLED))
                .setValue(SimpleChuteBlock.TALL, false);
            level.setBlockAndUpdate(pos.relative(Direction.DOWN), newBlockState);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    public static DirectionProperty determineProperty(BlockState blockState){
        if (blockState.is(ModBlocks.CHUTE) || blockState.is(ModBlocks.SIMPLE_CHUTE)){
            return ChuteBlock.FACING;
        }
        return FACING;
    }

    @Override
    public void onPlace(
        BlockState state,
        Level level,
        BlockPos pos,
        BlockState oldState,
        boolean movedByPiston) {
        BlockState facingState = level.getBlockState(pos.relative(state.getValue(FACING)));
        if (facingState.is(ModBlocks.CHUTE.get()) || facingState.is(ModBlocks.SIMPLE_CHUTE.get())) {
            if (facingState.getValue(ChuteBlock.FACING).getOpposite() == state.getValue(FACING)) {
                level.destroyBlock(pos, true);
                return;
            }
            BlockState newState = ModBlocks.SIMPLE_CHUTE.getDefaultState();
            newState = newState.setValue(SimpleChuteBlock.FACING, facingState.getValue(ChuteBlock.FACING))
                .setValue(SimpleChuteBlock.ENABLED, facingState.getValue(SimpleChuteBlock.ENABLED));
            if (facingState.hasProperty(SimpleChuteBlock.TALL))
                newState = newState.setValue(SimpleChuteBlock.TALL, facingState.getValue(SimpleChuteBlock.TALL));
            BlockState facingUpState =
                level.getBlockState(pos.relative(state.getValue(FACING)).relative(Direction.UP));
            if (state.getValue(FACING) == Direction.DOWN || isFacingDownChute(facingUpState)) {
                newState = newState.setValue(SimpleChuteBlock.TALL, true);
            } else {
                newState = newState.setValue(SimpleChuteBlock.TALL, false);
            }
            level.setBlockAndUpdate(pos.relative(state.getValue(FACING)), newState);
        }
        super.onPlace(state, level, pos, oldState, movedByPiston);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
            .setValue(FACING, context.getNearestLookingDirection())
            .setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
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
        builder.add(FACING, POWERED);
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
        level.setBlock(pos, state.setValue(POWERED, level.hasNeighborSignal(pos)), 2);
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
            ((level1, blockPos, blockState, blockEntity) -> blockEntity.tick()));
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
        if (level.isClientSide) {
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

    /**
     * 是朝下的溜槽
     */
    public static boolean isFacingDownChute(BlockState blockState) {
        if (!blockState.is(ModBlocks.MAGNETIC_CHUTE.get())
            && !blockState.is(ModBlocks.SIMPLE_CHUTE.get())
            && !blockState.is(ModBlocks.CHUTE.get())) {
            return false;
        }
        if (blockState.is(ModBlocks.SIMPLE_CHUTE.get()) || blockState.is(ModBlocks.CHUTE.get()))
            return blockState.getValue(ChuteBlock.FACING) == Direction.DOWN;
        return blockState.getValue(FACING) == Direction.DOWN;
    }
}
