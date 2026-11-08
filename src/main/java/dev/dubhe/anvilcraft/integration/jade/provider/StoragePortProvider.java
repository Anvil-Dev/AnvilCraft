package dev.dubhe.anvilcraft.integration.jade.provider;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.StoragePortBlockEntity;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;
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

/** 未标记端口的库存预览独立于自动化提取规则，避免已存物品在 Jade 中不可见。 */
public enum StoragePortProvider implements IServerExtensionProvider<ItemStack> {
    INSTANCE;

    public static final Identifier UID = AnvilCraft.of("storage_port");
    public static final String COUNTS_KEY = "Counts";

    @Override
    public @Nullable List<ViewGroup<ItemStack>> getGroups(Accessor<?> accessor) {
        if (!(accessor instanceof BlockAccessor blockAccessor)
            || !(blockAccessor.getBlockEntity() instanceof StoragePortBlockEntity port)
            || !port.getMarkedItem().isEmpty()) {
            return null;
        }
        var handler = port.getItemHandlerForDisplay();
        Map<ItemResource, Long> counts = new LinkedHashMap<>();
        for (int slot = 0; slot < handler.size(); slot++) {
            ItemResource resource = handler.getResource(slot);
            long amount = handler.getAmountAsLong(slot);
            if (resource.isEmpty() || amount <= 0) continue;
            counts.merge(resource, amount, Long::sum);
        }
        List<ItemStack> stacks = new ArrayList<>();
        long[] amounts = new long[Math.min(counts.size(), ItemCollector.MAX_SIZE)];
        for (var entry : counts.entrySet()) {
            if (stacks.size() == amounts.length) break;
            amounts[stacks.size()] = entry.getValue();
            stacks.add(entry.getKey().toStack(1));
        }
        ViewGroup<ItemStack> group = new ViewGroup<>(stacks);
        group.getExtraData().putLongArray(COUNTS_KEY, amounts);
        return List.of(group);
    }

    @Override
    public boolean shouldRequestData(Accessor<?> accessor) {
        return accessor instanceof BlockAccessor blockAccessor
            && blockAccessor.getBlockEntity() instanceof StoragePortBlockEntity;
    }

    @Override
    public Identifier getUid() {
        return UID;
    }
}
