package dev.dubhe.anvilcraft.recipe.generate;

import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.recipe.JewelCraftingRecipe;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SmithingTrimRecipe;
import net.minecraft.world.level.block.entity.DecoratedPotPatterns;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class JewelCraftingRecipeGeneratingCache extends BaseGeneratingCache<JewelCraftingRecipe> {
    private static final Logger logger = logger();

    private final List<Item> bannerPatterns = new ArrayList<>();
    private final List<Item> musicDiscs = new ArrayList<>();
    private final List<Item> potterySherds = new ArrayList<>();
    private final List<Item> trimTemplates = new ArrayList<>();

    public JewelCraftingRecipeGeneratingCache(HolderLookup.Provider registries, Collection<RecipeHolder<?>> recipes) {
        super(registries, "jewel_crafting", "jewel crafting recipe");
        for (Holder<Item> holder : registries.lookupOrThrow(Registries.ITEM).listElements().toList()) {
            if (holder.components().has(DataComponents.PROVIDES_BANNER_PATTERNS)) {
                logger.debug(
                    "Add a banner pattern {} for generating jewel crafting recipes", BuiltInRegistries.ITEM.getKey(holder.value()));
                this.bannerPatterns.add(holder.value());
            } else if (holder.components().has(DataComponents.JUKEBOX_PLAYABLE)) {
                logger.debug(
                    "Add a music disc {} for generating jewel crafting recipes", BuiltInRegistries.ITEM.getKey(holder.value()));
                this.musicDiscs.add(holder.value());
            } else if (
                DecoratedPotPatterns.getPatternFromItem(holder.value()) != null
                    && !holder.value().equals(Items.BRICK)
            ) {
                logger.debug(
                    "Add a pottery sherd {} for generating jewel crafting recipes", BuiltInRegistries.ITEM.getKey(holder.value()));
                this.potterySherds.add(holder.value());
            }
        }
        for (var holder : recipes) {
            if (!(holder.value() instanceof SmithingTrimRecipe trim)) continue;
            trim.templateIngredient().ifPresent(ingredient -> ingredient.getValues().forEach(item -> {
                if (!this.trimTemplates.contains(item.value())) this.trimTemplates.add(item.value());
            }));
        }
    }

    @Override
    public Optional<List<RecipeHolder<JewelCraftingRecipe>>> buildRecipes() {
        if (this.bannerPatterns.isEmpty()
            && this.musicDiscs.isEmpty()
            && this.potterySherds.isEmpty()
            && this.trimTemplates.isEmpty()
        ) {
            return Optional.empty();
        }

        List<RecipeHolder<JewelCraftingRecipe>> recipeHolders = new ArrayList<>();

        for (Item bannerPattern : this.bannerPatterns) {
            JewelCraftingRecipe recipe = JewelCraftingRecipe.builder(this.registries.lookupOrThrow(Registries.ITEM))
                .requires(Items.PAPER)
                .requires(Items.INK_SAC)
                .result(bannerPattern)
                .hasVanishingCurse(false)
                .buildRecipe();
            recipeHolders.add(new RecipeHolder<>(generateRecipeId("banner_patterns", bannerPattern, bannerPattern), recipe));
        }
        for (Item musicDisc : this.musicDiscs) {
            JewelCraftingRecipe recipe = JewelCraftingRecipe.builder(this.registries.lookupOrThrow(Registries.ITEM))
                .requires(ModItems.HARDEND_RESIN, 4)
                .requires(Items.PAPER)
                .result(musicDisc)
                .hasVanishingCurse(false)
                .buildRecipe();
            recipeHolders.add(new RecipeHolder<>(generateRecipeId("music_discs", musicDisc, musicDisc), recipe));
        }
        for (Item potterySherd : this.potterySherds) {
            JewelCraftingRecipe recipe = JewelCraftingRecipe.builder(this.registries.lookupOrThrow(Registries.ITEM))
                .requires(Items.BRICK, 2)
                .result(potterySherd)
                .hasVanishingCurse(false)
                .buildRecipe();
            recipeHolders.add(new RecipeHolder<>(generateRecipeId("pottery_sherds", potterySherd, potterySherd), recipe));
        }
        for (Item trimTemplate : this.trimTemplates) {
            JewelCraftingRecipe recipe = JewelCraftingRecipe.builder(this.registries.lookupOrThrow(Registries.ITEM))
                .requires(ModItems.EARTH_CORE_SHARD)
                .requires(Items.DIAMOND)
                .result(trimTemplate)
                .hasVanishingCurse(false)
                .buildRecipe();
            recipeHolders.add(new RecipeHolder<>(generateRecipeId("trim_templates", trimTemplate, trimTemplate), recipe));
        }

        return Optional.of(recipeHolders);
    }
}
