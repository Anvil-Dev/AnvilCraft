package dev.dubhe.anvilcraft.data.recipe.crafting;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Getter;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.advancements.RequirementsStrategy;
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.CraftingRecipeBuilder;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Getter
@SuppressWarnings("unused")
public class ShapedTagRecipeBuilder extends ShapedRecipeBuilder {
    private final RecipeCategory category;
    private final ItemStack stackResult;
    private final List<String> rows = Lists.newArrayList();
    private final Map<Character, Ingredient> key = Maps.newLinkedHashMap();
    private final Advancement.Builder advancement = Advancement.Builder.recipeAdvancement();
    private @Nullable String group;
    private boolean showNotification = true;

    /**
     * 有 NBT 的有序合成配方
     *
     * @param category 分组
     * @param stack    产物
     */
    public ShapedTagRecipeBuilder(RecipeCategory category, @NotNull ItemStack stack) {
        super(category, stack.getItem(), stack.getCount());
        this.category = category;
        this.stackResult = stack;
    }

    /**
     * 创建新的有序配方生成器。
     */
    public static @NotNull ShapedTagRecipeBuilder shaped(@NotNull RecipeCategory category, @NotNull ItemLike result) {
        return ShapedTagRecipeBuilder.shaped(category, result.asItem().getDefaultInstance());
    }

    /**
     * 创建新的有序配方生成器。
     */
    public static @NotNull ShapedTagRecipeBuilder shaped(
        @NotNull RecipeCategory category, @NotNull ItemLike result, int count
    ) {
        ItemStack stack = result.asItem().getDefaultInstance();
        stack.setCount(count);
        return ShapedTagRecipeBuilder.shaped(category, stack);
    }

    /**
     * 创建新的有序配方生成器。
     */
    public static @NotNull ShapedTagRecipeBuilder shaped(RecipeCategory category, ItemStack result) {
        return new ShapedTagRecipeBuilder(category, result);
    }

    /**
     * 向此配方的模式添加一个新条目。
     */
    public @NotNull ShapedTagRecipeBuilder pattern(@NotNull String pattern) {
        super.pattern(pattern);
        if (!this.rows.isEmpty() && pattern.length() != this.rows.get(0).length()) {
            throw new IllegalArgumentException("Pattern must be the same width on every line!");
        }
        this.rows.add(pattern);
        return this;
    }

    /**
     * 向配方模式添加一个键。
     */
    public @NotNull ShapedTagRecipeBuilder define(@NotNull Character symbol, @NotNull Ingredient ingredient) {
        if (this.key.containsKey(symbol)) {
            throw new IllegalArgumentException("Symbol '" + symbol + "' is already defined!");
        }
        if (symbol == ' ') {
            throw new IllegalArgumentException("Symbol ' ' (whitespace) is reserved and cannot be defined");
        }
        this.key.put(symbol, ingredient);
        return this;
    }

    @Override
    public @NotNull ShapedRecipeBuilder define(@NotNull Character symbol, @NotNull ItemLike item) {
        return this.define(symbol, Ingredient.of(item));
    }

    @Override
    public @NotNull ShapedRecipeBuilder define(@NotNull Character symbol, @NotNull TagKey<Item> item) {
        return this.define(symbol, Ingredient.of(item));
    }

    @Override
    public @NotNull ShapedTagRecipeBuilder unlockedBy(
        @NotNull String criterionName, @NotNull CriterionTriggerInstance criterionTrigger
    ) {
        super.unlockedBy(criterionName, criterionTrigger);
        this.advancement.addCriterion(criterionName, criterionTrigger);
        return this;
    }

    /**
     * @param showNotification 显示提示
     * @return 构造器
     */
    public @NotNull ShapedTagRecipeBuilder showNotification(boolean showNotification) {
        super.showNotification(showNotification);
        this.showNotification = showNotification;
        return this;
    }

    @Override
    public @NotNull ShapedTagRecipeBuilder group(@Nullable String groupName) {
        super.group(groupName);
        this.group = groupName;
        return this;
    }

    @Override
    public void save(Consumer<FinishedRecipe> finishedRecipeConsumer, @NotNull ResourceLocation recipeId) {
        this.ensureValid(recipeId);
        this.advancement
            .parent(ROOT_RECIPE_ADVANCEMENT)
            .addCriterion("has_the_recipe",
                RecipeUnlockedTrigger.unlocked(recipeId))
            .rewards(AdvancementRewards.Builder.recipe(recipeId))
            .requirements(RequirementsStrategy.OR);
        finishedRecipeConsumer.accept(new Result(recipeId,
            this.stackResult, this.stackResult.getCount(),
            null == this.group ? "" : this.group, ShapedTagRecipeBuilder
            .determineBookCategory(this.category), this.rows, this.key,
            this.advancement, recipeId.withPrefix("recipes/" + this.category.getFolderName() + "/"),
            this.showNotification));
    }

    /**
     * Makes sure that this recipe is valid and obtainable.
     */
    private void ensureValid(ResourceLocation id) {
        if (this.rows.isEmpty()) {
            throw new IllegalStateException("No pattern is defined for shaped recipe " + id + "!");
        }
        HashSet<Character> set = Sets.newHashSet(this.key.keySet());
        set.remove(' ');
        for (String string : this.rows) {
            for (int i = 0; i < string.length(); ++i) {
                char c = string.charAt(i);
                if (!this.key.containsKey(c) && c != ' ') {
                    throw new IllegalStateException("Pattern in recipe " + id + " uses undefined symbol '" + c + "'");
                }
                set.remove(c);
            }
        }
        if (!set.isEmpty()) {
            throw new IllegalStateException("Ingredients are defined but not used in pattern for recipe " + id);
        }
        if (this.rows.size() == 1 && this.rows.get(0).length() == 1) {
            throw new IllegalStateException("Shaped recipe " + id
                + " only takes in a single item - should it be a shapeless recipe instead?");
        }
        if (this.advancement.getCriteria().isEmpty()) {
            throw new IllegalStateException("No way of obtaining recipe " + id);
        }
    }

    static class Result extends CraftingRecipeBuilder.CraftingResult {
        @Getter
        private final ResourceLocation id;
        private final ItemStack result;
        private final String group;
        private final List<String> pattern;
        private final Map<Character, Ingredient> key;
        private final Advancement.Builder advancement;
        @Getter
        private final ResourceLocation advancementId;
        private final boolean showNotification;

        public Result(ResourceLocation id, ItemStack result, int count,
                      String group, CraftingBookCategory category, List<String> pattern,
                      Map<Character, Ingredient> key, Advancement.Builder advancement,
                      ResourceLocation advancementId, boolean showNotification) {
            super(category);
            this.id = id;
            this.result = result;
            this.result.setCount(count);
            this.group = group;
            this.pattern = pattern;
            this.key = key;
            this.advancement = advancement;
            this.advancementId = advancementId;
            this.showNotification = showNotification;
        }

        @Override
        public @NotNull RecipeSerializer<?> getType() {
            return RecipeSerializer.SHAPED_RECIPE;
        }

        @Nullable
        @Override
        public JsonObject serializeAdvancement() {
            return this.advancement.serializeToJson();
        }

        @Override
        @SuppressWarnings("DuplicatedCode")
        public void serializeRecipeData(@NotNull JsonObject json) {
            super.serializeRecipeData(json);
            if (!this.group.isEmpty()) {
                json.addProperty("group", this.group);
            }
            JsonArray jsonArray = new JsonArray();
            for (String string : this.pattern) {
                jsonArray.add(string);
            }
            json.add("pattern", jsonArray);
            JsonObject jsonObject = new JsonObject();
            for (Map.Entry<Character, Ingredient> entry : this.key.entrySet()) {
                JsonElement value = entry.getValue().toJson();
                jsonObject.add(String.valueOf(entry.getKey()), entry.getValue().toJson());
            }
            json.add("key", jsonObject);
            JsonObject result = new JsonObject();
            result.addProperty("item", BuiltInRegistries.ITEM.getKey(this.result.getItem()).toString());
            if (this.result.hasTag()) result.addProperty("data", this.result.getOrCreateTag().toString());
            if (this.result.getCount() > 1) result.addProperty("count", this.result.getCount());
            json.add("result", result);
            json.addProperty("show_notification", this.showNotification);
        }
    }
}
