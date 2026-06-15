package dev.dubhe.anvilcraft.block.power.generator;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.api.hammer.IHammerChangeable;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.api.itemhandler.FilteredItemStackHandler;
import dev.dubhe.anvilcraft.api.power.IPowerComponent;
import dev.dubhe.anvilcraft.block.entity.ChargerBlockEntity;
import dev.dubhe.anvilcraft.block.entity.DischargerBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.util.IStateListener;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

public class ChargerBlock extends BaseEntityBlock implements IHammerRemovable, IHammerChangeable {
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty OVERLOAD = IPowerComponent.OVERLOAD;

    public ChargerBlock(Properties properties) {
        super(properties);
        registerDefaultState(getStateDefinition().any().setValue(POWERED, false).setValue(OVERLOAD, true));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(ChargerBlock::new);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(POWERED, false).setValue(OVERLOAD, true);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
        Level level,
        BlockState state,
        BlockEntityType<T> type
    ) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(
            type,
            ModBlockEntities.CHARGER.get(),
            (level1, blockPos, _, be) -> be.tick(level1, blockPos)
        );
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
        if (level.isClientSide()) return;
        level.setBlock(pos, state.setValue(POWERED, level.hasNeighborSignal(pos)), 2);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ChargerBlockEntity(ModBlockEntities.CHARGER.get(), pos, state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWERED).add(OVERLOAD);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        level.updateNeighbourForOutputSignal(pos, this);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(POWERED) && !level.hasNeighborSignal(pos)) {
            level.setBlock(pos, state.cycle(POWERED), 2);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean change(Player player, BlockPos blockPos, Level level, ItemStack anvilHammer) {
        level.setBlock(blockPos, ModBlocks.DISCHARGER.getDefaultState(), 2);
        if (level.getBlockEntity(blockPos) instanceof IStateListener<?> listener) {
            IStateListener<Boolean> self = (IStateListener<Boolean>) listener;
            self.notifyStateChanged(false);
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
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof ChargerBlockEntity charger) return charger.getAnalogRedstoneSignal();
        if (blockEntity instanceof DischargerBlockEntity discharger) return discharger.getAnalogRedstoneSignal();
        return 0;
    }

    @Override
    protected InteractionResult useItemOn(
        ItemStack stack,
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        BlockHitResult hit
    ) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof ChargerBlockEntity) && !(be instanceof DischargerBlockEntity)) {
            return InteractionResult.PASS;
        }
        FilteredItemStackHandler handler = be instanceof ChargerBlockEntity charger
            ? charger.getFilteredItemStackHandler()
            : ((DischargerBlockEntity) be).getFilteredItemStackHandler();

        if (stack.isEmpty()) {
            return tryExtract(player, level, pos, handler, be);
        }

        if (!handler.getStacks().get(0).isEmpty()) return InteractionResult.PASS;

        ItemResource resource = ItemResource.of(stack);
        if (be instanceof ChargerBlockEntity charger && !charger.containsValidItem(resource)) return InteractionResult.PASS;
        if (be instanceof DischargerBlockEntity discharger && !discharger.containsValidItem(resource)) return InteractionResult.PASS;

        try (Transaction tx = Transaction.openRoot()) {
            int inserted = handler.insert(0, resource, 1, tx);
            if (inserted == 0) return InteractionResult.PASS;
            tx.commit();
            stack.shrink(inserted);
            return InteractionResult.CONSUME;
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof ChargerBlockEntity) && !(be instanceof DischargerBlockEntity)) {
            return InteractionResult.PASS;
        }
        FilteredItemStackHandler handler = be instanceof ChargerBlockEntity charger
            ? charger.getFilteredItemStackHandler()
            : ((DischargerBlockEntity) be).getFilteredItemStackHandler();

        return tryExtract(player, level, pos, handler, be);
    }

    private static InteractionResult tryExtract(
        Player player, Level level, BlockPos pos, FilteredItemStackHandler handler, BlockEntity be) {
        for (int slot : new int[]{2, 1, 0}) {
            ItemStack stack = handler.getStacks().get(slot);
            if (stack.isEmpty()) continue;
            try (Transaction tx = Transaction.openRoot()) {
                final ItemResource resourceIn = handler.getResource(slot);
                int count = handler.getAmountAsInt(slot);
                if (count <= 0) continue;
                handler.set(slot, ItemResource.EMPTY, 0);
                tx.commit();
                // 只有从加工槽(slot 1)取物才中断加工，从输出槽取成品不影响加工
                if (slot == 1) {
                    if (be instanceof ChargerBlockEntity charger) charger.stopProcessing();
                    if (be instanceof DischargerBlockEntity discharger) discharger.stopProcessing();
                }
                player.getInventory().placeItemBackInInventory(resourceIn.toStack(count));
                level.playSound(null, pos, SoundEvents.ITEM_PICKUP,
                    SoundSource.PLAYERS, .2F, 1F + level.getRandom().nextFloat());
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }
}
