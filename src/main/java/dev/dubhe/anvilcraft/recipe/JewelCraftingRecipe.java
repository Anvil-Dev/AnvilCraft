package dev.dubhe.anvilcraft.recipe;

import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.builder.AbstractRecipeBuilder;
import dev.dubhe.anvilcraft.recipe.anvil.input.IItemsInput;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import net.minecraft.core.HolderGetter;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
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

import java.util.ArrayList;
import java.util.List;

public record JewelCraftingRecipe(
    List<ICondition> conditions,
    ItemIngredientPredicate source,
    List<ItemIngredientPredicate> ingredients
) implements Recipe<JewelCraftingRecipe.Input> {
    public static final RecipeSerializer<JewelCraftingRecipe> SERIALIZER = new RecipeSerializer<>(
        RecordCodecBuilder.mapCodec(ins -> ins.group(
            ICondition.LIST_CODEC
                .optionalFieldOf("neoforge:conditions", new ArrayList<>())
                .forGetter(JewelCraftingRecipe::conditions),
            ItemIngredientPredicate.CODEC
                .fieldOf("source")
                .forGetter(JewelCraftingRecipe::source),
            ItemIngredientPredicate.CODEC
                .listOf(0, 4)
                .fieldOf("ingredients")
                .forGetter(JewelCraftingRecipe::ingredients)
        ).apply(ins, JewelCraftingRecipe::new)),
        StreamCodec.composite(
            ItemIngredientPredicate.STREAM_CODEC,
            JewelCraftingRecipe::source,
            ItemIngredientPredicate.STREAM_CODEC.apply(ByteBufCodecs.list(4)),
            JewelCraftingRecipe::ingredients,
            JewelCraftingRecipe::new
        )
    );

    public JewelCraftingRecipe {
        if (ingredients.size() > 4) throw new IllegalArgumentException("Too many different ingredients");
    }

    public JewelCraftingRecipe(ItemIngredientPredicate source, List<ItemIngredientPredicate> ingredients) {
        this(List.of(), source, ingredients);
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
        return input.source;
    }

    @Override
    public boolean matches(Input input, Level level) {
        if (!this.source.test(input.source)) return false;
        if (input.size() < this.ingredients.size()) return false;
        for (int i = 0; i < this.ingredients.size(); i++) {
            if (!this.ingredients.get(i).test(input.getItem(i))) return false;
        }
        for (int i = this.ingredients.size(); i < input.size(); i++) {
            if (!input.getItem(i).isEmpty()) return false;
        }
        return true;
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
        private ItemIngredientPredicate source = null;
        private final List<ItemIngredientPredicate> ingredients = new ArrayList<>();

        public Builder(HolderGetter<Item> items) {
            this.items = items;
        }

        public Builder withCondition(ICondition condition) {
            this.conditions.add(condition);
            return this;
        }

        public Builder requires(ItemIngredientPredicate.Builder ingredient) {
            this.ingredients.add(ingredient.build());
            return this;
        }

        public Builder requires(ItemLike item, int count) {
            return this.requires(ItemIngredientPredicate.of(item).withCount(count));
        }

        public Builder requires(ItemLike item) {
            return this.requires(item, 1);
        }

        public Builder requires(TagKey<Item> tag, int count) {
            return this.requires(ItemIngredientPredicate.of(this.items, tag).withCount(count));
        }

        public Builder requires(TagKey<Item> tag) {
            return this.requires(tag, 1);
        }

        public Builder source(ItemIngredientPredicate.Builder source) {
            this.source = source.build();
            return this;
        }

        public Builder source(ItemLike... sources) {
            return this.source(ItemIngredientPredicate.of(sources));
        }

        @Override
        public JewelCraftingRecipe buildRecipe() {
            return new JewelCraftingRecipe(this.conditions, this.source, this.ingredients);
        }

        @Override
        public void validate(Identifier id) {
            if (this.source == null) {
                throw new IllegalArgumentException("Recipe result must not be empty, RecipeId: " + id);
            }
            if (this.ingredients.isEmpty() || this.ingredients.size() > 4) {
                throw new IllegalArgumentException("Recipe ingredients size must in 1-4, RecipeId: " + id);
            }
        }

        @Override
        public String getType() {
            return "jewel_crafting";
        }

        @Override
        @SneakyThrows
        public ItemStackTemplate getResult() {
            throw new IllegalAccessException("Could not invoke 'JewelCraftingRecipe$Builder#getResult()'");
        }

        @Override
        @SneakyThrows
        public ResourceKey<Recipe<?>> defaultId() {
            throw new IllegalAccessException("Could not invoke 'JewelCraftingRecipe$Builder#defaultId()'");
        }
    }

}
