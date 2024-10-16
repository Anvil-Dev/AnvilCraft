package dev.dubhe.anvilcraft.recipe.anvil;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.builder.AbstractRecipeBuilder;
import dev.dubhe.anvilcraft.recipe.anvil.input.ItemProcessInput;
import dev.dubhe.anvilcraft.util.CodecUtil;
import dev.dubhe.anvilcraft.util.RecipeUtil;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

@Getter
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class ConcreteRecipe implements Recipe<ItemProcessInput> {
    public final NonNullList<Ingredient> ingredients;
    public final List<Object2IntMap.Entry<Ingredient>> mergedIngredients;
    public final String result;
    public final int resultCount;
    public final boolean isSimple;
    private ItemProcessInput cacheInput;
    private int cacheMaxCraftTime = -1;

    public ConcreteRecipe(NonNullList<Ingredient> ingredients, String result, int resultCount) {
        this.ingredients = ingredients;
        this.mergedIngredients = RecipeUtil.mergeIngredient(ingredients);
        this.result = result;
        this.resultCount = resultCount;
        this.isSimple = ingredients.stream().allMatch(Ingredient::isSimple);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.CONCRETE_TYPE.get();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeTypes.CONCRETE_SERIALIZER.get();
    }

    @Override
    public boolean canCraftInDimensions(int i, int i1) {
        return true;
    }

    @Override
    public ItemStack assemble(ItemProcessInput itemProcessInput, HolderLookup.Provider provider) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider provider) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean matches(ItemProcessInput input, Level level) {
        return getMaxCraftTime(input) >= 1;
    }

    @SuppressWarnings("DuplicatedCode")
    public int getMaxCraftTime(ItemProcessInput pInput) {
        if (cacheInput == pInput) {
            return cacheMaxCraftTime;
        }
        int times = RecipeUtil.getMaxCraftTime(pInput, ingredients);
        cacheInput = pInput;
        cacheMaxCraftTime = times <= AnvilCraft.config.anvilEfficiency ? times : AnvilCraft.config.anvilEfficiency;
        return cacheMaxCraftTime;
    }

    public static class Serializer implements RecipeSerializer<ConcreteRecipe> {
        private static final MapCodec<ConcreteRecipe> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
                        CodecUtil.createIngredientListCodec("ingredients", 16, "concrete")
                                .forGetter(ConcreteRecipe::getIngredients),
                        Codec.STRING.fieldOf("result").forGetter(ConcreteRecipe::getResult),
                        Codec.INT.fieldOf("resultCount").forGetter(ConcreteRecipe::getResultCount))
                .apply(ins, ConcreteRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, ConcreteRecipe> STREAM_CODEC =
                StreamCodec.of(Serializer::encode, Serializer::decode);

        @Override
        public MapCodec<ConcreteRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ConcreteRecipe> streamCodec() {
            return STREAM_CODEC;
        }

        private static ConcreteRecipe decode(RegistryFriendlyByteBuf buf) {
            int size = buf.readVarInt();
            NonNullList<Ingredient> ingredients = NonNullList.withSize(size, Ingredient.EMPTY);
            ingredients.replaceAll(i -> Ingredient.CONTENTS_STREAM_CODEC.decode(buf));
            String result = buf.readUtf();
            int resultCount = buf.readVarInt();
            return new ConcreteRecipe(ingredients, result, resultCount);
        }

        private static void encode(RegistryFriendlyByteBuf buf, ConcreteRecipe recipe) {
            buf.writeVarInt(recipe.ingredients.size());
            for (Ingredient ingredient : recipe.ingredients) {
                Ingredient.CONTENTS_STREAM_CODEC.encode(buf, ingredient);
            }
            buf.writeUtf(recipe.result);
            buf.writeVarInt(recipe.resultCount);
        }
    }

    @Setter
    @Accessors(fluent = true, chain = true)
    public static class Builder extends AbstractRecipeBuilder<ConcreteRecipe> {
        private NonNullList<Ingredient> ingredients = NonNullList.create();
        private String result;
        private int resultCount = 1;

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
        public ConcreteRecipe buildRecipe() {
            return new ConcreteRecipe(ingredients, result, resultCount);
        }

        @Override
        public void validate(ResourceLocation pId) {
            if (ingredients.isEmpty() || ingredients.size() > 16) {
                throw new IllegalArgumentException("Recipe ingredients size must in 1-16, RecipeId: " + pId);
            }
            if (result == null || result.isEmpty()) {
                throw new IllegalArgumentException("Recipe result must not be empty, RecipeId: " + pId);
            }
            if (resultCount <= 0) {
                throw new IllegalArgumentException("Recipe resultCount must be > 0, RecipeId: " + pId);
            }
        }

        @Override
        public String getType() {
            return "concrete";
        }

        @Override
        public Item getResult() {
            return Items.AIR;
        }

        @Override
        public void save(RecipeOutput recipeOutput) {
            throw new UnsupportedOperationException();
        }
    }
}
