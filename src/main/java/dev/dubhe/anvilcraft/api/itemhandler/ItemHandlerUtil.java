package dev.dubhe.anvilcraft.api.itemhandler;

import com.google.common.collect.ImmutableList;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.utility.BlockPlacerBlock;
import dev.dubhe.anvilcraft.util.AnvilUtil;
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
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;

public class ItemHandlerUtil {
    public static boolean exportToTarget(
        ResourceHandler<ItemResource> source,
        int maxAmountWeight,
        BiPredicate<ItemResource, Integer> predicate,
        @Nullable ResourceHandler<ItemResource> target
    ) {
        boolean success = false;
        int maxAmount = maxAmountWeight;
        try (Transaction root = Transaction.openRoot()) {
            for (int srcIndex = 0; srcIndex < source.size(); srcIndex++) {
                ItemResource resource = source.getResource(srcIndex);
                try (Transaction transaction = Transaction.open(root)) {
                    int extracted = source.extract(srcIndex, resource, Integer.MAX_VALUE, transaction);
                    if (extracted <= 0 || predicate.test(resource, extracted)) continue;
                    int inserted = target.insert(resource, extracted, transaction);
                    if (inserted == 0) continue;
                    success = true;
                    maxAmount -= inserted;
                    if (maxAmount <= 0) break;
                    transaction.commit();
                }
            }
            root.commit();
        }
        return success;
    }

    public static boolean importFromTarget(
        ResourceHandler<ItemResource> target,
        int maxAmountWeight,
        BiPredicate<ItemResource, Integer> predicate,
        ResourceHandler<ItemResource> source
    ) {
        return ItemHandlerUtil.exportToTarget(target, maxAmountWeight, predicate, source);
    }

    public static void exportAllToTarget(
        ResourceHandler<ItemResource> source,
        BiPredicate<ItemResource, Integer> predicate,
        ResourceHandler<ItemResource> target
    ) {
        try (Transaction root = Transaction.openRoot()) {
            for (int srcIndex = 0; srcIndex < source.size(); srcIndex++) {
                ItemResource resource = source.getResource(srcIndex);
                try (Transaction transaction = Transaction.open(root)) {
                    int extracted = source.extract(srcIndex, resource, Integer.MAX_VALUE, transaction);
                    if (extracted <= 0 || predicate.test(resource, extracted)) continue;
                    int inserted = target.insert(resource, extracted, transaction);
                    if (inserted == 0) continue;
                    transaction.commit();
                }
            }
            root.commit();
        }
    }

    public static void exportContentsToItemHandlers(
        ResourceHandler<ItemResource> source,
        @Nullable List<ResourceHandler<ItemResource>> itemHandlerList
    ) {
        if (itemHandlerList == null) return;
        for (ResourceHandler<ItemResource> target : itemHandlerList) {
            ItemHandlerUtil.exportAllToTarget(source, (_, _) -> true, target);
        }
    }

    public static void dropAllToPos(ResourceHandler<ItemResource> source, Level level, Vec3 pos) {
        List<ItemStack> items = new ArrayList<>();
        try (Transaction root = Transaction.openRoot()) {
            for (int srcIndex = 0; srcIndex < source.size(); srcIndex++) {
                ItemResource resource = source.getResource(srcIndex);
                if (resource.isEmpty()) continue;
                try (Transaction transaction = Transaction.open(root)) {
                    int extracted = source.extract(srcIndex, resource, Integer.MAX_VALUE, transaction);
                    if (extracted != 0) items.add(resource.toStack(extracted));
                }
            }
        }
        AnvilUtil.dropItems(items, level, pos);
    }

    public static @Nullable ResourceHandler<ItemResource> getSourceItemHandler(
        BlockPos inputBlockPos,
        Direction context,
        @Nullable Level level
    ) {
        if (level == null) return null;
        ResourceHandler<ItemResource> itemHandler = level.getCapability(
            Capabilities.Item.BLOCK,
            inputBlockPos,
            context
        );
        if (itemHandler != null) return itemHandler;
        AABB aabb = new AABB(inputBlockPos);
        List<ContainerEntity> entities = level.getEntitiesOfClass(
            Entity.class,
            aabb,
            e -> e instanceof ContainerEntity && !((ContainerEntity) e).isEmpty()
        ).stream().map(it -> (ContainerEntity) it).toList();
        if (!entities.isEmpty()) {
            itemHandler = ((Entity) entities.getFirst()).getCapability(
                Capabilities.Item.ENTITY,
                null
            );
        }
        return itemHandler;
    }

    public static @Nullable List<ResourceHandler<ItemResource>> getTargetItemHandlerList(
        BlockPos inputBlockPos,
        Direction context,
        @Nullable Level level
    ) {
        if (level == null) return null;
        List<ResourceHandler<ItemResource>> list = new ArrayList<>();
        ResourceHandler<ItemResource> input = level.getCapability(
            Capabilities.Item.BLOCK,
            inputBlockPos,
            context
        );
        if (input != null) {
            list.add(input);
            return list;
        }
        AABB aabb = new AABB(inputBlockPos);
        list = level.getEntitiesOfClass(
            Entity.class,
            aabb,
            e -> e instanceof ContainerEntity
        ).stream().map(e -> e.getCapability(Capabilities.Item.ENTITY, null)).toList();
        return list;
    }

    public static int countItemsInHandler(ResourceHandler<ItemResource> handler) {
        int count = 0;
        for (int i = 0; i < handler.size(); i++) {
            count += handler.getAmountAsInt(i);
        }
        return count;
    }

    @Nullable
    public static ResourceHandler<ItemResource> getSourceItemHandlerRecursive(
        Block source,
        BlockPos inputPos,
        Direction context,
        @Nullable Level level
    ) {
        int i = 0;
        do {
            if (level == null) return null;
            if (
                level.getBlockState(inputPos).is(source)
                && level.getBlockState(inputPos).getValue(BlockPlacerBlock.ORIENTATION).getDirection() == context
            ) {
                i++;
                inputPos = inputPos.relative(context.getOpposite());
            } else {
                return getSourceItemHandler(inputPos, context, level);
            }
        } while (i < AnvilCraft.CONFIG.blockPlacerRecursiveRetrievalDistanceMax);
        return null;
    }

    public static ItemStack insertItem(@Nullable ResourceHandler<ItemResource> dest, ItemStack stack, boolean simulate) {
        if (dest == null || stack.isEmpty()) return stack;

        if (dest instanceof PollableFilteredItemStackHandler pollable) {
            try (Transaction root = Transaction.openRoot()) {
                for (int i = 0; i < dest.size(); i++) {
                    try (Transaction transaction = Transaction.open(root)) {
                        stack.setCount(pollable.insertNoPolling(i, pollable.getResourceFrom(stack), stack.getCount(), transaction));
                        if (stack.isEmpty()) {
                            if (!simulate) transaction.commit();
                            return ItemStack.EMPTY;
                        }
                        if (!simulate) transaction.commit();
                    }
                }
                if (!simulate) root.commit();
            }
        } else {
            try (Transaction transaction = Transaction.openRoot()) {
                stack.setCount(dest.insert(ItemResource.of(stack.getItem(), stack.getComponentsPatch()), stack.getCount(), transaction));
                if (!simulate) transaction.commit();
            }
        }
        return stack;
    }

    public static boolean isEmptyContainer(@Nullable ResourceHandler<ItemResource> handler) {
        if (handler != null) {
            for (int i = 0; i < handler.size(); i++) {
                if (!handler.getResource(i).isEmpty()) return false;
            }
        }
        return true;
    }

    public static boolean isEmptyContainer(ItemStack stack) {
        return ItemHandlerUtil.isEmptyContainer(stack.getCapability(Capabilities.Item.ITEM, ItemAccess.forStack(stack)));
    }

    public static int hash(ResourceHandler<ItemResource> handler) {
        int hash = 0;
        for (int i = 0; i < handler.size(); i++) {
            ItemStack stack = handler.getResource(i).toStack();
            if (stack.isEmpty()) continue;
            hash *= 31;
            hash += Item.getId(stack.getItem()) + stack.getDamageValue();
        }
        return hash;
    }

    public static @Unmodifiable List<ItemStack> getNonEmptyItemsFromHandler(ResourceHandler<ItemResource> handler) {
        ImmutableList.Builder<ItemStack> builder = ImmutableList.builder();
        for (int i = 0; i < handler.size(); i++) {
            ItemResource resource = handler.getResource(i);
            ItemStack stack = new ItemStack(resource.typeHolder(), handler.getAmountAsInt(i), resource.getComponentsPatch());
            if (stack.isEmpty()) continue;
            builder.add(stack);
        }
        return builder.build();
    }
}
