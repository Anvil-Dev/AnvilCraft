package dev.dubhe.anvilcraft.util;

import com.google.common.collect.Collections2;
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

import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

public class TagUtil {
    public static <T> List<Holder<T>> getValuesFromTag(ResourceKey<Registry<T>> registryKey, TagKey<T> tag, RegistryAccess registry) {
        return registry.registryOrThrow(registryKey)
            .getTag(tag).orElseThrow(() -> new NoSuchElementException("The tag " + tag.location() + " does not exist!"))
            .stream().toList();
    }

    public static Collection<ItemStack> getItemStacksFromTag(TagKey<Item> tag, RegistryAccess registry) {
        return Collections2.transform(
            getValuesFromTag(Registries.ITEM, tag, registry),
            holder -> holder.value().getDefaultInstance()
        );
    }

    public static <T> Optional<HolderSet.Named<T>> toHolderSet(@Nullable HolderLookup.RegistryLookup<T> lookup, TagKey<T> tag) {
        if (lookup == null) return Optional.empty();
        return lookup.get(tag);
    }
}
