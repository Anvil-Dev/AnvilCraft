package dev.dubhe.anvilcraft.block;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.api.block.IDamagingHeater;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.entity.BurningHeaterBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.util.HeaterUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

public class BurningHeaterBlock extends BaseEntityBlock implements IHammerRemovable, IDamagingHeater {
    /**
     * 燃烧等级：0=熄灭，1=阴燃(0-300s)，2=点燃(≥300s)
     */
    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, 2);

    public BurningHeaterBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(LEVEL, 0));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(BurningHeaterBlock::new);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BurningHeaterBlockEntity(ModBlockEntities.BURNING_HEATER.get(), pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LEVEL);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.BURNING_HEATER.get(),
            (level1, pos, state1, entity) -> entity.tick(level1, pos, state1));
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
        if (hand != InteractionHand.MAIN_HAND) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof BurningHeaterBlockEntity be)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!(be.getItemHandler() instanceof ItemStackHandler handler)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        ItemStack held = player.getMainHandItem();
        ItemStack current = handler.getStackInSlot(0);
        boolean doubleClick = be.isDoubleClick(player);

        if (!held.isEmpty() && BurningHeaterBlockEntity.getItemBurnTime(held) > 0) {
            // 主手持燃料：单击放入 1 个，双击放入全部
            int insertCount = doubleClick ? held.getCount() : 1;
            ItemStack remaining = handler.insertItem(0, held.copyWithCount(insertCount), false);
            int inserted = insertCount - remaining.getCount();
            if (inserted > 0) {
                held.shrink(inserted);
                player.setItemInHand(InteractionHand.MAIN_HAND, held);
                level.sendBlockUpdated(pos, state, state, 3);
                return ItemInteractionResult.SUCCESS;
            }
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (held.isEmpty() && !current.isEmpty() && !doubleClick) {
            // 空手右键：取出所有物品
            player.setItemInHand(InteractionHand.MAIN_HAND, handler.extractItem(0, current.getMaxStackSize(), false));
            level.sendBlockUpdated(pos, state, state, 3);
            return ItemInteractionResult.SUCCESS;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof BurningHeaterBlockEntity be) {
                var handler = be.getItemHandler();
                for (int i = 0; i < handler.getSlots(); i++) {
                    Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, handler.getStackInSlot(i));
                }
                level.updateNeighbourForOutputSignal(pos, this);
            }
            super.onRemove(state, level, pos, newState, movedByPiston);
        }
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState blockState) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState blockState, Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof BurningHeaterBlockEntity be) {
            return Math.min(15, (be.getBurnTime() * 15) / BurningHeaterBlockEntity.REFUEL_THRESHOLD);
        }
        return 0;
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        HeaterUtil.hurtEntity(level, state, entity);
        super.stepOn(level, pos, state, entity);
    }

    @Override
    public boolean isActive(BlockState state) {
        return state.getValue(LEVEL) > 0;
    }
}