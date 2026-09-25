package dev.dubhe.anvilcraft.api.tooltip;

import dev.dubhe.anvilcraft.block.entity.StoragePortBlockEntity;
import dev.dubhe.anvilcraft.inventory.tooltip.StoragePortTooltip;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class StoragePortItemTooltip {
    private StoragePortItemTooltip() {
    }

    public static CompoundTag blockEntityTag(ItemStack stack) {
        var data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        return data == null ? new CompoundTag() : data.copyTagWithoutId();
    }

    public static Optional<TooltipComponent> storagePortTooltipImage(ItemStack stack) {
        CompoundTag tag = blockEntityTag(stack);
        if (tag.get("marked_item") instanceof CompoundTag) return Optional.of(new StoragePortTooltip(tag));
        ListTag items = bufferEntries(tag);
        for (int index = 0; index < items.size(); index++) {
            CompoundTag entry = items.getCompoundOrEmpty(index);
            String id = entry.getStringOr("id", "");
            if (!id.isEmpty() && !id.equals("minecraft:air") && entry.getIntOr("count", 1) > 0) {
                return Optional.of(new StoragePortTooltip(tag));
            }
        }
        return Optional.empty();
    }

    private static ListTag bufferEntries(CompoundTag tag) {
        CompoundTag buffer = tag.getCompoundOrEmpty("buffer");
        return buffer.get("Items") instanceof ListTag items ? items : buffer.getListOrEmpty("stacks");
    }

    public static ItemStack markedItem(CompoundTag tag, HolderLookup.@Nullable Provider registries) {
        return StoragePortBlockEntity.createMarker(parse(tag.getCompoundOrEmpty("marked_item"), registries));
    }

    private static ItemStack parse(CompoundTag tag, HolderLookup.@Nullable Provider registries) {
        if (registries == null) return ItemStack.EMPTY;
        return ItemStack.OPTIONAL_CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), tag).result().orElse(ItemStack.EMPTY);
    }

    public static Contents contents(CompoundTag tag, HolderLookup.@Nullable Provider registries) {
        List<ItemStack> buffer = new ArrayList<>();
        ListTag entries = bufferEntries(tag);
        for (int index = 0; index < entries.size(); index++) {
            ItemStack stack = parse(entries.getCompoundOrEmpty(index), registries);
            if (!stack.isEmpty()) buffer.add(stack);
        }
        ItemStack icon = markedItem(tag, registries);
        if (icon.isEmpty() && !buffer.isEmpty()) icon = buffer.getFirst();
        int count = 0;
        for (ItemStack stack : buffer) {
            if (ItemStack.isSameItemSameComponents(icon, stack)) count += stack.getCount();
        }
        return new Contents(icon.copyWithCount(1), count);
    }

    public record Contents(ItemStack item, int count) {
    }
}
