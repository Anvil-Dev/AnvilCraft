package dev.dubhe.anvilcraft.integration.jade.provider;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.container.storage.HyperdimensionStorageStationBlock;
import dev.dubhe.anvilcraft.block.container.storage.ShulkerContainerBlock;
import dev.dubhe.anvilcraft.block.entity.StoragePortBlockEntity;
import dev.dubhe.anvilcraft.block.entity.storage.StorageBlockEntity;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jspecify.annotations.Nullable;
import snownee.jade.api.Accessor;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.view.IServerExtensionProvider;
import snownee.jade.api.view.ViewGroup;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 端口仅展示固定大小的缓存和标记物品计数，仓储本体阻止通用库存枚举。 */
public enum StoragePortProvider implements IServerExtensionProvider<ItemStack> {
    INSTANCE;

    public static final Identifier UID = AnvilCraft.of("storage_port");
    public static final String COUNTS_KEY = "Counts";

    @Override
    public @Nullable List<ViewGroup<ItemStack>> getGroups(Accessor<?> accessor) {
        if (!(accessor instanceof BlockAccessor block)) {
            return null;
        }
        if (block.getBlockEntity() instanceof StoragePortBlockEntity port) {
            return this.portGroups(port);
        }
        return block.getBlockEntity() instanceof StorageBlockEntity
            || block.getBlockState().getBlock() instanceof ShulkerContainerBlock
            || block.getBlockState().getBlock() instanceof HyperdimensionStorageStationBlock ? List.of() : null;
    }

    List<ViewGroup<ItemStack>> portGroups(StoragePortBlockEntity port) {
        var handler = port.getBuffer();
        Map<ItemResource, Long> counts = new LinkedHashMap<>();
        for (int slot = 0; slot < StoragePortBlockEntity.BUFFER_SLOTS; slot++) {
            ItemResource resource = handler.getResource(slot);
            if (!resource.isEmpty()) {
                counts.merge(resource, handler.getAmountAsLong(slot), Long::sum);
            }
        }
        List<ItemStack> stacks = new ArrayList<>();
        long[] amounts = new long[counts.size()];
        for (var entry : counts.entrySet()) {
            amounts[stacks.size()] = entry.getValue();
            stacks.add(entry.getKey().toStack(1));
        }
        List<ViewGroup<ItemStack>> groups = new ArrayList<>();
        ViewGroup<ItemStack> buffer = new ViewGroup<>(stacks);
        buffer.id = "cache";
        buffer.getExtraData().putLongArray(COUNTS_KEY, amounts);
        groups.add(buffer);
        if (!port.getMarkedItem().isEmpty()) {
            ViewGroup<ItemStack> storage = new ViewGroup<>(List.of(port.getMarkedItem().copyWithCount(1)));
            storage.id = "storage";
            storage.getExtraData().putLongArray(COUNTS_KEY, new long[]{port.getMarkedStorageCount()});
            groups.add(storage);
        }
        return groups;
    }

    @Override
    public boolean shouldRequestData(Accessor<?> accessor) {
        return accessor instanceof BlockAccessor block && block.getBlockEntity() instanceof StoragePortBlockEntity;
    }

    @Override
    public Identifier getUid() {
        return UID;
    }
}
