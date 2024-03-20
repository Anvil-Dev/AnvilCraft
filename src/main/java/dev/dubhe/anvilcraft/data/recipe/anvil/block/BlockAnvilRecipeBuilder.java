package dev.dubhe.anvilcraft.data.recipe.anvil.block;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.dubhe.anvilcraft.data.recipe.Component;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public class BlockAnvilRecipeBuilder implements RecipeBuilder {
    @Getter
    private final RecipeCategory category;
    private final NonNullList<Component> components = NonNullList.create();
    private final NonNullList<BlockState> results = NonNullList.create();
    private final NonNullList<ItemStack> dropItems = NonNullList.create();
    private boolean isAnvilDamage = false;
    @Getter
    private final Advancement.Builder advancement = Advancement.Builder.recipeAdvancement();
    @Nullable
    private String group = null;

    private BlockAnvilRecipeBuilder(RecipeCategory category, @NotNull Stream<BlockState> results) {
        this.category = category;
        results.forEach(this.results::add);
    }

    public static @NotNull BlockAnvilRecipeBuilder block(RecipeCategory category, BlockState... results) {
        return new BlockAnvilRecipeBuilder(category, Arrays.stream(results));
    }

    public static @NotNull BlockAnvilRecipeBuilder block(RecipeCategory category, Block... results) {
        return new BlockAnvilRecipeBuilder(category, Arrays.stream(results).map(Block::defaultBlockState));
    }

    public BlockAnvilRecipeBuilder component(@NotNull Stream<Component> components) {
        components.forEach(this.components::add);
        return this;
    }

    public BlockAnvilRecipeBuilder component(Block... components) {
        return this.component(Arrays.stream(components).map(Component::of));
    }

    @SafeVarargs
    public final BlockAnvilRecipeBuilder component(TagKey<Block>... components) {
        return this.component(Arrays.stream(components).map(Component::of));
    }

    public BlockAnvilRecipeBuilder component(Component... components) {
        return this.component(Arrays.stream(components));
    }

    public BlockAnvilRecipeBuilder drop(@NotNull Stream<ItemStack> drops) {
        drops.forEach(this.dropItems::add);
        return this;
    }

    public BlockAnvilRecipeBuilder drop(ItemStack... drops) {
        return this.drop(Arrays.stream(drops));
    }

    public BlockAnvilRecipeBuilder drop(Item... drops) {
        return this.drop(Arrays.stream(drops).map(Item::getDefaultInstance));
    }

    public BlockAnvilRecipeBuilder anvilDamage(boolean bl) {
        this.isAnvilDamage = bl;
        return this;
    }

    @Override
    public void save(Consumer<FinishedRecipe> finishedRecipeConsumer, ResourceLocation recipeId) {
        this.ensureValid(recipeId);
        this.advancement.parent(ROOT_RECIPE_ADVANCEMENT).addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(recipeId)).rewards(AdvancementRewards.Builder.recipe(recipeId)).requirements(RequirementsStrategy.OR);
        finishedRecipeConsumer.accept(new Result(recipeId, this.components, this.results, this.dropItems, this.isAnvilDamage, null == this.group ? "" : this.group, this.advancement, recipeId.withPrefix("recipes/" + this.category.getFolderName() + "/")));
    }

    private void ensureValid(ResourceLocation id) {
        if (this.advancement.getCriteria().isEmpty()) {
            throw new IllegalStateException("No way of obtaining recipe " + id);
        }
    }

    @Override
    public @NotNull Item getResult() {
        return results.get(results.size() - 1).getBlock().asItem();
    }

    @Override
    public @NotNull BlockAnvilRecipeBuilder unlockedBy(String criterionName, CriterionTriggerInstance criterionTrigger) {
        this.advancement.addCriterion(criterionName, criterionTrigger);
        return this;
    }

    @Override
    public @NotNull BlockAnvilRecipeBuilder group(@Nullable String groupName) {
        this.group = groupName;
        return this;
    }

    static class Result implements FinishedRecipe {
        @Getter
        private final ResourceLocation id;
        private final NonNullList<Component> components;
        private final NonNullList<BlockState> results;
        private final NonNullList<ItemStack> dropItems;
        private final boolean isAnvilDamage;
        private final String group;
        private final Advancement.Builder advancement;
        @Getter
        private final ResourceLocation advancementId;

        Result(ResourceLocation id, NonNullList<Component> components, NonNullList<BlockState> results, NonNullList<ItemStack> dropItems, boolean isAnvilDamage, String group, Advancement.Builder advancement, ResourceLocation advancementId) {
            this.id = id;
            this.components = components;
            this.results = results;
            this.dropItems = dropItems;
            this.isAnvilDamage = isAnvilDamage;
            this.group = group;
            this.advancement = advancement;
            this.advancementId = advancementId;
        }

        @Override
        public void serializeRecipeData(JsonObject json) {
            if (!this.group.isEmpty()) {
                json.addProperty("group", this.group);
            }
            JsonArray componentsJson = new JsonArray();
            for (Component component : components) {
                componentsJson.add(component.toJson());
            }
            json.add("components", componentsJson);
            JsonArray results = new JsonArray();
            for (BlockState result : this.results) {
                JsonObject resultObj = new JsonObject();
                resultObj.addProperty("block", BuiltInRegistries.BLOCK.getKey(result.getBlock()).toString());
                if (!result.toString().equals(result.getBlock().defaultBlockState().toString())) {
                    JsonObject state = new JsonObject();
                    for (Map.Entry<Property<?>, Comparable<?>> entry : result.getValues().entrySet()) {
                        String key = entry.getKey().getName();
                        Comparable<?> comparable = entry.getValue();
                        if (comparable instanceof Number obj) {
                            state.addProperty(key, obj);
                        } else if (comparable instanceof Boolean obj) {
                            state.addProperty(key, obj);
                        } else if (comparable instanceof String obj) {
                            state.addProperty(key, obj);
                        } else if (comparable instanceof Character obj) {
                            state.addProperty(key, obj);
                        }
                    }
                    resultObj.add("state", state);
                }
                results.add(resultObj);
            }
            json.add("results", results);
            JsonArray drops = new JsonArray();
            for (ItemStack dropItem : dropItems) {
                JsonObject result = new JsonObject();
                result.addProperty("item", BuiltInRegistries.ITEM.getKey(dropItem.getItem()).toString());
                drops.add(result);
            }
            json.add("drops", drops);
            json.addProperty("is_anvil_damage", this.isAnvilDamage);
        }

        @Override
        public @NotNull RecipeSerializer<?> getType() {
            return BlockAnvilRecipe.Serializer.INSTANCE;
        }

        @Nullable
        @Override
        public JsonObject serializeAdvancement() {
            return this.advancement.serializeToJson();
        }
    }
}
