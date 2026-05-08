package dev.dubhe.anvilcraft.recipe;

import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.codec.CodecUtil;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeSerializers;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.builder.AbstractRecipeBuilder;
import dev.dubhe.anvilcraft.recipe.anvil.input.IItemsInput;
import dev.dubhe.anvilcraft.util.RecipeUtil;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
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

import java.util.ArrayList;
import java.util.List;

@Getter
public class JewelCraftingRecipe implements Recipe<JewelCraftingRecipe.Input> {
    public static final RecipeSerializer<JewelCraftingRecipe> SERIALIZER = new RecipeSerializer<>(
        RecordCodecBuilder.mapCodec(ins -> ins.group(
            ICondition.LIST_CODEC
                .optionalFieldOf("neoforge:conditions", new ArrayList<>())
                .forGetter(JewelCraftingRecipe::getConditions),
            CodecUtil.createIngredientListCodec("ingredients", 256, "jewel_crafting")
                .forGetter(JewelCraftingRecipe::getIngredients),
            ItemStackTemplate.CODEC
                .fieldOf("result")
                .forGetter(JewelCraftingRecipe::getResult)
        ).apply(ins, JewelCraftingRecipe::new)),
        StreamCodec.of(Serializer::encode, Serializer::decode)
    );
    public final List<ICondition> conditions;
    public final NonNullList<Ingredient> ingredients;
    public final ItemStackTemplate result;
    public final List<Object2IntMap.Entry<Ingredient>> mergedIngredients;
    public Input cache;
    public int cacheTimes;

    public JewelCraftingRecipe(List<ICondition> conditions, NonNullList<Ingredient> ingredients, ItemStackTemplate result) {
        this.conditions = conditions;
        this.ingredients = ingredients;
        this.result = result;
        this.mergedIngredients = RecipeUtil.mergeIngredient(ingredients);
        if (mergedIngredients.size() > 4) {
            throw new IllegalArgumentException("Too many different ingredients");
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
        return ModRecipeSerializers.JEWEL_CRAFTING.get();
    }

    @Override
    public ItemStack assemble(Input input) {
        return this.result.create();
    }

    @Override
    public boolean matches(Input input, Level level) {
        if (input == cache) {
            return cacheTimes >= 1;
        }
        int times = RecipeUtil.getMaxCraftTime(input, ingredients);
        cache = input;
        cacheTimes = times;
        return cacheTimes >= 1;
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
            return items.get(index);
        }

        @Override
        public int size() {
            return items.size();
        }
    }

    public static class Serializer {
        private static void encode(RegistryFriendlyByteBuf buf, JewelCraftingRecipe recipe) {
            buf.writeVarInt(recipe.ingredients.size());
            for (Ingredient ingredient : recipe.ingredients) {
                Ingredient.CONTENTS_STREAM_CODEC.encode(buf, ingredient);
            }
            ItemStackTemplate.STREAM_CODEC.encode(buf, recipe.result);
        }

        private static JewelCraftingRecipe decode(RegistryFriendlyByteBuf buf) {
            int size = buf.readVarInt();
            NonNullList<Ingredient> ingredients = NonNullList.withSize(size, null);
            ingredients.replaceAll(_ -> Ingredient.CONTENTS_STREAM_CODEC.decode(buf));
            ItemStackTemplate result = ItemStackTemplate.STREAM_CODEC.decode(buf);
            return new JewelCraftingRecipe(new ArrayList<>(), ingredients, result);
        }
    }

    @Setter
    @Accessors(fluent = true, chain = true)
    public static class Builder extends AbstractRecipeBuilder<JewelCraftingRecipe> {
        private final HolderGetter<Item> items;
        private List<ICondition> conditions = new ArrayList<>();
        private NonNullList<Ingredient> ingredients = NonNullList.create();
        private ItemStackTemplate result = new ItemStackTemplate(Items.AIR);

        public Builder(HolderGetter<Item> items) {
            this.items = items;
        }

        public Builder withCondition(ICondition condition) {
            this.conditions.add(condition);
            return this;
        }

        public Builder requires(Ingredient ingredient, int count) {
            for (int i = 0; i < count; i++) {
                this.ingredients.add(ingredient);
            }
            return this;
        }

        public Builder requires(Ingredient ingredient) {
            return requires(ingredient, 1);
        }

        public Builder requires(ItemLike item, int count) {
            return requires(Ingredient.of(item), count);
        }

        public Builder requires(ItemLike item) {
            return requires(item, 1);
        }

        public Builder requires(TagKey<Item> tag, int count) {
            return requires(Ingredient.of(this.items.getOrThrow(tag)), count);
        }

        public Builder requires(TagKey<Item> tag) {
            return requires(tag, 1);
        }

        public Builder result(ItemLike item) {
            this.result = new ItemStackTemplate(item.asItem());
            return this;
        }

        @Override
        public JewelCraftingRecipe buildRecipe() {
            return new JewelCraftingRecipe(conditions, ingredients, result);
        }

        @Override
        public void validate(Identifier id) {
            if (ingredients.isEmpty() || ingredients.size() > 256) {
                throw new IllegalArgumentException("Recipe ingredients size must in 0-256, RecipeId: " + id);
            }
            if (result.is(Items.AIR)) {
                throw new IllegalArgumentException("Recipe result must not be empty, RecipeId: " + id);
            }
        }

        @Override
        public String getType() {
            return "jewel_crafting";
        }

        @Override
        public ItemStackTemplate getResult() {
            return this.result;
        }
    }

}
