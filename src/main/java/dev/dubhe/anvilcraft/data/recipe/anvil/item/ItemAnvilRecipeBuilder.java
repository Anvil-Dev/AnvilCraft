package dev.dubhe.anvilcraft.data.recipe.anvil.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.dubhe.anvilcraft.data.recipe.Component;
import dev.dubhe.anvilcraft.data.recipe.TagIngredient;
import lombok.Getter;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.advancements.RequirementsStrategy;
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public class ItemAnvilRecipeBuilder implements RecipeBuilder {
    @Getter
    private final RecipeCategory category;
    @Getter
    private final NonNullList<TagIngredient> recipeItems = NonNullList.create();
    @Getter
    private ItemAnvilRecipe.Location location = ItemAnvilRecipe.Location.UP;
    @Getter
    private final NonNullList<Component> components = NonNullList.create();
    private final List<ItemStack> results = new ArrayList<>();
    @Getter
    private ItemAnvilRecipe.Location resultLocation = ItemAnvilRecipe.Location.UP;
    @Getter
    private boolean isAnvilDamage = false;
    @Nullable
    private String group;
    @Getter
    private final Advancement.Builder advancement = Advancement.Builder.recipeAdvancement();

    public ItemAnvilRecipeBuilder(RecipeCategory category, ItemStack... result) {
        this.category = category;
        this.results.addAll(Arrays.stream(result).toList());
    }
    public static @NotNull ItemAnvilRecipeBuilder item(RecipeCategory category) {
        return new ItemAnvilRecipeBuilder(category);
    }

    public static @NotNull ItemAnvilRecipeBuilder item(RecipeCategory category, ItemLike result) {
        return ItemAnvilRecipeBuilder.item(category, result, 1);
    }

    public static @NotNull ItemAnvilRecipeBuilder item(RecipeCategory category, ItemLike result, int count) {
        ItemStack stack = new ItemStack(result);
        stack.setCount(count);
        return ItemAnvilRecipeBuilder.item(category, stack);
    }

    public static @NotNull ItemAnvilRecipeBuilder item(RecipeCategory category, ItemStack result) {
        return new ItemAnvilRecipeBuilder(category, result);
    }

    public ItemAnvilRecipeBuilder result(ItemStack... stack) {
        this.results.addAll(Arrays.stream(stack).toList());
        return this;
    }

    public ItemAnvilRecipeBuilder result(ItemLike item, int count) {
        this.results.add(new ItemStack(item, count));
        return this;
    }

    public ItemAnvilRecipeBuilder requires(TagKey<Item> tag) {
        return this.requires(TagIngredient.of(tag));
    }

    public ItemAnvilRecipeBuilder requires(ItemLike item) {
        return this.requires(item, 1);
    }

    public ItemAnvilRecipeBuilder requires(ItemLike item, int quantity) {
        for (int i = 0; i < quantity; ++i) {
            this.requires(TagIngredient.of(item));
        }
        return this;
    }

    public ItemAnvilRecipeBuilder requires(TagIngredient ingredient) {
        return this.requires(ingredient, 1);
    }

    public ItemAnvilRecipeBuilder requires(TagIngredient ingredient, int quantity) {
        for (int i = 0; i < quantity; ++i) {
            this.recipeItems.add(ingredient);
        }
        return this;
    }

    public ItemAnvilRecipeBuilder requires(@NotNull ItemStack stack) {
        return this.requires(TagIngredient.of(stack), stack.getCount());
    }

    public ItemAnvilRecipeBuilder location(ItemAnvilRecipe.Location location) {
        this.location = location;
        return this;
    }

    public ItemAnvilRecipeBuilder component(Block... block) {
        this.components.add(Component.of(block));
        return this;
    }

    public ItemAnvilRecipeBuilder component(BlockItem... block) {
        this.components.add(Component.ofBlocks(Arrays.stream(block).map(BlockItem::getBlock)));
        return this;
    }

    @SafeVarargs
    public final ItemAnvilRecipeBuilder component(TagKey<Block>... block) {
        this.components.add(Component.of(block));
        return this;
    }

    public ItemAnvilRecipeBuilder resultLocation(ItemAnvilRecipe.Location location) {
        this.resultLocation = location;
        return this;
    }

    public ItemAnvilRecipeBuilder anvilDamage(boolean bl) {
        this.isAnvilDamage = bl;
        return this;
    }

    @Override
    public @NotNull ItemAnvilRecipeBuilder unlockedBy(String criterionName, CriterionTriggerInstance criterionTrigger) {
        this.advancement.addCriterion(criterionName, criterionTrigger);
        return this;
    }

    @Override
    public @NotNull ItemAnvilRecipeBuilder group(@Nullable String groupName) {
        this.group = groupName;
        return this;
    }

    @Override
    public @NotNull Item getResult() {
        return this.results.isEmpty() ? Items.AIR : this.results.get(0).getItem();
    }

    @Override
    public void save(Consumer<FinishedRecipe> finishedRecipeConsumer, ResourceLocation recipeId) {
        this.ensureValid(recipeId);
        this.advancement.parent(ROOT_RECIPE_ADVANCEMENT).addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(recipeId)).rewards(AdvancementRewards.Builder.recipe(recipeId)).requirements(RequirementsStrategy.OR);
        finishedRecipeConsumer.accept(new Result(recipeId, this.recipeItems, this.location, this.components, this.results, this.resultLocation, this.isAnvilDamage, null == this.group ? "" : this.group, this.advancement, recipeId.withPrefix("recipes/" + this.category.getFolderName() + "/")));
    }

    private void ensureValid(ResourceLocation id) {
        if (this.advancement.getCriteria().isEmpty()) {
            throw new IllegalStateException("No way of obtaining recipe " + id);
        }
    }

    static class Result implements FinishedRecipe {
        @Getter
        private final ResourceLocation id;
        @Getter
        private final NonNullList<TagIngredient> recipeItems;
        @Getter
        private final ItemAnvilRecipe.Location location;
        @Getter
        private final NonNullList<Component> components;
        private final List<ItemStack> results;
        @Getter
        private final ItemAnvilRecipe.Location resultLocation;
        @Getter
        private final boolean isAnvilDamage;
        private final String group;
        private final Advancement.Builder advancement;
        @Getter
        private final ResourceLocation advancementId;

        Result(ResourceLocation id, NonNullList<TagIngredient> recipeItems, ItemAnvilRecipe.Location location, NonNullList<Component> components, List<ItemStack> results, ItemAnvilRecipe.Location resultLocation, boolean isAnvilDamage, String group, Advancement.Builder advancement, ResourceLocation advancementId) {
            this.id = id;
            this.results = results;
            this.recipeItems = recipeItems;
            this.location = location;
            this.components = components;
            this.resultLocation = resultLocation;
            this.isAnvilDamage = isAnvilDamage;
            this.group = group;
            this.advancement = advancement;
            this.advancementId = advancementId;
        }

        @Override
        @SuppressWarnings("DuplicatedCode")
        public void serializeRecipeData(JsonObject json) {
            if (!this.group.isEmpty()) {
                json.addProperty("group", this.group);
            }
            JsonArray ingredients = new JsonArray();
            for (TagIngredient recipeItem : this.recipeItems) {
                JsonElement jsonElement = recipeItem.toJson();
                ingredients.add(jsonElement);
            }
            json.add("ingredients", ingredients);
            if (null != location && location != ItemAnvilRecipe.Location.UP)
                json.addProperty("location", location.getId());
            JsonArray componentsJson = new JsonArray();
            for (Component component : components) {
                componentsJson.add(component.toJson());
            }
            json.add("components", componentsJson);
            JsonArray results = new JsonArray();
            for (ItemStack result : this.results) {
                JsonObject object = new JsonObject();
                object.addProperty("item", BuiltInRegistries.ITEM.getKey(result.getItem()).toString());
                if (result.hasTag()) object.addProperty("data", result.getOrCreateTag().toString());
                if (result.getCount() > 1) object.addProperty("count", result.getCount());
                results.add(object);
            }
            json.add("results", results);
            if (null != resultLocation && resultLocation != ItemAnvilRecipe.Location.UP)
                json.addProperty("result_location", resultLocation.getId());
            json.addProperty("is_anvil_damage", this.isAnvilDamage);
        }

        @Override
        public @NotNull RecipeSerializer<?> getType() {
            return ItemAnvilRecipe.Serializer.INSTANCE;
        }

        @Nullable
        @Override
        public JsonObject serializeAdvancement() {
            return this.advancement.serializeToJson();
        }
    }
}
