package dev.dubhe.anvilcraft.block;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.api.power.IPowerComponent;
import dev.dubhe.anvilcraft.block.better.BetterBaseEntityBlock;
import dev.dubhe.anvilcraft.block.entity.ExpCollectorBlockEntity;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ExpCollectorBlock extends BetterBaseEntityBlock implements IHammerRemovable {
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty OVERLOAD = IPowerComponent.OVERLOAD;

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(ExpCollectorBlock::new);
    }

    public ExpCollectorBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(
            this.stateDefinition.any()
                .setValue(POWERED, false)
                .setValue(OVERLOAD, true)
        );
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        return this.defaultBlockState()
            .setValue(POWERED, level.hasNeighborSignal(context.getClickedPos()))
            .setValue(OVERLOAD, true);
    }

    @Override
    protected void neighborChanged(
        BlockState state,
        Level level,
        BlockPos pos,
        Block neighborBlock,
        BlockPos neighborPos,
        boolean movedByPiston
    ) {
        if (level.isClientSide()) {
            return;
        }
        level.setBlock(pos, state.setValue(POWERED, level.hasNeighborSignal(pos)), 2);
    }

    @Override
    protected void tick(
        BlockState state,
        ServerLevel level,
        BlockPos pos,
        RandomSource random
    ) {
        if (state.getValue(POWERED) && !level.hasNeighborSignal(pos)) {
            level.setBlock(pos, state.cycle(POWERED), 2);
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(
        ItemStack stack,
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        BlockHitResult hitResult
    ) {
        if (level.isClientSide()) {
            return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
        }
        if (level.getBlockEntity(pos) instanceof ExpCollectorBlockEntity expCollectorBlockEntity) {
            ItemStack item = player.getItemInHand(hand);
            if (item.is(Items.BUCKET)) {
                IFluidHandler fluidHandler = expCollectorBlockEntity.getFluidHandler();
                FluidStack fluidStack = fluidHandler.getFluidInTank(0);
                if (fluidStack.getAmount() >= 1000) {
                    fluidHandler.drain(1000, IFluidHandler.FluidAction.EXECUTE);
                    expCollectorBlockEntity.setChanged();
                    level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
                    level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS);
                    if (player.isCreative()) {
                        player.addItem(ModItems.EXP_BUCKET.asStack());
                    } else {
                        if (item.getCount() > 1) {
                            ItemStack itemStack = stack.copyWithCount(stack.getCount() - 1);
                            player.setItemInHand(hand, itemStack);
                            player.addItem(ModItems.EXP_BUCKET.asStack());
                        } else if (item.getCount() == 1) {
                            player.setItemInHand(hand, ModItems.EXP_BUCKET.asStack());
                        }
                    }
                    return ItemInteractionResult.SUCCESS;
                }
            }
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof ExpCollectorBlockEntity expCollectorBlockEntity) {
            if (player instanceof ServerPlayer serverPlayer) {
                if (serverPlayer.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) {
                    return InteractionResult.PASS;
                }
                ModMenuTypes.open(serverPlayer, expCollectorBlockEntity, pos);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (state.is(newState.getBlock())) return;
        if (level.getBlockEntity(pos) instanceof ExpCollectorBlockEntity entity) {
            List<ChunkPos> chunkPosList = entity.getPoachingMapPositions(8);
            for (ChunkPos chunkPos : chunkPosList) {
                if (
                    ExpCollectorBlockEntity.POACHING_COLLECTOR_MAP.containsKey(level)
                    && ExpCollectorBlockEntity.POACHING_COLLECTOR_MAP.get(level).containsKey(chunkPos)
                ) {
                    List<ExpCollectorBlockEntity> list = ExpCollectorBlockEntity.POACHING_COLLECTOR_MAP.get(level).get(chunkPos);
                    list.remove(entity);
                }
            }
            entity.setRemoved();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState blockState, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(
            type, ModBlockEntities.EXP_COLLECTOR.get(),
            (world, pos, state, be) -> be.tick(world, pos)
        );
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return ModBlockEntities.EXP_COLLECTOR.create(blockPos, blockState);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWERED, OVERLOAD);
    }
}
