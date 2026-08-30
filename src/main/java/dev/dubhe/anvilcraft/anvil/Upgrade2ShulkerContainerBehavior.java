package dev.dubhe.anvilcraft.anvil;

import dev.anvilcraft.lib.v2.util.stack.UnlimitedItemStack;
import dev.dubhe.anvilcraft.api.anvil.IAnvilBehavior;
import dev.dubhe.anvilcraft.api.event.AnvilEvent;
import dev.dubhe.anvilcraft.api.itemhandler.unlimited.TypeLimitItemStacksResourceHandler;
import dev.dubhe.anvilcraft.api.itemhandler.unlimited.UnlimitedItemStacksResourceHandler;
import dev.dubhe.anvilcraft.block.container.storage.ShulkerContainerBlock;
import dev.dubhe.anvilcraft.block.entity.storage.LargeCrateBlockEntity;
import dev.dubhe.anvilcraft.block.entity.storage.ShulkerContainerBlockEntity;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import dev.dubhe.anvilcraft.block.state.OpenedCube3x3PartHalf;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.saved.storage.BaseStorage;
import dev.dubhe.anvilcraft.saved.storage.ShulkerContainerStorage;
import dev.dubhe.anvilcraft.saved.storage.Storages;
import dev.dubhe.anvilcraft.util.AabbUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class Upgrade2ShulkerContainerBehavior implements IAnvilBehavior {
    @Override
    public boolean handle(Level level, BlockPos hitBlockPos, BlockState hitBlockState, float fallDistance, AnvilEvent.OnLand event) {
        if (!hitBlockState.is(ModBlocks.LARGE_CRATE)) {
            return false;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }

        List<ItemEntity> entities = serverLevel.getEntitiesOfClass(ItemEntity.class, AabbUtil.create(hitBlockPos, hitBlockPos.above()));
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
        // 只消耗 6 个下界合金块，多余的保留
        count = 6;

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
        BlockEntity blockEntity = serverLevel.getBlockEntity(mainPart);
        if (!(blockEntity instanceof LargeCrateBlockEntity be)) {
            return true;
        }

        for (Cube3x3PartHalf half : Cube3x3PartHalf.values()) {
            serverLevel.setBlock(mainPart.offset(half.getOffset()), Blocks.AIR.defaultBlockState(), Block.UPDATE_MOVE_BY_PISTON);
        }

        serverLevel.setBlock(
            mainPart,
            ModBlocks.SHULKER_CONTAINER.get().defaultBlockState().setValue(ShulkerContainerBlock.HALF, OpenedCube3x3PartHalf.BOTTOM_CENTER),
            Block.UPDATE_CLIENTS
        );
        BlockState placedState = serverLevel.getBlockState(mainPart);
        placedState.getBlock().setPlacedBy(serverLevel, mainPart, placedState, null, ItemStack.EMPTY);
        if (be.getId() == null) {
            return true;
        }
        Optional<BaseStorage<?>> storageOp = Storages.get().get(be.getId());
        if (storageOp.isEmpty()) {
            return true;
        }
        BaseStorage<?> storage = storageOp.get();

        BlockEntity scBlockEntity = serverLevel.getBlockEntity(mainPart);
        if (!(scBlockEntity instanceof ShulkerContainerBlockEntity scBe)) {
            return true;
        }
        UUID id = scBe.getId();
        if (id == null) {
            id = UUID.randomUUID();
            scBe.setId(id);
        }
        ShulkerContainerStorage sc = Storages.get().getOrCreate(id, ShulkerContainerStorage.class);
        TypeLimitItemStacksResourceHandler scItems = sc.getItems();
        UnlimitedItemStacksResourceHandler items = storage.getItems();
        for (int i = 0; i < items.size(); i++) {
            UnlimitedItemStack unlimitedStack = items.getUnlimitedStackInSlot(i);
            if (unlimitedStack.isEmpty()) continue;
            ItemStack toInsert = unlimitedStack.toStack();
            ItemStack leftover = scItems.insertItem(toInsert, false);
            if (!leftover.isEmpty()) {
                int remaining = leftover.getCount();
                int maxStack = leftover.getMaxStackSize();
                while (remaining > 0) {
                    int dropCount = Math.min(maxStack, remaining);
                    ItemStack dropped = leftover.copyWithCount(dropCount);
                    Block.popResource(serverLevel, mainPart.above(3), dropped);
                    remaining -= dropCount;
                }
            }
        }
        Storages.get().remove(storage.getId());

        return true;
    }
}
