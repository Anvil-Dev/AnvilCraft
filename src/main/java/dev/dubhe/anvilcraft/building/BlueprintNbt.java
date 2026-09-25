package dev.dubhe.anvilcraft.building;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

final class BlueprintNbt {
    private BlueprintNbt() {
    }

    static ItemStack readItem(HolderLookup.Provider registries, CompoundTag tag) {
        return ItemStack.OPTIONAL_CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), tag).result().orElse(ItemStack.EMPTY);
    }

    static CompoundTag writeItem(HolderLookup.Provider registries, ItemStack stack) {
        return (CompoundTag) ItemStack.OPTIONAL_CODEC
            .encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), stack).getOrThrow();
    }

    static Tag canonicalCopy(Tag value) {
        if (value instanceof CompoundTag compound) {
            CompoundTag result = new CompoundTag();
            compound.keySet().stream().sorted().forEach(key -> result.put(key, BlueprintNbt.canonicalCopy(compound.get(key))));
            return result;
        }
        if (value instanceof ListTag list) {
            ListTag result = new ListTag();
            for (Tag element : list) result.add(BlueprintNbt.canonicalCopy(element));
            return result;
        }
        return value.copy();
    }

    static ListTag list(CompoundTag tag, String key, int elementType) {
        ListTag values = tag.getListOrEmpty(key);
        return values.stream().allMatch(value -> value.getId() == elementType) ? values : new ListTag();
    }
}
