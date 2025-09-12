package dev.dubhe.anvilcraft.block;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.api.hammer.IHammerChangeable;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.api.itemhandler.FilteredItemStackHandler;
import dev.dubhe.anvilcraft.api.power.IPowerComponent;
import dev.dubhe.anvilcraft.block.entity.ChargerBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.util.IStateListener;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
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
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 充电器
 */
public class ChargerBlock extends BaseEntityBlock implements IHammerRemovable, IHammerChangeable {
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty OVERLOAD = IPowerComponent.OVERLOAD;

    /**
     * 充电器
     */
    public ChargerBlock(Properties properties) {
        super(properties);
        registerDefaultState(getStateDefinition().any().setValue(POWERED, false).setValue(OVERLOAD, true));
    }

    @Override
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(ChargerBlock::new);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
        return defaultBlockState().setValue(POWERED, false).setValue(OVERLOAD, true);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
        @NotNull Level level,
        @NotNull BlockState state,
        @NotNull BlockEntityType<T> type
    ) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(
            type,
            ModBlockEntities.CHARGER.get(),
            (level1, blockPos, blockState, blockEntity) -> blockEntity.tick(level1, blockPos)
        );
    }

    @Override
    public void neighborChanged(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull Block neighborBlock,
        @NotNull BlockPos neighborPos,
        boolean movedByPiston
    ) {
        if (level.isClientSide) {
            return;
        }
        level.setBlock(pos, state.setValue(POWERED, level.hasNeighborSignal(pos)), 2);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new ChargerBlockEntity(ModBlockEntities.CHARGER.get(), pos, state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWERED).add(OVERLOAD);
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void onRemove(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull BlockState newState,
        boolean movedByPiston
    ) {
        if (state.is(newState.getBlock())) return;
        if (level.getBlockEntity(pos) instanceof ChargerBlockEntity entity) {
            Vec3 vec3 = entity.getBlockPos().getCenter();
            FilteredItemStackHandler depository = entity.getFilteredItemStackHandler();
            for (int slot = 0; slot < depository.getSlots(); slot++) {
                Containers.dropItemStack(level, vec3.x, vec3.y, vec3.z, depository.getStackInSlot(slot));
            }
            level.updateNeighbourForOutputSignal(pos, this);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public void tick(@NotNull BlockState state, @NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull RandomSource random) {
        if (state.getValue(POWERED) && !level.hasNeighborSignal(pos)) {
            level.setBlock(pos, state.cycle(POWERED), 2);
        }
    }

    @Override
    public boolean change(Player player, BlockPos blockPos, @NotNull Level level, ItemStack anvilHammer) {
        level.setBlock(blockPos, ModBlocks.DISCHARGER.getDefaultState(), 2);
        if (level.getBlockEntity(blockPos) instanceof IStateListener<?> listener) {
            IStateListener<Boolean> thiz = (IStateListener<Boolean>) listener;
            thiz.notifyStateChanged(false);
        }
        return true;
    }

    @Override
    public @Nullable Property<?> getChangeableProperty(BlockState blockState) {
        return null;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity instanceof ChargerBlockEntity charger ? charger.getAnalogRedstoneSignal() : 0;
    }

    @Override
    protected ItemInteractionResult useItemOn(
        ItemStack stack,
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        BlockHitResult hit
    ) {
        if (level.getBlockEntity(pos) instanceof ChargerBlockEntity charger) {
            if (level.isClientSide) {
                return ItemInteractionResult.SUCCESS;
            }
            // 玩家空手时尝试取出物品
            if (stack.isEmpty()) {
                // 优先从输出槽（槽位2）取物品，如果为空则从输入槽（槽位0）取
                for (int slot : new int[]{
                    2,
                    0
                }) {
                    ItemStack itemInSlot = charger.getFilteredItemStackHandler().getStackInSlot(slot);
                    if (!itemInSlot.isEmpty()) {
                        ItemStack extracted = charger.getFilteredItemStackHandler().extractItem(slot, itemInSlot.getCount(), false);
                        player.getInventory().placeItemBackInInventory(extracted);
                        level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, .2f, 1f + level.getRandom().nextFloat());
                        return ItemInteractionResult.SUCCESS;
                    }
                }
            } else if (charger.containsValidItem(stack)) {
                ItemStack result = charger.getFilteredItemStackHandler().insertItem(0, stack, true);
                if (result.isEmpty() || result.getCount() < stack.getCount()) {
                    int countDiff = stack.getCount() - (result.isEmpty() ? 0 : result.getCount());
                    ItemStack toInsert = stack.split(countDiff);
                    charger.getFilteredItemStackHandler().insertItem(0, toInsert, false);
                    return ItemInteractionResult.SUCCESS;
                }
            }
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
}
