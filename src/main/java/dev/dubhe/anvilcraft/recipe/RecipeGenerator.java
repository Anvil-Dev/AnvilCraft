package dev.dubhe.anvilcraft.recipe;

import com.mojang.logging.LogUtils;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.recipe.anvil.CookingRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.ItemCompressRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.UnpackRecipe;
import dev.dubhe.anvilcraft.util.RecipeUtil;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BannerPatternItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.armortrim.TrimPatterns;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.SmokingRecipe;
import net.minecraft.world.level.block.entity.DecoratedPotPatterns;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RecipeGenerator {

    private static final Logger logger = LogUtils.getLogger();
    private static final String HASH_TO_CHAR = "0123456789abcdefghijklmnopqrstuv";

    private static ResourceLocation generateRecipeId(
        RecipeType<?> recipeType,
        RecipeHolder<?> recipeHolder
    ) {
        logger.debug("Generating anvil recipe for {}", recipeHolder.id());
        logger.debug("Recipe type of {} is {}", recipeHolder.id(), recipeType);
        ResourceLocation newId = hashRecipeId(recipeHolder.id());
        logger.debug("New id of {} is {}", recipeHolder.id(), newId);
        return newId;
    }

    private static ResourceLocation hashRecipeId(ResourceLocation rl) {
        long hash = 0;
        for (char c : rl.toString().toCharArray()) {
            hash *= 19980731;
            hash += c;
        }
        StringBuilder hashedId = new StringBuilder(rl.getPath());
        hashedId.append("_generated_");
        for (int i = 0; i < 13; i++) {
            hashedId.append(HASH_TO_CHAR.charAt((int) (hash >>> (5 * i)) & 31));
        }
        return AnvilCraft.of(hashedId.toString());
    }

    public static Optional<RecipeHolder<?>> handleVanillaRecipe(
        RecipeType<?> recipeType,
        RecipeHolder<?> recipeHolder
    ) {
        if (recipeType != RecipeType.SMOKING && recipeType != RecipeType.CRAFTING) return Optional.empty();
        if (recipeType == RecipeType.SMOKING) {
            SmokingRecipe recipe = (SmokingRecipe) recipeHolder.value();
            CookingRecipe newRecipe = CookingRecipe.builder()
                .requires(recipe.ingredient)
                .result(recipe.result)
                .buildRecipe();
            return Optional.of(new RecipeHolder<>(generateRecipeId(recipeType, recipeHolder), newRecipe));
        }
//        if (recipeType == RecipeType.BLASTING) {
//            BlastingRecipe recipe = (BlastingRecipe) recipeHolder.value();
//            AbstractItemProcessBuilder<SuperHeatingRecipe> builder =
//                SuperHeatingRecipe.builder()
//                    .requires(recipe.ingredient)
//                    .generated(true);
//            ItemStack result = recipe.result.copy();
//            logger.debug("Result of new recipe {} is {}", newId, result);
//            for (ItemStack item : recipe.ingredient.getItems()) {
//                logger.debug("Ingredient Item {} has following tags:", item);
//                item.getTags().forEach(it -> logger.debug("\t- {}", it.location()));
//            }
//            SuperHeatingRecipe newRecipe = builder.result(result).buildRecipe();
//            return Optional.of(new RecipeHolder<>(newId, newRecipe));
//        }
//        if (recipeType == RecipeType.SMELTING) {
//            SmeltingRecipe recipe = (SmeltingRecipe) recipeHolder.value();
//            AbstractItemProcessBuilder<SuperHeatingRecipe> builder =
//                SuperHeatingRecipe.builder()
//                    .requires(recipe.ingredient)
//                    .generated(true);
//            ItemStack result = recipe.result.copy();
//            logger.debug("Result of new recipe {} is {}", newId, result);
//            for (ItemStack item : recipe.ingredient.getItems()) {
//                logger.debug("Ingredient Item {} has following tags:", item);
//                item.getTags().forEach(it -> logger.debug("\t- {}", it.location()));
//            }
//            SuperHeatingRecipe newRecipe = builder.result(result)
//                .buildRecipe();
//            return Optional.of(new RecipeHolder<>(newId, newRecipe));
//        }
        if (recipeType == RecipeType.CRAFTING) {
            CraftingRecipe recipe = (CraftingRecipe) recipeHolder.value();
            if (recipe instanceof ShapedRecipe shapedRecipe) {
                ShapedRecipePattern pattern = shapedRecipe.pattern;
                if (pattern.height() == pattern.width()
                    && (pattern.height() == 2 || pattern.height() == 3)
                    && RecipeUtil.allIngredientEquals(pattern.ingredients())
                ) {
                    ItemCompressRecipe newRecipe = ItemCompressRecipe.builder()
                        .result(shapedRecipe.result)
                        .requires(pattern.ingredients().getFirst(), pattern.height() * pattern.height())
                        .buildRecipe();
                    return Optional.of(new RecipeHolder<>(generateRecipeId(recipeType, recipeHolder), newRecipe));
                }
            } else {
                if (recipe instanceof ShapelessRecipe shapelessRecipe) {
                    NonNullList<Ingredient> ingredients = shapelessRecipe.getIngredients();
                    if (ingredients.size() == 1) {
                        UnpackRecipe newRecipe = UnpackRecipe.builder()
                            .result(shapelessRecipe.result)
                            .requires(ingredients.getFirst())
                            .buildRecipe();
                        return Optional.of(new RecipeHolder<>(generateRecipeId(recipeType, recipeHolder), newRecipe));
                    }
                    if (RecipeUtil.allIngredientEquals(ingredients)) {
                        ItemCompressRecipe newRecipe = ItemCompressRecipe.builder()
                            .result(shapelessRecipe.result)
                            .requires(ingredients.getFirst(), ingredients.size())
                            .buildRecipe();
                        return Optional.of(new RecipeHolder<>(generateRecipeId(recipeType, recipeHolder), newRecipe));
                    }
                }
            }
        }
        return Optional.empty();
    }

    public static void handleJewelsRemove(
        JewelCraftingRecipeBuildingCache cache,
        RecipeHolder<?> recipeHolder
    ) {
        if (!(recipeHolder.value() instanceof JewelCraftingRecipe)) return;
        ItemStack result = recipeHolder.value().getResultItem(cache.registries);
        if (result.getItem() instanceof BannerPatternItem bannerPattern) {
            cache.remove(bannerPattern, 0);
        } else if (result.has(DataComponents.JUKEBOX_PLAYABLE)) {
            cache.remove(result.getItem(), 1);
        } else if (DecoratedPotPatterns.getPatternFromItem(result.getItem()) != null && !result.is(Items.BRICK)) {
            cache.remove(result.getItem(), 2);
        } else if (TrimPatterns.getFromTemplate(cache.registries, result).isPresent()) {
            cache.remove(result.getItem(), 3);
        }
    }

    public static class JewelCraftingRecipeBuildingCache {
        private final HolderLookup.Provider registries;
        private final List<Item> bannerPatterns = new ArrayList<>();
        private final List<Item> musicDiscs = new ArrayList<>();
        private final List<Item> potterySherds = new ArrayList<>();
        private final List<Item> trimTemplates = new ArrayList<>();

        public JewelCraftingRecipeBuildingCache(HolderLookup.Provider registries) {
            logger.debug("Initializing jewel crafting recipe building cache");
            this.registries = registries;
            for (Holder<Item> holder : registries.lookupOrThrow(Registries.ITEM).listElements().toList()) {
                if (holder.value() instanceof BannerPatternItem bannerPattern) {
                    logger.debug(
                        "Add a banner pattern {} for building jewel crafting recipes", BuiltInRegistries.ITEM.getKey(holder.value()));
                    this.bannerPatterns.add(bannerPattern);
                } else if (holder.value().getDefaultInstance().has(DataComponents.JUKEBOX_PLAYABLE)) {
                    logger.debug(
                        "Add a music disc {} for building jewel crafting recipes", BuiltInRegistries.ITEM.getKey(holder.value()));
                    this.musicDiscs.add(holder.value());
                } else if (
                    DecoratedPotPatterns.getPatternFromItem(holder.value()) != null
                        && !holder.value().equals(Items.BRICK)
                ) {
                    logger.debug(
                        "Add a pottery sherd {} for building jewel crafting recipes", BuiltInRegistries.ITEM.getKey(holder.value()));
                    this.potterySherds.add(holder.value());
                } else if (TrimPatterns.getFromTemplate(registries, holder.value().getDefaultInstance()).isPresent()) {
                    logger.debug(
                        "Add a trim template {} for building jewel crafting recipes", BuiltInRegistries.ITEM.getKey(holder.value()));
                    this.trimTemplates.add(holder.value());
                }
            }
        }

        private static ResourceLocation generateRecipeId(String type, Item recipeResult) {
            ResourceLocation resultId = BuiltInRegistries.ITEM.getKey(recipeResult);
            logger.debug("Generating jewel crafting recipe for {}", resultId);
            ResourceLocation newId = AnvilCraft.of("jewel_crafting/" + resultId.getPath() + "_for_" + type);
            logger.debug("The generated recipe id is {}", newId);
            return newId;
        }

        public void remove(Item item, int type) {
            switch (type) {
                case 0 -> {
                    logger.debug(
                        "Removes a banner pattern {} from jewel's cache, because there has a custom jewel' recipe",
                        BuiltInRegistries.ITEM.getKey(item));
                    this.bannerPatterns.remove(item);
                }
                case 1 -> {
                    logger.debug(
                        "Removes a music disc {} from jewel's cache, because there has a custom jewel' recipe",
                        BuiltInRegistries.ITEM.getKey(item));
                    this.musicDiscs.remove(item);
                }
                case 2 -> {
                    logger.debug(
                        "Removes a pottery sherd {} from jewel's cache, because there has a custom jewel' recipe",
                        BuiltInRegistries.ITEM.getKey(item));
                    this.potterySherds.remove(item);
                }
                case 3 -> {
                    logger.debug(
                        "Removes a trim template {} from jewel's cache, because there has a custom jewel' recipe",
                        BuiltInRegistries.ITEM.getKey(item));
                    this.trimTemplates.remove(item);
                }
            }
        }

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
                JewelCraftingRecipe recipe = JewelCraftingRecipe.builder()
                    .requires(Items.PAPER)
                    .requires(Items.INK_SAC)
                    .result(bannerPattern.getDefaultInstance())
                    .buildRecipe();
                recipeHolders.add(new RecipeHolder<>(generateRecipeId("banner_pattern", bannerPattern), recipe));
            }
            for (Item musicDisc : this.musicDiscs) {
                JewelCraftingRecipe recipe = JewelCraftingRecipe.builder()
                    .requires(ModItems.HARDEND_RESIN, 4)
                    .requires(Items.PAPER)
                    .result(musicDisc.getDefaultInstance())
                    .buildRecipe();
                recipeHolders.add(new RecipeHolder<>(generateRecipeId("music_disc", musicDisc), recipe));
            }
            for (Item potterySherd : this.potterySherds) {
                JewelCraftingRecipe recipe = JewelCraftingRecipe.builder()
                    .requires(Items.BRICK, 2)
                    .result(potterySherd.getDefaultInstance())
                    .buildRecipe();
                recipeHolders.add(new RecipeHolder<>(generateRecipeId("pottery_sherd", potterySherd), recipe));
            }
            for (Item trimTemplate : this.trimTemplates) {
                JewelCraftingRecipe recipe = JewelCraftingRecipe.builder()
                    .requires(ModItems.EARTH_CORE_SHARD)
                    .requires(Items.DIAMOND)
                    .result(trimTemplate.getDefaultInstance())
                    .buildRecipe();
                recipeHolders.add(new RecipeHolder<>(generateRecipeId("trim_template", trimTemplate), recipe));
            }

            return Optional.of(recipeHolders);
        }
    }
}
