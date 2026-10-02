package dev.dubhe.anvilcraft.block.container.storage;

import dev.anvilcraft.lib.v2.util.DistExecutor;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.api.itemhandler.unlimited.UnlimitedItemStacksResourceHandler;
import dev.dubhe.anvilcraft.block.entity.storage.CrateBlockEntity;
import dev.dubhe.anvilcraft.block.entity.storage.StorageBlockEntity;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import dev.dubhe.anvilcraft.block.storage.VoidMatterBlock;
import dev.dubhe.anvilcraft.client.gui.screen.StorageScreen;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.StorageRef;
import dev.dubhe.anvilcraft.saved.storage.BaseStorage;
import dev.dubhe.anvilcraft.saved.storage.LargeCrateStorage;
import dev.dubhe.anvilcraft.saved.storage.StorageType;
import dev.dubhe.anvilcraft.saved.storage.Storages;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class CrateBlock extends Block implements EntityBlock, IHammerRemovable {
    public static final BooleanProperty DISPOSE = BooleanProperty.create("dispose");

    public CrateBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(DISPOSE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DISPOSE);
    }

    public static boolean hasAdjacentVoidMatter(LevelReader level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (level.getBlockState(pos.relative(direction)).getBlock() instanceof VoidMatterBlock) return true;
        }
        return false;
    }

    public static void updateDisposeState(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (level.isClientSide() || !(state.getBlock() instanceof CrateBlock)) return;
        boolean dispose = hasAdjacentVoidMatter(level, pos);
        if (state.getValue(DISPOSE) != dispose) {
            level.setBlock(pos, state.setValue(DISPOSE, dispose), Block.UPDATE_CLIENTS);
            if (level.getBlockEntity(pos) instanceof CrateBlockEntity crate) crate.refreshDispose();
        }
    }

    @Override
    protected void neighborChanged(
        BlockState state, Level level, BlockPos pos, Block neighbor, @Nullable Orientation orientation, boolean isMoving
    ) {
        super.neighborChanged(state, level, pos, neighbor, orientation, isMoving);
        updateDisposeState(level, pos);
    }

    public static Component displayName(BlockState state) {
        return state.getValue(DISPOSE) ? Component.translatable("block.anvilcraft.overflow_disposal_crate") : state.getBlock().getName();
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData, Player player) {
        ItemStack stack = super.getCloneItemStack(level, pos, state, includeData, player);
        StorageBlockEntity.applyPickStorageId(stack, level, pos, state, includeData);
        return stack;
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof StorageBlockEntity storage ? storage.getComparatorSignal() : 0;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.CRATE.create(pos, state);
    }

    public static List<CrateBlockEntity> getNearbyCrates(Level level, BlockPos sourcePos) {
        List<CrateBlockEntity> crates = new ArrayList<>();
        CrateBlockEntity source = null;
        for (BlockPos pos : BlockPos.betweenClosed(sourcePos.offset(-1, -1, -1), sourcePos.offset(1, 1, 1))) {
            if (!(level.getBlockEntity(pos) instanceof CrateBlockEntity crate)) {
                continue;
            }
            if (pos.equals(sourcePos)) {
                source = crate;
            } else {
                crates.add(crate);
            }
        }
        if (source != null) {
            crates.add(source);
        }
        return crates;
    }

    @Override
    protected InteractionResult useItemOn(
        ItemStack itemStack,
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        BlockHitResult hitResult
    ) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof CrateBlockEntity entity) {
            if (player.isSpectator()) return InteractionResult.PASS;
            if (player.isShiftKeyDown() && itemStack.is(ModBlocks.LARGE_CRATE.asItem())) {
                if (level.isClientSide()) return InteractionResult.SUCCESS;
                return CrateBlock.mergeIntoLargeCrate(level, pos, itemStack, player);
            }
            if (player instanceof ServerPlayer) {
                return InteractionResult.SUCCESS_SERVER;
            } else if (level.isClientSide()) {
                level.playSound(player, pos, SoundEvents.BARREL_OPEN, SoundSource.BLOCKS, 1.0F, 1.0F);
                DistExecutor.run(Dist.CLIENT, () -> () -> StorageScreen.openScreen(entity.getBlockPos(), displayName(state)));
                return InteractionResult.SUCCESS;
            }
        }
        return super.useItemOn(itemStack, state, level, pos, player, hand, hitResult);
    }

    private static InteractionResult mergeIntoLargeCrate(
        Level level,
        BlockPos center,
        ItemStack largeCrateStack,
        Player player
    ) {
        BlockPos origin = CrateBlock.findLargeCrateOrigin(level, center);
        if (origin == null) return InteractionResult.FAIL;
        List<CrateBlockEntity> crates = new ArrayList<>();
        for (Cube3x3PartHalf part : Cube3x3PartHalf.values()) {
            BlockPos pos = origin.offset(part.getOffset());
            if (!(level.getBlockEntity(pos) instanceof CrateBlockEntity crate)) return InteractionResult.FAIL;
            crates.add(crate);
        }
        StorageRef ref = largeCrateStack.get(ModComponents.STORAGE);
        UUID targetId = ref != null && ref.type() == StorageType.LARGE_CRATE
            ? ref.id().orElseGet(UUID::randomUUID)
            : UUID.randomUUID();
        BaseStorage<?> target = Storages.get().get(targetId, LargeCrateStorage.class)
            .map(BaseStorage.class::cast)
            .orElseGet(() -> new LargeCrateStorage(targetId));
        UnlimitedItemStacksResourceHandler targetItems = target.getItems();
        Set<UUID> sourceIds = new HashSet<>();
        List<List<ItemStack>> recipeBases = new ArrayList<>();
        if (target.getRecipeBases() != null) recipeBases.add(target.getRecipeBases());
        List<ItemStack> overflow = new ArrayList<>();
        try (Transaction root = Transaction.openRoot()) {
            try (Transaction transaction = Transaction.open(root)) {
                for (CrateBlockEntity crate : crates) {
                    UUID sourceId = crate.getId();
                    if (sourceId == null || !sourceIds.add(sourceId)) continue;
                    Optional<BaseStorage<?>> sourceOp = Storages.get().get(sourceId);
                    if (sourceOp.isEmpty()) continue;
                    BaseStorage<?> source = sourceOp.get();
                    if (source.getRecipeBases() != null) recipeBases.add(source.getRecipeBases());
                    UnlimitedItemStacksResourceHandler items = source.getItems();
                    for (int i = 0; i < items.size(); i++) {
                        long amountAsLong = items.getAmountAsLong(i);
                        if (amountAsLong <= 0) continue;
                        ItemResource resource = items.getResource(i);
                        int amount = Math.toIntExact(amountAsLong);
                        CrateBlock.insertOrCollectOverflow(targetItems, resource, amount, transaction, overflow);
                    }
                }
                CrateBlock.insertOrCollectOverflow(targetItems, ItemResource.of(ModBlocks.CRATE.asItem()), 27, transaction, overflow);
                for (int index = 1; index < recipeBases.size(); index++) {
                    for (ItemStack base : recipeBases.get(index)) {
                        CrateBlock.insertOrCollectOverflow(targetItems, ItemResource.of(base), base.getCount(), transaction, overflow);
                    }
                }
                transaction.commit();
            }
            Storages.get().put(target);
            for (UUID sourceId : sourceIds) {
                Storages.get().remove(sourceId);
            }
            LargeCrateBlock largeCrate = ModBlocks.LARGE_CRATE.get();
            for (Cube3x3PartHalf part : Cube3x3PartHalf.values()) {
                level.setBlock(origin.offset(part.getOffset()), Blocks.AIR.defaultBlockState(), Block.UPDATE_NONE);
            }
            BlockState mainState = largeCrate.defaultBlockState().setValue(LargeCrateBlock.HALF, Cube3x3PartHalf.BOTTOM_CENTER);
            level.setBlock(origin, mainState, Block.UPDATE_CLIENTS);
            largeCrate.setPlacedBy(level, origin, mainState, player, ItemStack.EMPTY);
            root.commit();
        }
        if (!recipeBases.isEmpty()) target.setRecipeBases(recipeBases.getFirst());
        for (ItemStack stack : overflow) Block.popResourceFromFace(level, origin.above(), Direction.UP, stack);
        if (level.getBlockEntity(origin) instanceof StorageBlockEntity storage) storage.setId(target.getId());
        if (!player.hasInfiniteMaterials()) largeCrateStack.shrink(1);
        return InteractionResult.SUCCESS_SERVER;
    }

    private static void insertOrCollectOverflow(
        UnlimitedItemStacksResourceHandler items, ItemResource resource, int amount, Transaction transaction, List<ItemStack> overflow
    ) {
        int remaining = amount - items.insert(resource, amount, transaction);
        if (remaining > 0) overflow.add(resource.toStack(remaining));
    }

    private static @Nullable BlockPos findLargeCrateOrigin(Level level, BlockPos center) {
        Cube3x3PartHalf[] parts = Cube3x3PartHalf.values();
        for (int ox = -1; ox <= 1; ox++) {
            for (int oy = 0; oy <= 2; oy++) {
                for (int oz = -1; oz <= 1; oz++) {
                    BlockPos candidate = center.offset(ox, oy - 2, oz);
                    boolean matched = true;
                    for (Cube3x3PartHalf part : parts) {
                        if (!(level.getBlockEntity(candidate.offset(part.getOffset())) instanceof CrateBlockEntity)) {
                            matched = false;
                            break;
                        }
                    }
                    if (matched) return candidate;
                }
            }
        }
        return null;
    }
}
