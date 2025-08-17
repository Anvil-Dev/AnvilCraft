package dev.dubhe.anvilcraft.recipe.anvil;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.builder.AbstractRecipeBuilder;
import dev.dubhe.anvilcraft.recipe.anvil.input.ItemProcessInput;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.components.ChanceItemStack;
import dev.dubhe.anvilcraft.util.CodecUtil;
import dev.dubhe.anvilcraft.util.CollectionUtil;
import dev.dubhe.anvilcraft.util.RecipeUtil;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import lombok.Getter;
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
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Getter
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class StampingUniqueItemsRecipe implements Recipe<ItemProcessInput> {
    public final NonNullList<Ingredient> ingredients;
    public final List<Object2IntMap.Entry<Ingredient>> mergedIngredients;
    public final List<ChanceItemStack> results;
    public final boolean isSimple;
    protected ItemProcessInput cacheInput;
    protected int cacheMaxCraftTime = -1;

    public StampingUniqueItemsRecipe(
        NonNullList<Ingredient> ingredients,
        List<ChanceItemStack> results
    ) {
        this.ingredients = ingredients;
        this.mergedIngredients = RecipeUtil.mergeIngredient(ingredients);
        this.results = results;
        this.isSimple = ingredients.stream().allMatch(Ingredient::isSimple);
    }

    public static Builder builderUnique() {
        return new Builder();
    }

    @Override
    public boolean canCraftInDimensions(int pWidth, int pHeight) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider pRegistries) {
        return results.isEmpty() ? ItemStack.EMPTY : results.getFirst().getStack();
    }

    @Override
    public ItemStack assemble(ItemProcessInput pInput, HolderLookup.Provider pRegistries) {
        return results.isEmpty() ? ItemStack.EMPTY : results.getFirst().getStack();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public boolean matches(ItemProcessInput pInput, Level pLevel) {
        if (pInput.items().size() != Set.copyOf(pInput.items()).size()) return false;
        if (pInput.items().size() != this.ingredients.size()) return false;
        if (!CollectionUtil.allMatch(pInput.items(), itemStack -> itemStack.getCount() == 1)) return false;

        for (int i = 0; i < pInput.size(); i++) {
            if (!this.ingredients.get(i).test(pInput.getItem(i))) return false;
        }

        return true;
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.STAMPING_UNIQUE_ITEMS_TYPE.get();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeTypes.STAMPING_UNIQUE_ITEMS_SERIALIZER.get();
    }

    public int getMaxCraftTime() {
        return 1;
    }

    public static class Serializer implements RecipeSerializer<StampingUniqueItemsRecipe> {
        private static final MapCodec<StampingUniqueItemsRecipe> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
                CodecUtil.createIngredientListCodec("ingredients", 9, "stamping_unique_items")
                    .forGetter(StampingUniqueItemsRecipe::getIngredients),
                ChanceItemStack.CODEC.listOf().fieldOf("results").forGetter(StampingUniqueItemsRecipe::getResults))
            .apply(ins, StampingUniqueItemsRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, StampingUniqueItemsRecipe> STREAM_CODEC =
            StreamCodec.of(Serializer::encode, Serializer::decode);

        @Override
        public MapCodec<StampingUniqueItemsRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, StampingUniqueItemsRecipe> streamCodec() {
            return STREAM_CODEC;
        }

        private static StampingUniqueItemsRecipe decode(RegistryFriendlyByteBuf buf) {
            List<ChanceItemStack> results = new ArrayList<>();
            int size = buf.readVarInt();
            for (int i = 0; i < size; i++) {
                results.add(ChanceItemStack.STREAM_CODEC.decode(buf));
            }
            size = buf.readVarInt();
            NonNullList<Ingredient> ingredients = NonNullList.withSize(size, Ingredient.EMPTY);
            ingredients.replaceAll(i -> Ingredient.CONTENTS_STREAM_CODEC.decode(buf));
            return new StampingUniqueItemsRecipe(ingredients, results);
        }

        private static void encode(RegistryFriendlyByteBuf buf, StampingUniqueItemsRecipe recipe) {
            buf.writeVarInt(recipe.results.size());
            for (ChanceItemStack stack : recipe.results) {
                ChanceItemStack.STREAM_CODEC.encode(buf, stack);
            }
            buf.writeVarInt(recipe.ingredients.size());
            for (Ingredient ingredient : recipe.ingredients) {
                Ingredient.CONTENTS_STREAM_CODEC.encode(buf, ingredient);
            }
        }
    }

    public static class Builder extends AbstractRecipeBuilder<StampingUniqueItemsRecipe> {
        protected NonNullList<Ingredient> ingredients = NonNullList.create();
        protected List<ChanceItemStack> results = new ArrayList<>();
        @Getter
        protected boolean generated = false;

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

        public Builder result(ChanceItemStack stack) {
            results.add(stack);
            return this;
        }

        public Builder result(ItemStack stack) {
            results.add(ChanceItemStack.of(stack));
            return this;
        }

        public Builder result(ItemLike item) {
            return this.result(item.asItem().getDefaultInstance());
        }

        public Builder result(ItemLike item, int count) {
            ItemStack stack = item.asItem().getDefaultInstance();
            stack.setCount(count);
            return this.result(stack);
        }

        public Builder result(ItemLike item, int count, float chance) {
            ItemStack stack = item.asItem().getDefaultInstance();
            stack.setCount(count);
            return this.result(ChanceItemStack.of(stack, chance));
        }

        @Override
        public void validate(ResourceLocation pId) {
            if (ingredients.isEmpty() || ingredients.size() > 9) {
                throw new IllegalArgumentException("Recipe ingredients size must in 0-9, RecipeId: " + pId);
            }
            if (results.isEmpty()) {
                throw new IllegalArgumentException("Recipe results must not be null, RecipeId: " + pId);
            }
        }

        @Override
        public Item getResult() {
            return results.getFirst().getStack().getItem();
        }

        @Override
        public StampingUniqueItemsRecipe buildRecipe() {
            return new StampingUniqueItemsRecipe(ingredients, results);
        }

        @Override
        public String getType() {
            return "stamping_unique_items";
        }
    }
}
