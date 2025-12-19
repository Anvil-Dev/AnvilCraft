package dev.dubhe.anvilcraft.data.recipe.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

public class RecipeLoaderUtil {
    public static String getName(Object object) {
        return switch (object) {
            case null -> throw new IllegalArgumentException("Object cannot be null");
            case Block block -> BuiltInRegistries.BLOCK.getKey(block).getPath();
            case Fluid fluid -> BuiltInRegistries.FLUID.getKey(fluid).getPath();
            case ItemLike item -> BuiltInRegistries.ITEM.getKey(item.asItem()).getPath();
            case TagKey<?> tagKey -> tagKey.location().getPath();
            default -> throw new IllegalArgumentException("Unsupported object type: " + object.getClass().getName());
        };
    }
}
