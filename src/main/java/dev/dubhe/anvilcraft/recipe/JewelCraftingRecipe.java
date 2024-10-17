package dev.dubhe.anvilcraft.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.builder.AbstractItemProcessBuilder;
import dev.dubhe.anvilcraft.recipe.anvil.builder.AbstractRecipeBuilder;
import dev.dubhe.anvilcraft.recipe.anvil.input.IItemsInput;
import dev.dubhe.anvilcraft.util.CodecUtil;
import dev.dubhe.anvilcraft.util.RecipeUtil;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Contract;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@Getter
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class JewelCraftingRecipe implements Recipe<JewelCraftingRecipe.Input> {
    public final NonNullList<Ingredient> ingredients;
    public final ItemStack result;
    public final List<Object2IntMap.Entry<Ingredient>> mergedIngredients;
    public Input cache;
    public int cache_times;

    public JewelCraftingRecipe(NonNullList<Ingredient> ingredients, ItemStack result) {
        this.ingredients = ingredients;
        this.result = result;
        this.mergedIngredients = RecipeUtil.mergeIngredient(ingredients);
        if (mergedIngredients.size() > 4) {
            throw new IllegalArgumentException("Too many different ingredients");
        }
    }

    @Contract(" -> new")
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.JEWEL_CRAFTING_TYPE.get();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeTypes.JEWEL_CRAFTING_SERIALIZER.get();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result;
    }

    @Override
    public ItemStack assemble(Input input, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean matches(Input input, Level level) {
        if (input == cache) {
            return cache_times >= 1;
        }
        int times = RecipeUtil.getMaxCraftTime(input, ingredients);
        cache = input;
        cache_times = times;
        return cache_times >= 1;
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

    public static class Serializer implements RecipeSerializer<JewelCraftingRecipe> {

        private static final MapCodec<JewelCraftingRecipe> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
            CodecUtil.createIngredientListCodec("ingredients", 256, "jewel_crafting").forGetter(JewelCraftingRecipe::getIngredients),
            ItemStack.CODEC.fieldOf("result").forGetter(JewelCraftingRecipe::getResult)
        ).apply(ins, JewelCraftingRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, JewelCraftingRecipe> STREAM_CODEC = StreamCodec.of(
            Serializer::encode, Serializer::decode
        );

        @Override
        public MapCodec<JewelCraftingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, JewelCraftingRecipe> streamCodec() {
            return STREAM_CODEC;
        }

        private static void encode(RegistryFriendlyByteBuf buf, JewelCraftingRecipe recipe) {
            buf.writeVarInt(recipe.ingredients.size());
            for (Ingredient ingredient : recipe.ingredients) {
                Ingredient.CONTENTS_STREAM_CODEC.encode(buf, ingredient);
            }
            ItemStack.STREAM_CODEC.encode(buf, recipe.result);
        }

        private static JewelCraftingRecipe decode(RegistryFriendlyByteBuf buf) {
            int size = buf.readVarInt();
            NonNullList<Ingredient> ingredients = NonNullList.withSize(size, Ingredient.EMPTY);
            ingredients.replaceAll(i -> Ingredient.CONTENTS_STREAM_CODEC.decode(buf));
            ItemStack result = ItemStack.STREAM_CODEC.decode(buf);
            return new JewelCraftingRecipe(ingredients, result);
        }
    }
    
    @Setter
    @Accessors(fluent = true, chain = true)
    public static class Builder extends AbstractRecipeBuilder<JewelCraftingRecipe> {
        private NonNullList<Ingredient> ingredients = NonNullList.create();
        private ItemStack result = ItemStack.EMPTY;

        public Builder requires(Ingredient ingredient, int count) {
            for (int i = 0; i < count; i++) {
                this.ingredients.add(ingredient);
            }
            return this;
        }

        public Builder requires(Ingredient ingredient) {
            return requires(ingredient, 1);
        }

        public Builder requires(ItemLike pItem, int count) {
            return requires(Ingredient.of(pItem), count);
        }

        public Builder requires(ItemLike pItem) {
            return requires(pItem, 1);
        }

        public Builder requires(TagKey<Item> pTag, int count) {
            return requires(Ingredient.of(pTag), count);
        }

        public Builder requires(TagKey<Item> pTag) {
            return requires(pTag, 1);
        }
        
        @Override
        public JewelCraftingRecipe buildRecipe() {
            return new JewelCraftingRecipe(ingredients, result);
        }

        @Override
        public void validate(ResourceLocation pId) {
            if (ingredients.isEmpty() || ingredients.size() > 256) {
                throw new IllegalArgumentException("Recipe ingredients size must in 0-256, RecipeId: " + pId);
            }
            if (result.isEmpty()) {
                throw new IllegalArgumentException("Recipe result must not be empty, RecipeId: " + pId);
            }
        }

        @Override
        public String getType() {
            return "jewel_crafting";
        }

        @Override
        public Item getResult() {
            return result.getItem();
        }
    }

}
