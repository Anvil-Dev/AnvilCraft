package dev.dubhe.anvilcraft.util;

import dev.dubhe.anvilcraft.recipe.anvil.AbstractItemProcessRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.input.ItemProcessInput;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AnvilUtil {
    public static <T extends AbstractItemProcessRecipe> void itemProcess(
            RecipeType<T> recipeType, Level level, final BlockPos itemPos, final Vec3 resultPos) {
        Map<ItemEntity, ItemStack> items = level.getEntitiesOfClass(ItemEntity.class, new AABB(itemPos)).stream()
                .map(it -> Map.entry(it, it.getItem()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        ItemProcessInput input = new ItemProcessInput(items.values().stream().toList());
        level.getRecipeManager().getRecipeFor(recipeType, input, level).ifPresent(recipe -> {
            int times = recipe.value().getMaxCraftTime(input);
            List<ItemStack> results = recipe.value().results.stream().map(ItemStack::copy).toList();
            results.forEach(s -> s.setCount(s.getCount() + times));
            for (int i = 0; i < times; i++) {
                for (Ingredient ingredient : recipe.value().getIngredients()) {
                    for (ItemStack stack : items.values()) {
                        if (ingredient.test(stack)) {
                            stack.shrink(1);
                            break;
                        }
                    }
                }
            }
            dropItems(results, level, resultPos);
        });
        items.forEach((k, v) -> {
            if (v.isEmpty()) {
                k.discard();
                return;
            }
            k.setItem(v.copy());
        });
    }

    public static void dropItems(@NotNull List<ItemStack> items, Level level, Vec3 pos) {
        for (ItemStack item : items) {
            if (item.isEmpty()) continue;
            ItemEntity entity = new ItemEntity(level, pos.x, pos.y, pos.z, item.copy(), 0.0d, 0.0d, 0.0d);
            level.addFreshEntity(entity);
        }
    }

    public static void returnItems(@NotNull Level level, @NotNull BlockPos pos, @NotNull List<ItemStack> items) {
        for (ItemStack item : items) {
            ItemStack type = item.copy();
            type.setCount(1);
            int maxSize = item.getMaxStackSize();
            int count = item.getCount();
            while (count > 0) {
                int size = Math.min(maxSize, count);
                count -= size;
                ItemStack stack = type.copy();
                stack.setCount(size);
                Vec3 vec3 = pos.getCenter();
                ItemEntity entity = new ItemEntity(level, vec3.x, vec3.y, vec3.z, stack, 0.0d, 0.0d, 0.0d);
                level.addFreshEntity(entity);
            }
        }
    }
}
