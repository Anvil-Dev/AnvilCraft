package dev.dubhe.anvilcraft.api.event;

import dev.dubhe.anvilcraft.init.registry.ModRegistryKeys;
import dev.dubhe.anvilcraft.saved.storage.category.ICategory;
import dev.dubhe.anvilcraft.saved.storage.category.NamespaceCategory;
import lombok.Getter;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.bus.api.Event;

import java.util.HashMap;
import java.util.Map;

@Getter
public class ModCategoryInitEvent extends Event {
    private final HolderLookup.Provider registries;
    private final HolderLookup.RegistryLookup<ICategory> lookup;
    private final Map<String, ICategory> categories = new HashMap<>();

    public ModCategoryInitEvent(HolderLookup.Provider registries) {
        this.registries = registries;
        this.lookup = registries.lookupOrThrow(ModRegistryKeys.CATEGORY);
    }

    public void register(String modId, ICategory category) {
        this.categories.put(modId, category);
    }

    public void register(String modId, ItemStack icon) {
        this.register(modId, new NamespaceCategory(icon, modId));
    }

    public void register(String modId, ItemLike icon) {
        this.register(modId, new NamespaceCategory(icon, modId));
    }

    public void register(String modId, Component name, ItemStack icon) {
        this.register(modId, new NamespaceCategory(icon, name, modId));
    }

    public void register(String modId, Component name, ItemLike icon) {
        this.register(modId, new NamespaceCategory(new ItemStack(icon), name, modId));
    }

    public ICategory get(ResourceKey<ICategory> key) {
        return this.getHolder(key).value();
    }

    public Holder.Reference<ICategory> getHolder(ResourceKey<ICategory> key) {
        return this.lookup.getOrThrow(key);
    }
}
