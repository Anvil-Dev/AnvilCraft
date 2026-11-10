package dev.dubhe.anvilcraft.integration.jade.provider;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.itemhandler.unlimited.UnlimitedItemStacksResourceHandler;
import dev.dubhe.anvilcraft.api.itemhandler.unlimited.UnlimitedItemStacksResourceHandler.ResourceKey;
import dev.dubhe.anvilcraft.block.container.storage.CrateBlock;
import dev.dubhe.anvilcraft.block.container.storage.HyperdimensionStorageStationBlock;
import dev.dubhe.anvilcraft.block.container.storage.LargeCrateBlock;
import dev.dubhe.anvilcraft.block.container.storage.ShulkerContainerBlock;
import dev.dubhe.anvilcraft.block.entity.StoragePortBlockEntity;
import dev.dubhe.anvilcraft.block.entity.StoragePortConsolidatorBlockEntity;
import dev.dubhe.anvilcraft.block.entity.storage.StorageBlockEntity;
import dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock;
import dev.dubhe.anvilcraft.saved.storage.Storages;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import org.jspecify.annotations.Nullable;
import snownee.jade.addon.universal.ItemCollector;
import snownee.jade.api.Accessor;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.view.IServerExtensionProvider;
import snownee.jade.api.view.ViewGroup;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 仓储相关方块的 Jade 物品预览。
 *
 * <p>端口展示缓存与标记物品计数；板条箱、大型板条箱、潜影集装箱、超维存储站
 * 以及直连仓储的整合器以有限条目预览库存，避免海量条目一次性同步。</p>
 */
public enum StoragePortProvider implements IServerExtensionProvider<ItemStack> {
    INSTANCE;

    public static final ResourceLocation UID = AnvilCraft.of("storage_port");
    public static final String COUNTS_KEY = "Counts";

    @Override
    public @Nullable List<ViewGroup<ItemStack>> getGroups(Accessor<?> accessor) {
        if (!(accessor instanceof BlockAccessor block)) {
            return null;
        }
        if (block.getBlockEntity() instanceof StoragePortBlockEntity port) {
            return this.portGroups(port);
        }
        if (block.getBlockEntity() instanceof StoragePortConsolidatorBlockEntity consolidator) {
            return this.consolidatorGroups(consolidator);
        }
        if (isStorageBlock(block.getBlockState())) {
            return this.storageGroups(block);
        }
        return null;
    }

    List<ViewGroup<ItemStack>> portGroups(StoragePortBlockEntity port) {
        List<ViewGroup<ItemStack>> groups = new ArrayList<>();
        groups.add(this.group("cache", collectItemCounts(port.getBuffer(), StoragePortBlockEntity.BUFFER_SLOTS)));
        if (!port.getMarkedItem().isEmpty()) {
            ViewGroup<ItemStack> storage = new ViewGroup<>(List.of(port.getMarkedItem().copyWithCount(1)));
            storage.id = "storage";
            storage.getExtraData().putLongArray(COUNTS_KEY, new long[]{port.getMarkedStorageCount()});
            groups.add(storage);
        }
        return groups;
    }

    /**
     * 潜影集装箱 / 超维存储站本体的库存预览；多方块部件先解析到主方块。
     */
    private List<ViewGroup<ItemStack>> storageGroups(BlockAccessor block) {
        BlockState state = block.getBlockState();
        BlockPos pos = block.getPosition();
        if (state.getBlock() instanceof AbstractMultiPartBlock<?> multipart) {
            pos = multipart.getMainPartPos(pos, state);
        }
        if (!(block.getLevel().getBlockEntity(pos) instanceof StorageBlockEntity storage)) {
            return List.of();
        }
        UUID id = storage.getId();
        if (id == null) {
            return List.of();
        }
        return Storages.get().get(id)
            .map(base -> {
                Map<ResourceKey, Long> counts = collectStorageCounts(
                    base.getAvailableItems(), ItemCollector.MAX_SIZE);
                return counts.isEmpty() ? List.<ViewGroup<ItemStack>>of()
                    : List.of(this.group("storage", counts));
            })
            .orElse(List.of());
    }

    /**
     * 整合器的物品预览：直连仓储时把缓存与只读仓储分成两组；
     * 否则预览相连端口汇总后的缓存与标记物品。
     */
    private List<ViewGroup<ItemStack>> consolidatorGroups(StoragePortConsolidatorBlockEntity consolidator) {
        UnlimitedItemStacksResourceHandler storage = consolidator.getDirectStorageHandler();
        if (storage == null) {
            Map<ResourceKey, Long> exposed = collectItemCounts(
                consolidator.getItemHandler(), ItemCollector.MAX_SIZE);
            return exposed.isEmpty() ? List.of() : List.of(this.group("storage", exposed));
        }
        List<ViewGroup<ItemStack>> groups = new ArrayList<>();
        Map<ResourceKey, Long> cache = collectItemCounts(
            consolidator.getBuffer(), StoragePortConsolidatorBlockEntity.BUFFER_SLOTS);
        if (!cache.isEmpty()) {
            groups.add(this.group("cache", cache));
        }
        Map<ResourceKey, Long> counts = collectStorageCounts(storage, ItemCollector.MAX_SIZE);
        if (!counts.isEmpty()) {
            groups.add(this.group("storage", counts));
        }
        return groups;
    }

    private static Map<ResourceKey, Long> collectItemCounts(IItemHandler handler, int maxTypes) {
        Map<ResourceKey, Long> counts = new LinkedHashMap<>();
        for (int slot = 0; slot < handler.getSlots() && counts.size() < maxTypes; slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            counts.merge(ResourceKey.of(stack), (long) stack.getCount(), Long::sum);
        }
        return counts;
    }

    private static Map<ResourceKey, Long> collectStorageCounts(
        UnlimitedItemStacksResourceHandler items,
        int maxTypes
    ) {
        Map<ResourceKey, Long> counts = new LinkedHashMap<>();
        for (int index = 0; index < items.size() && counts.size() < maxTypes; index++) {
            long amount = items.getAmountAsLong(index);
            if (amount <= 0) {
                continue;
            }
            ItemStack stack = items.getUnlimitedStackInSlot(index).toStack().copyWithCount(1);
            counts.merge(ResourceKey.of(stack), amount, Long::sum);
        }
        return counts;
    }

    private ViewGroup<ItemStack> group(String id, Map<ResourceKey, Long> counts) {
        List<ItemStack> stacks = new ArrayList<>(counts.size());
        long[] amounts = new long[counts.size()];
        int index = 0;
        for (Map.Entry<ResourceKey, Long> entry : counts.entrySet()) {
            stacks.add(entry.getKey().stack().copyWithCount(1));
            amounts[index++] = entry.getValue();
        }
        ViewGroup<ItemStack> group = new ViewGroup<>(stacks);
        group.id = id;
        group.getExtraData().putLongArray(COUNTS_KEY, amounts);
        return group;
    }

    @Override
    public boolean shouldRequestData(Accessor<?> accessor) {
        if (!(accessor instanceof BlockAccessor block)) {
            return false;
        }
        return block.getBlockEntity() instanceof StoragePortBlockEntity
            || block.getBlockEntity() instanceof StoragePortConsolidatorBlockEntity
            || isStorageBlock(block.getBlockState());
    }

    private static boolean isStorageBlock(BlockState state) {
        Block block = state.getBlock();
        return block instanceof CrateBlock
            || block instanceof LargeCrateBlock
            || block instanceof ShulkerContainerBlock
            || block instanceof HyperdimensionStorageStationBlock;
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}
