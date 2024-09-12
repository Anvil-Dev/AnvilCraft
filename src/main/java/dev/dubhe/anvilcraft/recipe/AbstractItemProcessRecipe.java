package dev.dubhe.anvilcraft.recipe;

import dev.dubhe.anvilcraft.recipe.input.ItemProcessInput;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;

import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import lombok.Getter;

@Getter
public abstract class AbstractItemProcessRecipe implements Recipe<ItemProcessInput> {
    public final NonNullList<Ingredient> ingredients;
    public final ItemStack result;
    public final boolean isSimple;
    private ItemProcessInput cacheInput;
    private int cacheMaxCraftTime = -1;

    public AbstractItemProcessRecipe(NonNullList<Ingredient> ingredients, ItemStack result) {
        this.ingredients = ingredients;
        this.result = result;
        this.isSimple = ingredients.stream().allMatch(Ingredient::isSimple);
    }

    @Override
    public boolean canCraftInDimensions(int pWidth, int pHeight) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider pRegistries) {
        return result;
    }

    @Override
    public boolean matches(ItemProcessInput pInput, Level pLevel) {
        return getMaxCraftTime(pInput) > 0;
    }

    @Override
    public ItemStack assemble(ItemProcessInput pInput, HolderLookup.Provider pRegistries) {
        return this.result.copy();
    }

    public int getMaxCraftTime(ItemProcessInput pInput) {
        if (cacheInput == pInput) {
            return cacheMaxCraftTime;
        }
        Object2IntMap<Item> contents = new Object2IntOpenHashMap<>();
        Object2BooleanMap<Item> flags = new Object2BooleanOpenHashMap<>();
        for (ItemStack stack : pInput.items()) {
            contents.mergeInt(stack.getItem(), stack.getCount(), Integer::sum);
            flags.put(stack.getItem(), false);
        }
        int times = 0;
        while (true) {
            for (Ingredient ingredient : ingredients) {
                for (Item item : contents.keySet()) {
                    if (ingredient.test(new ItemStack(item))) {
                        contents.put(item, contents.getInt(item) - 1);
                        flags.put(item, true);
                    }
                }
            }
            if (flags.values().stream().anyMatch(flag -> !flag)) {
                cacheInput = pInput;
                cacheMaxCraftTime = 0;
                return 0;
            }
            if (contents.values().intStream().allMatch(i -> i >= 0)) {
                times += 1;
            } else {
                cacheInput = pInput;
                cacheMaxCraftTime = times;
                return times;
            }
        }
    }
}
