package dev.dubhe.anvilcraft.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

public class RegistryUtil {
    public static <T> HolderLookup.RegistryLookup<T> lookup(ResourceKey<? extends Registry<? extends T>> key) {
        return RegistryUtil.lookup(Minecraft.getInstance(), key);
    }

    public static <T> HolderLookup.RegistryLookup<T> lookup(Minecraft minecraft, ResourceKey<? extends Registry<? extends T>> key) {
        return minecraft.getConnection().registryAccess().lookupOrThrow(key);
    }
}
