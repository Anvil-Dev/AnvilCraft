package dev.dubhe.anvilcraft.recipe;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.builder.AbstractRecipeBuilder;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
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

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@Getter
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ItemCrushRecipe implements Recipe<ItemCrushRecipe.Input> {
    public final NonNullList<Ingredient> ingredients;
    public final ItemStack result;
    public final boolean isSimple;
    private Input cacheInput;
    private int cacheMaxCraftTime = -1;

    public ItemCrushRecipe(NonNullList<Ingredient> ingredients, ItemStack result) {
        this.ingredients = ingredients;
        this.result = result;
        this.isSimple = ingredients.stream().allMatch(Ingredient::isSimple);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.ITEM_CRUSH_TYPE.get();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeTypes.ITEM_CRUSH_SERIALIZERS.get();
    }

    @Override
    public boolean canCraftInDimensions(int pWidth, int pHeight) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider pRegistries) {
        return result;
    }

    @Override
    public boolean matches(Input pInput, Level pLevel) {
        return getMaxCraftTime(pInput) > 0;
    }

    @Override
    public ItemStack assemble(Input pInput, HolderLookup.Provider pRegistries) {
        return this.result.copy();
    }

    public int getMaxCraftTime(Input pInput) {
        if (cacheInput == pInput) {
            return cacheMaxCraftTime;
        }
        Object2IntMap<Item> contents = new Object2IntOpenHashMap<>();
        Object2BooleanMap<Item> flags = new Object2BooleanOpenHashMap<>();
        for (ItemStack stack : pInput.items()) {
            contents.mergeInt(stack.getItem(), stack.getCount(), Integer::sum);
            flags.put(stack.getItem(), false);
        }
        int times = 0;
        while (true) {
            for (Ingredient ingredient : ingredients) {
                for (Item item : contents.keySet()) {
                    if (ingredient.test(new ItemStack(item))) {
                        contents.put(item, contents.getInt(item) - 1);
                        flags.put(item, true);
                    }
                }
            }
            if (flags.values().stream().anyMatch(flag -> !flag)) {
                cacheInput = pInput;
                cacheMaxCraftTime = 0;
                return 0;
            }
            if (contents.values().intStream().allMatch(i -> i >= 0)) {
                times += 1;
            } else {
                cacheInput = pInput;
                cacheMaxCraftTime = times;
                return times;
            }
        }
    }

    public record Input(List<ItemStack> items) implements RecipeInput {

        @Override
        public ItemStack getItem(int pIndex) {
            return items.get(pIndex);
        }

        @Override
        public int size() {
            return items.size();
        }
    }

    public static class Serializer implements RecipeSerializer<ItemCrushRecipe> {
        private static final MapCodec<ItemCrushRecipe> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
                Ingredient.CODEC_NONEMPTY
                        .listOf(1, 9)
                        .fieldOf("ingredients")
                        .flatXmap(i -> {
                            Ingredient[] ingredients = i.toArray(Ingredient[]::new);
                            if (ingredients.length == 0) {
                                return DataResult.error(() -> "No ingredients for item_crush recipe");
                            } else {
                                return ingredients.length > 9 ?
                                        DataResult.error(() -> "Too many ingredients for item_crush recipe. The maximum is: 9") :
                                        DataResult.success(NonNullList.of(Ingredient.EMPTY, ingredients));
                            }
                        }, DataResult::success)
                        .forGetter(ItemCrushRecipe::getIngredients),
                ItemStack.CODEC.fieldOf("result").forGetter(ItemCrushRecipe::getResult)
        ).apply(ins, ItemCrushRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, ItemCrushRecipe> STREAM_CODEC = StreamCodec.of(
                Serializer::encode, Serializer::decode
        );

        @Override
        public MapCodec<ItemCrushRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ItemCrushRecipe> streamCodec() {
            return STREAM_CODEC;
        }

        private static ItemCrushRecipe decode(RegistryFriendlyByteBuf buf) {
            ItemStack result = ItemStack.STREAM_CODEC.decode(buf);
            int size = buf.readVarInt();
            NonNullList<Ingredient> ingredients = NonNullList.withSize(size, Ingredient.EMPTY);
            ingredients.replaceAll(i -> Ingredient.CONTENTS_STREAM_CODEC.decode(buf));
            return new ItemCrushRecipe(ingredients, result);
        }

        private static void encode(RegistryFriendlyByteBuf buf, ItemCrushRecipe recipe) {
            ItemStack.STREAM_CODEC.encode(buf, recipe.result);
            buf.writeVarInt(recipe.ingredients.size());
            for (Ingredient ingredient : recipe.ingredients) {
                Ingredient.CONTENTS_STREAM_CODEC.encode(buf, ingredient);
            }
        }
    }

    @Setter
    @Accessors(fluent = true, chain = true)
    public static class Builder extends AbstractRecipeBuilder<ItemCrushRecipe> {
        private NonNullList<Ingredient> ingredients = NonNullList.create();
        private ItemStack result;

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
        public ItemCrushRecipe buildRecipe() {
            return new ItemCrushRecipe(ingredients, result);
        }

        @Override
        public void validate(ResourceLocation pId) {
            if (ingredients.isEmpty() || ingredients.size() > 9) {
                throw new IllegalArgumentException("Recipe ingredients size must in 0-9, RecipeId: " + pId);
            }
            if (result == null) {
                throw new IllegalArgumentException("Recipe result must not be null, RecipeId: " + pId);
            }
        }

        @Override
        public Item getResult() {
            return result.getItem();
        }
    }
}
