package dev.dubhe.anvilcraft.util;

import com.google.common.collect.Iterables;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class TagUtil {
    public static <T> Iterable<Holder<T>> getValuesFromTag(ResourceKey<Registry<T>> registryKey, TagKey<T> tag, RegistryAccess registry) {
        return registry.lookupOrThrow(registryKey)
                .getTagOrEmpty(tag);
    }

    public static Iterable<ItemStack> getItemStacksFromTag(TagKey<Item> tag, RegistryAccess registry) {
        Iterable<Holder<Item>> iterable = getValuesFromTag(Registries.ITEM, tag, registry);
        return Iterables.transform(
            iterable,
            holder -> {
                if (holder == null) throw new NullPointerException("The item recipes is null");
                return holder.value().getDefaultInstance();
            }
        );
    }

    public static <T> Optional<HolderSet.Named<T>> toHolderSet(@Nullable HolderLookup.RegistryLookup<T> lookup, TagKey<T> tag) {
        if (lookup == null) return Optional.empty();
        return lookup.get(tag);
    }
}
