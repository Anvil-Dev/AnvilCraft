package dev.dubhe.anvilcraft.api.itemhandler;

import com.google.common.collect.ImmutableList;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.OverLimitItemContainerContents;
import dev.dubhe.anvilcraft.util.AnvilUtil;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import javax.annotation.Nullable;

import static dev.dubhe.anvilcraft.block.BlockPlacerBlock.ORIENTATION;

public class ItemHandlerUtil {
    public static boolean exportToTarget(
        IItemHandler source,
        int maxAmountWeight,
        Predicate<ItemStack> predicate,
        @Nullable IItemHandler target
    ) {
        boolean success = false;
        ItemStack filterStack = null;
        boolean lockFilterItem = false;
        int maxAmount = maxAmountWeight;
        outerLoop:
        for (int srcIndex = 0; srcIndex < source.getSlots(); srcIndex++) {
            ItemStack sourceStack = source.extractItem(srcIndex, Integer.MAX_VALUE, true);
            if (sourceStack.isEmpty() || !predicate.test(sourceStack)) continue;
            if (filterStack == null) {
                filterStack = sourceStack.copy();
                maxAmount = (int) (maxAmountWeight / 64f * sourceStack.getMaxStackSize());
            } else if (!ItemStack.isSameItemSameComponents(filterStack, sourceStack)) {
                continue;
            }
            for (int i = 0; i < maxAmount; i++) {
                ItemStack remainder = ItemHandlerHelper.insertItem(target, sourceStack, true);
                int amountToInsert = sourceStack.getCount() - remainder.getCount();
                sourceStack = remainder;
                if (amountToInsert > 0) {
                    ItemStack extracted = source.extractItem(srcIndex, Math.min(maxAmount, amountToInsert), false);
                    ItemStack actualRemainder = ItemHandlerHelper.insertItem(target, extracted, false);
                    if (!actualRemainder.isEmpty()) {
                        source.insertItem(srcIndex, actualRemainder, false);
                        amountToInsert -= actualRemainder.getCount();
                    }
                    success = true;
                    lockFilterItem = true;
                    maxAmount -= amountToInsert;
                    if (maxAmount <= 0) break outerLoop;
                    if (remainder.getCount() == 0) break;
                } else {
                    if (!lockFilterItem) filterStack = null;
                    break;
                }
            }
        }
        return success;
    }

    public static boolean importFromTarget(
        IItemHandler target,
        int maxAmountWeight,
        Predicate<ItemStack> predicate,
        IItemHandler source
    ) {
        boolean success = false;
        ItemStack filterStack = null;
        boolean lockFilterItem = false;
        int maxAmount = maxAmountWeight;
        outerLoop:
        for (int srcIndex = 0; srcIndex < source.getSlots(); srcIndex++) {
            ItemStack sourceStack = source.extractItem(srcIndex, Integer.MAX_VALUE, true);
            if (sourceStack.isEmpty() || !predicate.test(sourceStack)) continue;
            if (filterStack == null) {
                filterStack = sourceStack.copy();
                maxAmount = (int) (maxAmountWeight / 64f * sourceStack.getMaxStackSize());
            } else if (!ItemStack.isSameItemSameComponents(filterStack, sourceStack)) {
                continue;
            }
            for (int i = 0; i < maxAmount; i++) {
                ItemStack remainder = ItemHandlerHelper.insertItem(target, sourceStack, true);
                int amountToInsert = sourceStack.getCount() - remainder.getCount();
                if (amountToInsert > 0) {
                    ItemStack extracted = source.extractItem(srcIndex, Math.min(maxAmount, amountToInsert), false);
                    ItemStack actualRemainder = ItemHandlerHelper.insertItem(target, extracted, false);
                    if (!actualRemainder.isEmpty()) {
                        source.insertItem(srcIndex, actualRemainder, false);
                        amountToInsert -= actualRemainder.getCount();
                    }
                    success = true;
                    lockFilterItem = true;
                    maxAmount -= amountToInsert;
                    if (maxAmount <= 0) break outerLoop;
                    if (remainder.getCount() == 0) break;
                } else {
                    if (!lockFilterItem) filterStack = null;
                    break;
                }
            }
        }
        return success;
    }

    public static void exportAllToTarget(IItemHandler source, Predicate<ItemStack> predicate, IItemHandler target) {
        for (int srcIndex = 0; srcIndex < source.getSlots(); srcIndex++) {
            ItemStack sourceStack = source.extractItem(srcIndex, Integer.MAX_VALUE, true);
            if (sourceStack.isEmpty() || !predicate.test(sourceStack)) continue;

            ItemStack remainder = ItemHandlerHelper.insertItem(target, sourceStack, true);

            int amountToInsert = sourceStack.getCount() - remainder.getCount();
            if (amountToInsert > 0) {
                sourceStack = source.extractItem(srcIndex, amountToInsert, false);
                ItemHandlerHelper.insertItem(target, sourceStack, false);
            }
        }
    }

    public static void exportContentsToItemHandlers(IItemHandler source, @Nullable List<IItemHandler> itemHandlerList) {
        if (itemHandlerList == null) return;
        for (IItemHandler target : itemHandlerList) {
            exportAllToTarget(source, stack -> true, target);
        }
    }

    public static void dropAllToPos(IItemHandler source, Level level, Vec3 pos) {
        List<ItemStack> items = new ArrayList<>();
        for (int slot = 0; slot < source.getSlots(); slot++) {
            ItemStack stack = source.extractItem(slot, Integer.MAX_VALUE, false);
            if (!stack.isEmpty()) items.add(stack);
        }
        AnvilUtil.dropItems(items, level, pos);
    }

    public static @Nullable IItemHandler getSourceItemHandler(BlockPos inputBlockPos, Direction context, @Nullable Level level) {
        if (level == null) return null;
        IItemHandler itemHandler = level.getCapability(
            Capabilities.ItemHandler.BLOCK,
            inputBlockPos,
            context
        );
        if (itemHandler != null) return itemHandler;
        AABB aabb = new AABB(inputBlockPos);
        List<ContainerEntity> entities = level.getEntitiesOfClass(
                Entity.class, aabb, e -> e instanceof ContainerEntity && !((ContainerEntity) e).isEmpty())
            .stream()
            .map(it -> (ContainerEntity) it)
            .toList();
        if (!entities.isEmpty()) {
            itemHandler = ((Entity) entities.getFirst()).getCapability(
                Capabilities.ItemHandler.ENTITY,
                null
            );
        }
        return itemHandler;
    }

    public static @Nullable List<IItemHandler> getTargetItemHandlerList(
        BlockPos inputBlockPos,
        @Nullable Direction context,
        @Nullable Level level
    ) {
        if (level == null) return null;
        List<IItemHandler> list = new ArrayList<>();
        IItemHandler input = level.getCapability(
            Capabilities.ItemHandler.BLOCK,
            inputBlockPos,
            context
        );
        if (input != null) {
            list.add(input);
            return list;
        }
        AABB aabb = new AABB(inputBlockPos);
        list = level.getEntitiesOfClass(
                Entity.class, aabb, e -> e instanceof ContainerEntity)
            .stream()
            .map(e -> e.getCapability(
                Capabilities.ItemHandler.ENTITY,
                null
            ))
            .toList();
        return list;
    }

    public static int countItemsInHandler(IItemHandler handler) {
        int count = 0;
        for (int i = 0; i < handler.getSlots(); i++) {
            count += handler.getStackInSlot(i).getCount();
        }
        return count;
    }

    public static Object2IntMap<Item> mergeHandlerItems(IItemHandler handler) {
        Object2IntMap<Item> items = new Object2IntOpenHashMap<>();
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            items.mergeInt(stack.getItem(), stack.getCount(), Integer::sum);
        }
        return items;
    }

    @Nullable
    public static IItemHandler getSourceItemHandlerRecursive(Block source, BlockPos inputPos, Direction context, @Nullable Level level) {
        int i = 0;
        do {
            if (level == null) return null;
            if (
                level.getBlockState(inputPos).is(source)
                && level.getBlockState(inputPos).getValue(ORIENTATION).getDirection() == context
            ) {
                i++;
                inputPos = inputPos.relative(context.getOpposite());
            } else {
                return getSourceItemHandler(inputPos, context, level);
            }
        } while (i < AnvilCraft.CONFIG.blockPlacerRecursiveRetrievalDistanceMax);
        return null;
    }

    public static ItemStack insertItem(@Nullable IItemHandler dest, ItemStack stack, boolean simulate) {
        if (dest == null || stack.isEmpty()) {
            return stack;
        }

        if (dest instanceof PollableFilteredItemStackHandler pollable) {
            for (int i = 0; i < dest.getSlots(); i++) {
                stack = pollable.insertItemNoPolling(i, stack, simulate);
                if (stack.isEmpty()) {
                    return ItemStack.EMPTY;
                }
            }

            return stack;
        } else {
            return ItemHandlerHelper.insertItem(dest, stack, simulate);
        }
    }

    public static boolean isEmptyContainer(@Nullable IItemHandler handler) {
        if (handler != null) {
            for (int i = 0; i < handler.getSlots(); i++) {
                if (!handler.getStackInSlot(i).isEmpty()) return true;
            }
        }
        return false;
    }

    public static boolean isEmptyContainer(ItemStack stack) {
        if (stack.has(ModComponents.OVER_LIMIT_CONTAINER)) {
            OverLimitItemContainerContents contents = stack.get(ModComponents.OVER_LIMIT_CONTAINER);
            return contents != null && contents != OverLimitItemContainerContents.EMPTY;
        }
        return ItemHandlerUtil.isEmptyContainer(stack.getCapability(Capabilities.ItemHandler.ITEM));
    }

    public static int hash(IItemHandler handler) {
        int hash = 0;
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            hash *= 31;
            hash += Item.getId(stack.getItem()) + stack.getDamageValue();
        }
        return hash;
    }

    public static @Unmodifiable List<ItemStack> getNonEmptyItemsFromHandler(IItemHandler handler) {
        ImmutableList.Builder<ItemStack> builder = ImmutableList.builder();
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            builder.add(stack);
        }
        return builder.build();
    }
}
