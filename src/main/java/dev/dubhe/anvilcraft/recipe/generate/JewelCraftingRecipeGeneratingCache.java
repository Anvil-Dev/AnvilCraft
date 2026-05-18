package dev.dubhe.anvilcraft.recipe.generate;

import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.recipe.JewelCraftingRecipe;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
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
    private static final Logger logger = logger();

    private final List<Item> bannerPatterns = new ArrayList<>();
    private final List<Item> musicDiscs = new ArrayList<>();
    private final List<Item> potterySherds = new ArrayList<>();
    private final List<Item> trimTemplates = new ArrayList<>();

    public JewelCraftingRecipeGeneratingCache(HolderLookup.Provider registries) {
        super(registries, "jewel_crafting", "jewel crafting recipe");

    }

    private void prepareRun() {
        for (Holder.Reference<Item> holder : registries.lookupOrThrow(Registries.ITEM).listElements().toList()) {
            if (holder.value().components().has(DataComponents.PROVIDES_BANNER_PATTERNS)) {
                logger.debug(
                    "Add a banner pattern {} for generating jewel crafting recipes", BuiltInRegistries.ITEM.getKey(holder.value())
                );
                this.bannerPatterns.add(holder.value());
            }
            if (holder.value().components().has(DataComponents.JUKEBOX_PLAYABLE)) {
                logger.debug(
                    "Add a music disc {} for generating jewel crafting recipes", BuiltInRegistries.ITEM.getKey(holder.value())
                );
                this.musicDiscs.add(holder.value());
            }
            if (DecoratedPotPatterns.getPatternFromItem(holder.value()) != null && !holder.value().equals(Items.BRICK)) {
                logger.debug(
                    "Add a pottery sherd {} for generating jewel crafting recipes", BuiltInRegistries.ITEM.getKey(holder.value())
                );
                this.potterySherds.add(holder.value());
            }
            if (holder.key().identifier().getPath().endsWith("trim_smithing_template")) {
                logger.debug(
                    "Add a trim template {} for generating jewel crafting recipes", BuiltInRegistries.ITEM.getKey(holder.value())
                );
                this.trimTemplates.add(holder.value());
            }
        }
    }

    @Override
    public Optional<List<RecipeHolder<JewelCraftingRecipe>>> buildRecipes() {
        prepareRun();
        if (this.bannerPatterns.isEmpty()
            && this.musicDiscs.isEmpty()
            && this.potterySherds.isEmpty()
            && this.trimTemplates.isEmpty()
        ) {
            return Optional.empty();
        }
        HolderGetter<Item> items = this.registries.lookupOrThrow(Registries.ITEM);

        List<RecipeHolder<JewelCraftingRecipe>> recipeHolders = new ArrayList<>();

        for (Item bannerPattern : this.bannerPatterns) {
            JewelCraftingRecipe recipe = JewelCraftingRecipe.builder(items)
                .requires(Items.PAPER)
                .requires(Items.INK_SAC)
                .result(bannerPattern)
                .buildRecipe();
            recipeHolders.add(new RecipeHolder<>(generateRecipeId("banner_patterns", bannerPattern, bannerPattern), recipe));
        }
        for (Item musicDisc : this.musicDiscs) {
            JewelCraftingRecipe recipe = JewelCraftingRecipe.builder(items)
                .requires(ModItems.HARDEND_RESIN, 4)
                .requires(Items.PAPER)
                .result(musicDisc)
                .buildRecipe();
            recipeHolders.add(new RecipeHolder<>(generateRecipeId("music_discs", musicDisc, musicDisc), recipe));
        }
        for (Item potterySherd : this.potterySherds) {
            JewelCraftingRecipe recipe = JewelCraftingRecipe.builder(items)
                .requires(Items.BRICK, 2)
                .result(potterySherd)
                .buildRecipe();
            recipeHolders.add(new RecipeHolder<>(generateRecipeId("pottery_sherds", potterySherd, potterySherd), recipe));
        }
        for (Item trimTemplate : this.trimTemplates) {
            JewelCraftingRecipe recipe = JewelCraftingRecipe.builder(items)
                .requires(ModItems.EARTH_CORE_SHARD)
                .requires(Items.DIAMOND)
                .result(trimTemplate)
                .buildRecipe();
            recipeHolders.add(new RecipeHolder<>(generateRecipeId("trim_templates", trimTemplate, trimTemplate), recipe));
        }

        return Optional.of(recipeHolders);
    }
}
