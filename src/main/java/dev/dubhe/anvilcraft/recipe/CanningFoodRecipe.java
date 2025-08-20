package dev.dubhe.anvilcraft.recipe;

import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CanningFoodRecipe extends CustomRecipe {
    public CanningFoodRecipe(CraftingBookCategory category) {
        super(category);
    }

    public boolean isFood(ItemStack foodStack) {
        if (foodStack.is(ModItems.CANNED_FOOD)) return false;
        return foodStack.has(DataComponents.FOOD) && !foodStack.is(ModItems.CANNED_FOOD);
    }

    public boolean matches(CraftingInput input, Level level) {
        List<ItemStack> items = new ArrayList<>();
        for (ItemStack item : input.items()) {
            if (!item.isEmpty()) {
                items.add(item);
            }
        }
        if (items.size() < 2 || items.size() > 6) {
            return false;
        }
        int canCount = 0;
        int foodCount = 0;
        ItemStack food = ItemStack.EMPTY;
        for (ItemStack item : items) {
            if (item.is(ModItems.TIN_CAN)) {
                canCount++;
            } else if (isFood(item)) {
                if (food.isEmpty()) {
                    food = item.copy();
                } else if (!food.is(item.getItem())) {
                    return false;
                }
                foodCount++;
            } else {
                return false;
            }
        }
        return canCount == 1 && foodCount >= 1 && foodCount <= 5;
    }

    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack food = ItemStack.EMPTY;
        for (ItemStack item : input.items()) {
            if (food.isEmpty() && isFood(item)) {
                food = item.copy();
                food.setCount(1);
            } else if (isFood(item)) {
                food.setCount(food.getCount() + 1);
            }
        }

        return ModItems.CANNED_FOOD.get().setFood(ModItems.CANNED_FOOD.asStack(), food);
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> remainingItems = NonNullList.withSize(input.size(), ItemStack.EMPTY);

        for (int i = 0; i < remainingItems.size(); i++) {
            ItemStack item = input.getItem(i);
            if (item.hasCraftingRemainingItem()) {
                remainingItems.set(i, item.getCraftingRemainingItem());
            } else {
                int finalI = i;
                Optional.ofNullable(item.get(DataComponents.FOOD))
                    .flatMap(FoodProperties::usingConvertsTo)
                    .ifPresent(stack -> remainingItems.set(finalI, stack.copy()));
            }
        }

        return remainingItems;
    }

    /**
     * Used to determine if this recipe can fit in a grid of the given width/height
     */
    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeTypes.CANNING_FOOD_SERIALIZER.get();
    }
}
