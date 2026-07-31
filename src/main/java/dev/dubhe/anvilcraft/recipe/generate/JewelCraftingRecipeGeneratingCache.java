package dev.dubhe.anvilcraft.recipe.generate;

import dev.dubhe.anvilcraft.recipe.JewelCraftingRecipe;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.DecoratedPotPatterns;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JewelCraftingRecipeGeneratingCache extends BaseGeneratingCache<JewelCraftingRecipe> {
    private static final Logger logger = BaseGeneratingCache.logger();

    private final List<Item> potterySherds = new ArrayList<>();

    public JewelCraftingRecipeGeneratingCache(HolderLookup.Provider registries) {
        super(registries, "jewel_crafting", "jewel crafting recipe");
    }

    private void prepareRun() {
        for (Holder.Reference<Item> holder : this.registries.lookupOrThrow(Registries.ITEM).listElements().toList()) {
            if (DecoratedPotPatterns.getPatternFromItem(holder.value()) != null && !holder.value().equals(Items.BRICK)) {
                JewelCraftingRecipeGeneratingCache.logger.debug(
                    "Add a pottery sherd {} for generating jewel crafting recipes", BuiltInRegistries.ITEM.getKey(holder.value())
                );
                this.potterySherds.add(holder.value());
            }
        }
    }

    @Override
    public Optional<List<RecipeHolder<JewelCraftingRecipe>>> buildRecipes() {
        this.prepareRun();
        if (this.potterySherds.isEmpty()) return Optional.empty();
        HolderGetter<Item> items = this.registries.lookupOrThrow(Registries.ITEM);

        List<RecipeHolder<JewelCraftingRecipe>> recipeHolders = new ArrayList<>();

        for (Item potterySherd : this.potterySherds) {
            JewelCraftingRecipe recipe = JewelCraftingRecipe.builder(items)
                .requires(Items.BRICK, 2)
                .source(potterySherd)
                .buildRecipe();
            recipeHolders.add(new RecipeHolder<>(this.generateRecipeId("pottery_sherds", potterySherd, potterySherd), recipe));
        }

        return Optional.of(recipeHolders);
    }
}
