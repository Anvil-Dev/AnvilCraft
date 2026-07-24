package dev.dubhe.anvilcraft.block.container.storage;

import dev.anvilcraft.lib.v2.util.DistExecutor;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.api.itemhandler.unlimited.UnlimitedItemStacksResourceHandler;
import dev.dubhe.anvilcraft.block.entity.storage.CrateBlockEntity;
import dev.dubhe.anvilcraft.block.entity.storage.StorageBlockEntity;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
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
    public CrateBlock(Properties properties) {
        super(properties);
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
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof CrateBlockEntity be) {
            be.playerWillDestroy(level, pos, state, player);
        }

        return super.playerWillDestroy(level, pos, state, player);
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
                return CrateBlock.mergeIntoLargeCrate(level, pos, hitResult.getDirection(), itemStack, player);
            }
            if (player instanceof ServerPlayer) {
                return InteractionResult.SUCCESS_SERVER;
            } else if (level.isClientSide()) {
                DistExecutor.run(Dist.CLIENT, () -> () -> StorageScreen.openScreen(entity.getBlockPos()));
                return InteractionResult.SUCCESS;
            }
        }
        return super.useItemOn(itemStack, state, level, pos, player, hand, hitResult);
    }

    private static InteractionResult mergeIntoLargeCrate(
        Level level,
        BlockPos center,
        Direction clickedFace,
        ItemStack largeCrateStack,
        Player player
    ) {
        BlockPos origin = CrateBlock.getLargeCrateOrigin(center, clickedFace);
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
        try (Transaction root = Transaction.openRoot()) {
            try (Transaction transaction = Transaction.open(root)) {
                for (CrateBlockEntity crate : crates) {
                    UUID sourceId = crate.getId();
                    if (sourceId == null || !sourceIds.add(sourceId)) continue;
                    Optional<BaseStorage<?>> sourceOp = Storages.get().get(sourceId);
                    if (sourceOp.isEmpty()) continue;
                    BaseStorage<?> source = sourceOp.get();
                    UnlimitedItemStacksResourceHandler items = source.getItems();
                    for (int i = 0; i < items.size(); i++) {
                        long amountAsLong = items.getAmountAsLong(i);
                        if (amountAsLong <= 0) continue;
                        ItemResource resource = items.getResource(i);
                        int amount = Math.toIntExact(amountAsLong);
                        if (targetItems.insert(resource, amount, transaction) != amount) return InteractionResult.FAIL;
                    }
                }
                transaction.commit();
            }
            Storages.get().put(target);
            for (UUID sourceId : sourceIds) {
                Storages.get().remove(sourceId);
            }
            LargeCrateBlock largeCrate = ModBlocks.LARGE_CRATE.get();
            BlockState defaultState = largeCrate.defaultBlockState();
            for (Cube3x3PartHalf part : Cube3x3PartHalf.values()) {
                level.setBlockAndUpdate(origin.offset(part.getOffset()), defaultState.setValue(LargeCrateBlock.HALF, part));
            }
            root.commit();
        }
        if (level.getBlockEntity(origin) instanceof StorageBlockEntity storage) storage.setId(target.getId());
        if (!player.hasInfiniteMaterials()) largeCrateStack.shrink(1);
        return InteractionResult.SUCCESS_SERVER;
    }

    private static BlockPos getLargeCrateOrigin(BlockPos center, Direction clickedFace) {
        return switch (clickedFace.getAxis()) {
            case X, Z -> center.relative(clickedFace.getOpposite()).below();
            case Y -> center.offset(0, clickedFace == Direction.UP ? -2 : 0, 0);
        };
    }
}
