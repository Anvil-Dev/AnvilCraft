package dev.dubhe.anvilcraft.recipe;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.init.item.ModFoodItems;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.UseRemainder;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CanningFoodRecipe extends CustomRecipe {
    private static final CanningFoodRecipe INSTANCE = new CanningFoodRecipe();
    public static final MapCodec<CanningFoodRecipe> CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, CanningFoodRecipe> STREAM_CODEC = StreamCodec.unit(INSTANCE);
    public static final RecipeSerializer<CanningFoodRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);

    public boolean isFood(ItemStack foodStack) {
        if (foodStack.is(ModFoodItems.CANNED_FOOD)) return false;
        return foodStack.has(DataComponents.FOOD) && !foodStack.is(ModFoodItems.CANNED_FOOD);
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

    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack food = ItemStack.EMPTY;
        for (ItemStack item : input.items()) {
            if (food.isEmpty() && isFood(item)) {
                food = item.copy();
                food.setCount(1);
            } else if (isFood(item)) {
                food.setCount(food.getCount() + 1);
            }
        }

        return ModFoodItems.CANNED_FOOD.get().setFood(ModFoodItems.CANNED_FOOD.asStack(), food);
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> remainingItems = NonNullList.withSize(input.size(), ItemStack.EMPTY);

        for (int i = 0; i < remainingItems.size(); i++) {
            ItemStack item = input.getItem(i);
            if (item.getCraftingRemainder() != null) {
                remainingItems.set(i, item.getCraftingRemainder().create());
            } else {
                int finalI = i;
                Optional.ofNullable(item.get(DataComponents.USE_REMAINDER))
                    .map(UseRemainder::convertInto)
                    .map(ItemStackTemplate::create)
                    .ifPresent(stack -> remainingItems.set(finalI, stack));
            }
        }

        return remainingItems;
    }

    @Override
    public RecipeSerializer<CanningFoodRecipe> getSerializer() {
        return ModRecipeTypes.CANNING_FOOD_SERIALIZER.get();
    }
}
