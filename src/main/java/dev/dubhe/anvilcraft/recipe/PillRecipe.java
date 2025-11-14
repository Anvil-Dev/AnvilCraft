package dev.dubhe.anvilcraft.recipe;

import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModFoodItems;
import dev.dubhe.anvilcraft.init.reicpe.ModRecipeTypes;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PillRecipe extends CustomRecipe {
    public PillRecipe(CraftingBookCategory category) {
        super(category);
    }

    private boolean validatePill(ItemStack item) {
        PotionContents potionContents = item.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        Optional<Holder<Potion>> potion = potionContents.potion();
        return item.is(ModFoodItems.PILL) && potion.isEmpty();
    }

    public boolean validatePotion(ItemStack item) {
        PotionContents potionContents = item.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        Optional<Holder<Potion>> potion = potionContents.potion();
        return (
            item.is(Items.POTION)
                || item.is(Items.SPLASH_POTION)
                || item.is(Items.LINGERING_POTION)
        )
            && potion.isPresent() && !potion.get().is(Potions.WATER);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        List<ItemStack> items = new ArrayList<>();
        for (ItemStack item : input.items()) {
            if (!item.isEmpty()) {
                items.add(item);
            }
        }
        if (items.size() != 2) {
            return false;
        }
        int pillCount = 0;
        int potionCount = 0;
        for (ItemStack item : items) {
            if (validatePill(item)) {
                pillCount++;
            } else if (validatePotion(item)) {
                potionCount++;
            }
        }
        return pillCount == 1 && potionCount == 1;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        for (ItemStack item : input.items()) {
            if (item.is(Items.POTION)
                || item.is(Items.SPLASH_POTION)
                || item.is(Items.LINGERING_POTION)) {
                ItemStack stack = ModFoodItems.PILL.asStack();
                stack.set(DataComponents.POTION_CONTENTS, item.get(DataComponents.POTION_CONTENTS));
                if (item.is(Items.LINGERING_POTION)) {
                    stack.set(ModComponents.WEAKENING, true);
                }
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        return NonNullList.withSize(input.size(), Items.GLASS_BOTTLE.getDefaultInstance());
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeTypes.PILL_RECIPE_SERIALIZER.get();
    }
}
