package dev.dubhe.anvilcraft.data.recipe.anvil.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.dubhe.anvilcraft.data.recipe.Component;
import dev.dubhe.anvilcraft.data.recipe.CompoundTagPredicate;
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
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public class ItemAnvilRecipeBuilder implements RecipeBuilder {
    @Getter
    private final RecipeCategory category;
    @Getter
    private final NonNullList<Ingredient> recipeItems = NonNullList.create();
    @Getter
    final NonNullList<CompoundTagPredicate> compoundTagPredicates = NonNullList.create();
    @Getter
    private ItemAnvilRecipe.Location location = ItemAnvilRecipe.Location.UP;
    @Getter
    private final NonNullList<Component> components = NonNullList.create();
    private final ItemStack result;
    @Getter
    private ItemAnvilRecipe.Location resultLocation = ItemAnvilRecipe.Location.UP;
    @Getter
    private boolean isAnvilDamage = false;
    @Nullable
    private String group;
    @Getter
    private final Advancement.Builder advancement = Advancement.Builder.recipeAdvancement();

    public ItemAnvilRecipeBuilder(RecipeCategory category, ItemStack result) {
        this.category = category;
        this.result = result;
    }

    public static ItemAnvilRecipeBuilder item(RecipeCategory category, ItemLike result) {
        return ItemAnvilRecipeBuilder.item(category, result, 1);
    }

    public static ItemAnvilRecipeBuilder item(RecipeCategory category, ItemLike result, int count) {
        ItemStack stack = new ItemStack(result);
        stack.setCount(count);
        return ItemAnvilRecipeBuilder.item(category, stack);
    }

    public static ItemAnvilRecipeBuilder item(RecipeCategory category, ItemStack result) {
        return new ItemAnvilRecipeBuilder(category, result);
    }

    public ItemAnvilRecipeBuilder requires(TagKey<Item> tag) {
        return this.requires(Ingredient.of(tag));
    }

    public ItemAnvilRecipeBuilder requires(ItemLike item) {
        return this.requires(item, 1);
    }

    public ItemAnvilRecipeBuilder requires(ItemLike item, int quantity) {
        for (int i = 0; i < quantity; ++i) {
            this.requires(Ingredient.of(item));
        }
        return this;
    }

    public ItemAnvilRecipeBuilder requires(Ingredient ingredient) {
        return this.requires(ingredient, 1);
    }

    public ItemAnvilRecipeBuilder requires(Ingredient ingredient, int quantity) {
        return this.requires(ingredient, CompoundTagPredicate.EMPTY, quantity);
    }

    public ItemAnvilRecipeBuilder requires(Ingredient ingredient, CompoundTagPredicate compoundTagPredicate, int quantity) {
        for (int i = 0; i < quantity; ++i) {
            this.recipeItems.add(ingredient);
            this.compoundTagPredicates.add(compoundTagPredicate);
        }
        return this;
    }

    public ItemAnvilRecipeBuilder requires(@NotNull ItemStack stack) {
        return this.requires(Ingredient.of(stack.getItem()), CompoundTagPredicate.of(stack.getOrCreateTag()), stack.getCount());
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
        this.components.add(Component.of(Arrays.stream(block).map(BlockItem::getBlock)));
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
        return result.getItem();
    }

    @Override
    public void save(Consumer<FinishedRecipe> finishedRecipeConsumer, ResourceLocation recipeId) {
        this.ensureValid(recipeId);
        this.advancement.parent(ROOT_RECIPE_ADVANCEMENT).addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(recipeId)).rewards(AdvancementRewards.Builder.recipe(recipeId)).requirements(RequirementsStrategy.OR);
        finishedRecipeConsumer.accept(new Result(recipeId, this.recipeItems, this.compoundTagPredicates, this.location, this.components, this.result, this.resultLocation, this.isAnvilDamage, null == this.group ? "" : this.group, this.advancement, recipeId.withPrefix("recipes/" + this.category.getFolderName() + "/")));
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
        private final NonNullList<Ingredient> recipeItems;
        @Getter
        private final NonNullList<CompoundTagPredicate> compoundTagPredicates;
        @Getter
        private final ItemAnvilRecipe.Location location;
        @Getter
        private final NonNullList<Component> components;
        private final ItemStack result;
        @Getter
        private final ItemAnvilRecipe.Location resultLocation;
        @Getter
        private final boolean isAnvilDamage;
        private final String group;
        private final Advancement.Builder advancement;
        @Getter
        private final ResourceLocation advancementId;

        Result(ResourceLocation id, NonNullList<Ingredient> recipeItems, NonNullList<CompoundTagPredicate> compoundTagPredicates, ItemAnvilRecipe.Location location, NonNullList<Component> components, ItemStack result, ItemAnvilRecipe.Location resultLocation, boolean isAnvilDamage, String group, Advancement.Builder advancement, ResourceLocation advancementId) {
            this.id = id;
            this.result = result;
            this.recipeItems = recipeItems;
            this.compoundTagPredicates = compoundTagPredicates;
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
            for (int i = 0; i < this.recipeItems.size(); i++) {
                JsonElement jsonElement = this.recipeItems.get(i).toJson();
                if (jsonElement instanceof JsonObject object) {
                    JsonElement elem = this.compoundTagPredicates.get(i).toJson();
                    if (!elem.isJsonArray() || !elem.getAsJsonArray().isEmpty()) object.add("data", elem);
                }
                if (jsonElement instanceof JsonArray array) {
                    for (JsonElement element : array) {
                        if (element instanceof JsonObject obj) {
                            JsonElement elem = this.compoundTagPredicates.get(i).toJson();
                            if (!elem.isJsonArray() || !elem.getAsJsonArray().isEmpty()) obj.add("data", elem);
                        }
                    }
                }
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
            JsonObject result = new JsonObject();
            result.addProperty("item", BuiltInRegistries.ITEM.getKey(this.result.getItem()).toString());
            if (this.result.hasTag()) result.addProperty("data", this.result.getOrCreateTag().toString());
            if (this.result.getCount() > 1) result.addProperty("count", this.result.getCount());
            json.add("result", result);
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
