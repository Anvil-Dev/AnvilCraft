package dev.dubhe.anvilcraft.block;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.api.hammer.HammerRotateBehavior;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.api.itemhandler.FilteredItemStackHandler;
import dev.dubhe.anvilcraft.api.power.IPowerComponent;
import dev.dubhe.anvilcraft.block.better.BetterBaseEntityBlock;
import dev.dubhe.anvilcraft.block.entity.BatchCrafterBlockEntity;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.network.MachineEnableFilterPacket;
import dev.dubhe.anvilcraft.network.MachineOutputDirectionPacket;
import dev.dubhe.anvilcraft.network.SlotDisableChangePacket;
import dev.dubhe.anvilcraft.network.SlotFilterChangePacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.block.DirectionalBlock;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

public class BatchCrafterBlock extends BetterBaseEntityBlock implements HammerRotateBehavior, IHammerRemovable {
    public static final DirectionProperty FACING = DirectionalBlock.FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty OVERLOAD = IPowerComponent.OVERLOAD;

    public BatchCrafterBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition
            .any()
            .setValue(POWERED, false)
            .setValue(OVERLOAD, true)
            .setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(BatchCrafterBlock::new);
    }

    @Override

    public boolean hasAnalogOutputSignal(BlockState blockState) {
        return true;
    }

    @Override

    public int getAnalogOutputSignal(BlockState blockState, Level level, BlockPos blockPos) {
        BlockEntity blockEntity = level.getBlockEntity(blockPos);
        if (blockEntity instanceof BatchCrafterBlockEntity crafterBlockEntity) {
            return crafterBlockEntity.getRedstoneSignal();
        }
        return 0;
    }

    @Override
    @SuppressWarnings({"UnreachableCode"})
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
        if (blockEntity instanceof BatchCrafterBlockEntity entity) {
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
                            i, entity.getItemHandler().getDisabled().get(i)
                        )
                    );
                    PacketDistributor.sendToPlayer(serverPlayer, new SlotFilterChangePacket(i, entity.getFilter(i)));
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override

    public void onRemove(
        BlockState state,
        Level level,
        BlockPos pos,
        BlockState newState,
        boolean movedByPiston
    ) {
        if (state.is(newState.getBlock())) return;
        if (level.getBlockEntity(pos) instanceof BatchCrafterBlockEntity entity) {
            Vec3 vec3 = entity.getBlockPos().getCenter();
            FilteredItemStackHandler itemHandler = entity.getItemHandler();
            for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
                Containers.dropItemStack(level, vec3.x, vec3.y, vec3.z, itemHandler.getStackInSlot(slot));
            }
            level.updateNeighbourForOutputSignal(pos, this);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    public VoxelShape getVisualShape(
        BlockState state,
        BlockGetter level,
        BlockPos pos,
        CollisionContext context) {
        return Shapes.empty();
    }

    public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    public boolean propagatesSkylightDown(
        BlockState state, BlockGetter level, BlockPos pos) {
        return false;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BatchCrafterBlockEntity(ModBlockEntities.BATCH_CRAFTER.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
        Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(
            type,
            ModBlockEntities.BATCH_CRAFTER.get(),
            (level1, blockPos, blockState, blockEntity) -> blockEntity.tick(level1, blockPos)
        );
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction dir = context.getNearestLookingDirection().getOpposite();
        if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) {
            dir = dir.getOpposite();
        }
        return this.defaultBlockState()
            .setValue(FACING, dir)
            .setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()))
            .setValue(OVERLOAD, true);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWERED).add(OVERLOAD).add(FACING);
    }

    @Override

    public void neighborChanged(
        BlockState state,
        Level level,
        BlockPos pos,
        Block neighborBlock,
        BlockPos neighborPos,
        boolean movedByPiston) {
        if (level.isClientSide) {
            return;
        }
        level.setBlock(pos, state.setValue(POWERED, level.hasNeighborSignal(pos)), 2);
    }

    @Override

    public void tick(
        BlockState state,
        ServerLevel level,
        BlockPos pos,
        RandomSource random) {
        if (state.getValue(POWERED) && !level.hasNeighborSignal(pos)) {
            level.setBlock(pos, state.cycle(POWERED), 2);
        }
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return this.rotate(state, mirror.getRotation(state.getValue(FACING)));
    }
}
