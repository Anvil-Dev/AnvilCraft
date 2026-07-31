package dev.dubhe.anvilcraft.util;

import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.recipe.anvil.input.IItemsInput;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.context.ContextKeySet;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RecipeUtil {
    public static final ItemIngredientPredicate EMPTY_ITEM_INGREDIENT = ItemIngredientPredicate.Builder.item().build();

    public static LootContext emptyLootContext(ServerLevel level) {
        return new LootContext.Builder(new LootParams.Builder(level).create(ContextKeySet.EMPTY)).create(Optional.empty());
    }

    public static List<Object2IntMap.Entry<Ingredient>> mergeIngredient(List<Ingredient> ingredients) {
        Object2IntMap<Ingredient> margeIngredients = new Object2IntLinkedOpenHashMap<>();
        for (Ingredient ingredient : ingredients) {
            boolean flag = false;
            for (Ingredient key : margeIngredients.keySet()) {
                if (ingredient.equals(key)) {
                    margeIngredients.put(key, margeIngredients.getInt(key) + 1);
                    flag = true;
                }
            }
            if (!flag) {
                margeIngredients.put(ingredient, 1);
            }
        }
        return new ArrayList<>(margeIngredients.object2IntEntrySet());
    }

    public static int getMaxCraftTime(IItemsInput input, List<Ingredient> ingredients) {
        Object2IntMap<Item> contents = new Object2IntOpenHashMap<>();
        Object2BooleanMap<Ingredient> ingredientFlags = new Object2BooleanOpenHashMap<>();
        Object2BooleanMap<Item> flags = new Object2BooleanOpenHashMap<>();
        for (Ingredient ingredient : ingredients) {
            ingredientFlags.put(ingredient, false);
        }
        for (ItemStack stack : input.items()) {
            if (stack.isEmpty()) continue;
            contents.mergeInt(stack.getItem(), stack.getCount(), Integer::sum);
            flags.put(stack.getItem(), false);
        }
        int times = 0;
        while (true) {
            for (Ingredient ingredient : ingredients) {
                for (Item item : contents.keySet()) {
                    if (ingredient.test(new ItemStack(item))) {
                        contents.put(item, contents.getInt(item) - 1);
                        ingredientFlags.put(ingredient, true);
                        flags.put(item, true);
                    }
                }
            }
            if (ingredientFlags.values().stream().anyMatch(flag -> !flag)
                || flags.values().stream().anyMatch(flag -> !flag)) {
                return 0;
            }
            if (contents.values().intStream().allMatch(i -> i >= 0)) {
                times += 1;
            } else {
                return times;
            }
        }
    }

    private static final Int2ObjectMap<List<ItemStack>> INGREDIENT_CACHE = new Int2ObjectArrayMap<>();

    public static List<ItemStack> getItems(ItemIngredientPredicate predicate, HolderLookup<Item> items) {
        int hash = predicate.hashCode();
        if (!RecipeUtil.INGREDIENT_CACHE.containsKey(hash)) {
            if (predicate.items().isPresent()) {
                RecipeUtil.INGREDIENT_CACHE.put(hash, Arrays.stream(predicate.getItems()).map(ItemStackTemplate::create).toList());
            } else {
                List<ItemStack> stacks = new ArrayList<>();
                items.listElements()
                    .map(Holder.Reference::value)
                    .map(Item::getDefaultInstance)
                    .filter(predicate)
                    .forEach(stacks::add);
                RecipeUtil.INGREDIENT_CACHE.put(hash, List.copyOf(stacks));
            }
        }
        return RecipeUtil.INGREDIENT_CACHE.get(hash);
    }
}
