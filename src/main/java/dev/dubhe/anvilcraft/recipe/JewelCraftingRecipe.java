package dev.dubhe.anvilcraft.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.builder.AbstractRecipeBuilder;
import dev.dubhe.anvilcraft.recipe.anvil.input.IItemsInput;
import dev.dubhe.anvilcraft.util.RecipeUtil;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.core.HolderGetter;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.conditions.ICondition;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Getter
@Accessors(fluent = true)
public final class JewelCraftingRecipe implements Recipe<JewelCraftingRecipe.Input> {
    private final List<ICondition> conditions;
    private final List<Ingredient> ingredients;
    private final ItemStackTemplate result;
    private final boolean hasVanishingCurse;
    private final List<Object2IntMap.Entry<Ingredient>> mergedIngredients;

    public static final RecipeSerializer<JewelCraftingRecipe> SERIALIZER = new RecipeSerializer<>(
        RecordCodecBuilder.mapCodec(instance -> instance.group(
            ICondition.LIST_CODEC.optionalFieldOf("neoforge:conditions", List.of()).forGetter(JewelCraftingRecipe::conditions),
            Ingredient.CODEC.listOf(1, 256).fieldOf("ingredients").forGetter(JewelCraftingRecipe::ingredients),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(JewelCraftingRecipe::result),
            Codec.BOOL.optionalFieldOf("has_vanishing_curse", true).forGetter(JewelCraftingRecipe::hasVanishingCurse)
        ).apply(instance, JewelCraftingRecipe::new)),
        StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list(256)), JewelCraftingRecipe::ingredients,
            ItemStackTemplate.STREAM_CODEC, JewelCraftingRecipe::result,
            ByteBufCodecs.BOOL, JewelCraftingRecipe::hasVanishingCurse,
            (ingredients, result, curse) -> new JewelCraftingRecipe(List.of(), ingredients, result, curse)
        )
    );

    public JewelCraftingRecipe(List<ICondition> conditions, List<Ingredient> ingredients, ItemStackTemplate result, boolean curse) {
        this.conditions = List.copyOf(conditions);
        this.ingredients = List.copyOf(ingredients);
        this.result = result;
        this.hasVanishingCurse = curse;
        this.mergedIngredients = RecipeUtil.mergeIngredient(ingredients);
        if (ingredients.isEmpty() || ingredients.size() > 256 || this.mergedIngredients.size() > 4) {
            throw new IllegalArgumentException("Jewel recipes require 1-256 ingredients in at most four groups");
        }
    }

    public static Builder builder(HolderGetter<Item> items) {
        return new Builder(items);
    }

    @Override
    public RecipeType<JewelCraftingRecipe> getType() {
        return ModRecipeTypes.JEWEL_CRAFTING.get();
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }

    @Override
    public RecipeSerializer<JewelCraftingRecipe> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public ItemStack assemble(Input input) {
        return this.result.create();
    }

    @Override
    public boolean matches(Input input, Level level) {
        return RecipeUtil.getMaxCraftTime(input, this.ingredients) >= 1;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public String group() {
        return "jewel_crafting";
    }

    public record Input(ItemStack source, List<ItemStack> items) implements RecipeInput, IItemsInput {
        @Override
        public ItemStack getItem(int index) {
            return this.items.get(index);
        }

        @Override
        public int size() {
            return this.items.size();
        }
    }

    @Setter
    @Accessors(fluent = true, chain = true)
    public static class Builder extends AbstractRecipeBuilder<JewelCraftingRecipe> {
        private final HolderGetter<Item> items;
        private final List<ICondition> conditions = new ArrayList<>();
        private final List<Ingredient> ingredients = new ArrayList<>();
        private @Nullable ItemStackTemplate result;
        private boolean hasVanishingCurse = true;

        public Builder(HolderGetter<Item> items) {
            this.items = items;
        }

        public Builder withCondition(ICondition condition) {
            this.conditions.add(condition);
            return this;
        }

        public Builder requires(Ingredient ingredient, int count) {
            for (int i = 0; i < count; i++) this.ingredients.add(ingredient);
            return this;
        }

        public Builder requires(Ingredient ingredient) {
            return this.requires(ingredient, 1);
        }

        public Builder requires(ItemLike item, int count) {
            return this.requires(Ingredient.of(item), count);
        }

        public Builder requires(ItemLike item) {
            return this.requires(item, 1);
        }

        public Builder requires(TagKey<Item> tag, int count) {
            return this.requires(Ingredient.of(this.items.getOrThrow(tag)), count);
        }

        public Builder requires(TagKey<Item> tag) {
            return this.requires(tag, 1);
        }

        public Builder result(ItemStack stack) {
            this.result = ItemStackTemplate.fromNonEmptyStack(stack);
            return this;
        }

        public Builder result(ItemLike item) {
            this.result = new ItemStackTemplate(item.asItem());
            return this;
        }

        @Override
        public JewelCraftingRecipe buildRecipe() {
            return new JewelCraftingRecipe(this.conditions, this.ingredients, this.getResult(), this.hasVanishingCurse);
        }

        @Override
        public void validate(Identifier id) {
            this.buildRecipe();
        }

        @Override
        public String getType() {
            return "jewel_crafting";
        }

        @Override
        public ItemStackTemplate getResult() {
            if (this.result == null) throw new IllegalStateException("Jewel recipe result must not be empty");
            return this.result;
        }
    }
}
