package dev.dubhe.anvilcraft.util;

import dev.dubhe.anvilcraft.recipe.ChanceItemStack;
import dev.dubhe.anvilcraft.recipe.anvil.AbstractItemProcessRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.input.ItemProcessInput;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AnvilUtil {
    public static <T extends AbstractItemProcessRecipe> boolean itemProcess(
        RecipeType<T> recipeType,
        Level level,
        final BlockPos itemPos,
        final Vec3 resultPos
    ) {
        Map<ItemEntity, ItemStack> items = level.getEntitiesOfClass(ItemEntity.class, new AABB(itemPos)).stream()
            .map(it -> Map.entry(it, it.getItem()))
            .collect(Util.toMap());

        ItemProcessInput input = new ItemProcessInput(items.values().stream().toList());
        Optional<RecipeHolder<T>> recipeOptional = level.getRecipeManager()
            .getRecipesFor(recipeType, input, level)
            .stream()
            .max(RecipeUtil::compareRecipeHolders);
        if (recipeOptional.isPresent()) {
            RecipeHolder<T> recipe = recipeOptional.get();
            int times = recipe.value().getMaxCraftTime(input);
            Object2IntMap<Item> results = new Object2IntOpenHashMap<>();
            LootContext context;
            if (level instanceof ServerLevel serverLevel) {
                context = RecipeUtil.emptyLootContext(serverLevel);
            } else {
                return false;
            }

            for (int i = 0; i < times; i++) {
                for (Ingredient ingredient : recipe.value().getIngredients()) {
                    for (ItemStack stack : items.values()) {
                        if (ingredient.test(stack)) {
                            stack.shrink(1);
                            break;
                        }
                    }
                }
                for (ChanceItemStack stack : recipe.value().getResults()) {
                    int amount = stack.getStack().getCount() * stack.getAmount().getInt(context);
                    results.mergeInt(stack.getStack().getItem(), amount, Integer::sum);
                }
            }
            dropItems(
                results.object2IntEntrySet().stream()
                    .map(entry -> new ItemStack(entry.getKey(), entry.getIntValue()))
                    .toList(),
                level,
                resultPos
            );
            items.forEach((k, v) -> {
                if (v.isEmpty()) {
                    k.discard();
                    return;
                }
                k.setItem(v.copy());
            });
            return true;
        }
        return false;
    }

    public static void dropItems(@NotNull List<ItemStack> items, Level level, Vec3 pos) {
        for (ItemStack item : items) {
            if (item.isEmpty()) continue;
            int count = item.getCount();
            int maxStack = item.getMaxStackSize();
            for (; count >= maxStack; count -= maxStack) {
                ItemEntity entity = new ItemEntity(
                    level,
                    pos.x,
                    pos.y,
                    pos.z,
                    item.copyWithCount(maxStack),
                    0.0d,
                    0.0d,
                    0.0d
                );
                level.addFreshEntity(entity);
            }
            if (count <= 0) continue;
            ItemEntity entity = new ItemEntity(
                level,
                pos.x,
                pos.y,
                pos.z,
                item.copyWithCount(count),
                0.0d,
                0.0d,
                0.0d
            );
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
