package dev.dubhe.anvilcraft.anvil;

import dev.dubhe.anvilcraft.api.anvil.IAnvilBehavior;
import dev.dubhe.anvilcraft.api.event.AnvilEvent;
import dev.dubhe.anvilcraft.api.itemhandler.unlimited.TypeLimitItemStacksResourceHandler;
import dev.dubhe.anvilcraft.api.itemhandler.unlimited.UnlimitedItemStacksResourceHandler;
import dev.dubhe.anvilcraft.block.container.storage.ShulkerContainerBlock;
import dev.dubhe.anvilcraft.block.entity.storage.LargeCrateBlockEntity;
import dev.dubhe.anvilcraft.block.entity.storage.ShulkerContainerBlockEntity;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import dev.dubhe.anvilcraft.block.state.OpenedCube3x3PartHalf;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.saved.storage.BaseStorage;
import dev.dubhe.anvilcraft.saved.storage.ShulkerContainerStorage;
import dev.dubhe.anvilcraft.saved.storage.Storages;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class Upgrade2ShulkerContainerBehavior implements IAnvilBehavior {
    @Override
    public boolean handle(ServerLevel level, BlockPos hitBlockPos, BlockState hitBlockState, double fallDistance, AnvilEvent.OnLand event) {
        if (!hitBlockState.is(ModBlocks.LARGE_CRATE)) {
            return false;
        }

        List<ItemEntity> entities = level.getEntitiesOfClass(ItemEntity.class, new AABB(hitBlockPos.above()));
        ItemEntity spaceOvercompressor = null;
        List<ItemEntity> netheriteBlock = new ArrayList<>();
        int count = 0;
        for (ItemEntity entity : entities) {
            ItemStack stack = entity.getItem();
            if (stack.is(ModBlocks.SPACE_OVERCOMPRESSOR.asItem())) {
                spaceOvercompressor = entity;
            } else if (stack.is(Blocks.NETHERITE_BLOCK.asItem())) {
                netheriteBlock.add(entity);
                count += stack.getCount();
            }
        }
        if (spaceOvercompressor == null || netheriteBlock.isEmpty() || count < 6) {
            return false;
        }

        ItemStack stack = spaceOvercompressor.getItem();
        stack.shrink(1);
        if (stack.getCount() <= 0) {
            spaceOvercompressor.discard();
        } else {
            spaceOvercompressor.setItem(stack);
        }

        for (ItemEntity entity : netheriteBlock) {
            stack = entity.getItem();
            int shrink = Math.min(count, stack.getCount());
            count -= shrink;
            stack.shrink(shrink);
            if (stack.getCount() <= 0) {
                entity.discard();
            } else {
                entity.setItem(stack);
            }
            if (count == 0) break;
        }

        BlockPos mainPart = ModBlocks.LARGE_CRATE.get().getMainPartPos(hitBlockPos, hitBlockState);
        final Optional<LargeCrateBlockEntity> beOp = level.getBlockEntity(mainPart, ModBlockEntities.LARGE_CRATE.get());

        for (Cube3x3PartHalf half : Cube3x3PartHalf.values()) {
            level.setBlock(mainPart.offset(half.getOffset()), Blocks.AIR.defaultBlockState(), Block.UPDATE_NONE);
        }

        level.setBlock(
            mainPart,
            ModBlocks.SHULKER_CONTAINER.getDefaultState().setValue(ShulkerContainerBlock.HALF, OpenedCube3x3PartHalf.BOTTOM_CENTER),
            Block.UPDATE_CLIENTS
        );
        level.getBlockState(mainPart).getBlock().setPlacedBy(level, mainPart, level.getBlockState(mainPart), null, ItemStack.EMPTY);

        if (beOp.isEmpty()) {
            return true;
        }
        LargeCrateBlockEntity be = beOp.get();
        if (be.getId() == null) {
            return true;
        }
        Optional<BaseStorage<?>> storageOp = Storages.get().get(be.getId());
        if (storageOp.isEmpty()) {
            return true;
        }
        BaseStorage<?> storage = storageOp.get();

        Optional<ShulkerContainerBlockEntity> scBeOp = level.getBlockEntity(mainPart, ModBlockEntities.SHULKER_CONTAINER.get());
        if (scBeOp.isEmpty()) {
            return true;
        }
        ShulkerContainerBlockEntity scBe = scBeOp.get();
        UUID id = scBe.getId();
        if (id == null) {
            id = UUID.randomUUID();
            scBe.setId(id);
        }
        ShulkerContainerStorage sc = Storages.get().getOrCreate(id, ShulkerContainerStorage.class);
        TypeLimitItemStacksResourceHandler scItems = sc.getItems();
        try (Transaction root = Transaction.openRoot()) {
            UnlimitedItemStacksResourceHandler items = storage.getItems();
            for (int i = 0; i < items.size(); i++) {
                try (Transaction transaction = Transaction.open(root)) {
                    long amountAsLong = items.getAmountAsLong(i);
                    if (amountAsLong <= 0) continue;
                    ItemResource resource = items.getResource(i);
                    int amount = Math.toIntExact(amountAsLong);
                    int inserted = scItems.insert(resource, amount, transaction);
                    if (inserted <= amount) {
                        int diff = amount - inserted;
                        for (int j = 0; j < diff; j -= Math.min(resource.getMaxStackSize(), j)) {
                            Block.popResource(level, mainPart.above(3), resource.toStack(j));
                        }
                    }
                    transaction.commit();
                }
            }
            root.commit();
        }
        Storages.get().remove(storage.getId());

        return true;
    }
}
