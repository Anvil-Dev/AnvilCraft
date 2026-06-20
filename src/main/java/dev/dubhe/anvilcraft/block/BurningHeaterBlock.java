package dev.dubhe.anvilcraft.block;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.entity.BurningHeaterBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.entity.ModDamageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
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

public class BurningHeaterBlock extends BaseEntityBlock implements IHammerRemovable {
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
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof BurningHeaterBlockEntity be)) return InteractionResult.PASS;
        if (!(be.getItemHandler() instanceof ItemStackHandler handler)) return InteractionResult.PASS;

        ItemStack held = player.getMainHandItem();
        ItemStack current = handler.getStackInSlot(0);

        if (!held.isEmpty() && BurningHeaterBlockEntity.getItemBurnTime(held) > 0) {
            ItemStack remaining = handler.insertItem(0, held, false);
            if (remaining.getCount() != held.getCount()) {
                player.setItemInHand(player.getUsedItemHand(), remaining);
                return InteractionResult.CONSUME;
            }
        } else if (held.isEmpty() && !current.isEmpty()) {
            player.setItemInHand(player.getUsedItemHand(), handler.extractItem(0, current.getMaxStackSize(), false));
            return InteractionResult.CONSUME;
        }

        return InteractionResult.PASS;
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
            return (be.getBurnTime() * 15) / BurningHeaterBlockEntity.MAX_BURN_TIME;
        }
        return 0;
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (state.getValue(LEVEL) > 0
            && !entity.isSteppingCarefully()
            && entity instanceof LivingEntity) {
            entity.hurt(ModDamageTypes.heaterBurn(level), 4.0F);
        }
        super.stepOn(level, pos, state, entity);
    }
}
