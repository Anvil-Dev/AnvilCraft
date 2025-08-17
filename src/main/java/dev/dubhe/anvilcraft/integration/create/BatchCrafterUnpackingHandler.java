package dev.dubhe.anvilcraft.integration.create;

import com.simibubi.create.api.packager.unpacking.UnpackingHandler;
import com.simibubi.create.api.registry.SimpleRegistry;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import dev.dubhe.anvilcraft.api.itemhandler.PollableFilteredItemStackHandler;
import dev.dubhe.anvilcraft.block.entity.BatchCrafterBlockEntity;
import dev.dubhe.anvilcraft.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public class BatchCrafterUnpackingHandler implements UnpackingHandler {
    public static final UnpackingHandler DEFAULT = (level, pos, state, side, items, orderContext, simulate) -> {
        BlockEntity targetBE = level.getBlockEntity(pos);
        if (!(targetBE instanceof BatchCrafterBlockEntity batchCrafter)) {
            return false;
        }
        PollableFilteredItemStackHandler itemHandler = batchCrafter.getItemHandler();
        List<ItemStack> copyItems = items.stream().map(ItemStack::copy).toList();
        if (simulate) return itemHandler.canCompletelyInsert(items);
        outer:
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            for (ItemStack item : copyItems) {
                if (item.isEmpty()) continue;
                ItemStack stack = itemHandler.insertItemNoPolling(i, item.copy(), false);
                item.setCount(stack.getCount());
                if (!stack.isEmpty()) continue outer;
            }
        }
        if (copyItems.stream().anyMatch(stack -> !stack.isEmpty())) {
            return false;
        }
        for (int i = 0; i < copyItems.size(); i++) {
            ItemStack copy = copyItems.get(i);
            ItemStack item = items.get(i);
            item.setCount(copy.getCount());
        }
        return true;
    };
    public static final BatchCrafterUnpackingHandler INSTANCE = new BatchCrafterUnpackingHandler();

    @Override
    public boolean unpack(
        Level level,
        BlockPos pos,
        BlockState state,
        Direction side,
        List<ItemStack> items,
        @Nullable PackageOrderWithCrafts orderContext,
        boolean simulate
    ) {
        if (!PackageOrderWithCrafts.hasCraftingInformation(orderContext)) {
            return BatchCrafterUnpackingHandler.DEFAULT.unpack(level, pos, state, side, items, null, simulate);
        }
        BlockEntity targetBE = level.getBlockEntity(pos);
        if (targetBE instanceof BatchCrafterBlockEntity batchCrafter) {
            PollableFilteredItemStackHandler itemHandler = batchCrafter.getItemHandler();
            // 有物品或者过滤开了就不接受包裹物品
            if (itemHandler.isFilterEnabled() || !itemHandler.isEmpty()) {
                return false;
            }
            List<BigItemStack> craftingContext = orderContext.getCraftingInformation();
            long inputCount = items.stream().filter(stack -> !stack.isEmpty()).count();
            long needCount = craftingContext.stream().filter(stack -> !stack.stack.isEmpty()).count();
            if (inputCount != needCount) {
                return BatchCrafterUnpackingHandler.DEFAULT.unpack(level, pos, state, side, items, null, simulate);
            }
            int max = Math.min(itemHandler.getSlots(), craftingContext.size());
            while (true) {
                boolean allInsertFailed = true;
                outer:
                for (int i = 0; i < max; i++) {
                    BigItemStack targetStack = craftingContext.get(i);
                    if (targetStack.stack.isEmpty()) continue;

                    // go through each item in the box and try insert if it matches the target
                    for (ItemStack stack : items) {
                        if (ItemStack.isSameItemSameComponents(stack, targetStack.stack)) {
                            ItemStack toInsert = stack.copyWithCount(targetStack.count);
                            if (itemHandler.insertItemNoPolling(i, toInsert, simulate).isEmpty()) {
                                stack.shrink(targetStack.count);
                                allInsertFailed = false;
                                continue outer;
                            }
                        }
                    }
                }
                if (allInsertFailed) {
                    if (!simulate) {
                        batchCrafter.craft(level);
                    }
                }
                boolean finished = true;
                for (ItemStack item : items) {
                    if (!item.isEmpty()) {
                        finished = false;
                        break;
                    }
                }
                if (finished) {
                    break;
                }
            }
            if (!simulate) {
                batchCrafter.craft(level);
            }

            return true;
        }
        return false;
    }

    public static class Provider implements SimpleRegistry.Provider<Block, UnpackingHandler> {

        @Override
        public @Nullable UnpackingHandler get(Block object) {
            if (object == ModBlocks.BATCH_CRAFTER.get()) {
                return INSTANCE;
            }
            return null;
        }
    }
}
