package dev.dubhe.anvilcraft.recipe.anvil;

import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.builder.AbstractRecipeBuilder;
import dev.dubhe.anvilcraft.util.CodecUtil;

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
import net.minecraft.world.level.block.Block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Contract;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

@Getter
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class ItemInjectRecipe implements Recipe<ItemInjectRecipe.Input> {
    public final NonNullList<Ingredient> ingredients;
    public final Block inputBlock;
    public final Block resultBlock;

    public ItemInjectRecipe(NonNullList<Ingredient> ingredients, Block inputBlock, Block resultBlock) {
        this.ingredients = ingredients;
        this.inputBlock = inputBlock;
        this.resultBlock = resultBlock;
    }

    @Contract(" -> new")
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.ITEM_INJECT_TYPE.get();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeTypes.ITEM_INJECT_SERIALIZER.get();
    }

    @Override
    public boolean canCraftInDimensions(int pWidth, int pHeight) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider pRegistries) {
        return resultBlock.asItem().getDefaultInstance();
    }

    @Override
    public ItemStack assemble(Input pInput, HolderLookup.Provider pRegistries) {
        return ItemStack.EMPTY;
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public boolean matches(Input input, Level pLevel) {
        Object2IntMap<Item> contents = new Object2IntOpenHashMap<>();
        Object2BooleanMap<Item> flags = new Object2BooleanOpenHashMap<>();
        for (ItemStack stack : input.items()) {
            contents.mergeInt(stack.getItem(), stack.getCount(), Integer::sum);
            flags.put(stack.getItem(), false);
        }
        for (Ingredient ingredient : ingredients) {
            for (Item item : contents.keySet()) {
                if (ingredient.test(new ItemStack(item))) {
                    contents.put(item, contents.getInt(item) - 1);
                    flags.put(item, true);
                }
            }
        }
        if (flags.values().stream().anyMatch(flag -> !flag)) {
            return false;
        }
        return contents.values().intStream().allMatch(count -> count >= 0);
    }

    public record Input(List<ItemStack> items, Block inputBlock) implements RecipeInput {

        @Override
        public ItemStack getItem(int i) {
            return items.get(i);
        }

        @Override
        public int size() {
            return items.size();
        }
    }

    public static class Serializer implements RecipeSerializer<ItemInjectRecipe> {
        private static final MapCodec<ItemInjectRecipe> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
                        CodecUtil.createIngredientListCodec("ingredients", 9, "item_inject")
                                .forGetter(ItemInjectRecipe::getIngredients),
                        CodecUtil.BLOCK_CODEC.fieldOf("input_block").forGetter(ItemInjectRecipe::getInputBlock),
                        CodecUtil.BLOCK_CODEC.fieldOf("result_block").forGetter(ItemInjectRecipe::getResultBlock))
                .apply(ins, ItemInjectRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, ItemInjectRecipe> STREAM_CODEC =
                StreamCodec.of(Serializer::encode, Serializer::decode);

        @Override
        public MapCodec<ItemInjectRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ItemInjectRecipe> streamCodec() {
            return STREAM_CODEC;
        }

        private static void encode(RegistryFriendlyByteBuf buf, ItemInjectRecipe recipe) {
            buf.writeVarInt(recipe.ingredients.size());
            for (Ingredient ingredient : recipe.ingredients) {
                Ingredient.CONTENTS_STREAM_CODEC.encode(buf, ingredient);
            }
            CodecUtil.BLOCK_STREAM_CODEC.encode(buf, recipe.inputBlock);
            CodecUtil.BLOCK_STREAM_CODEC.encode(buf, recipe.resultBlock);
        }

        private static ItemInjectRecipe decode(RegistryFriendlyByteBuf buf) {
            int size = buf.readVarInt();
            NonNullList<Ingredient> ingredients = NonNullList.withSize(size, Ingredient.EMPTY);
            ingredients.replaceAll(i -> Ingredient.CONTENTS_STREAM_CODEC.decode(buf));
            Block inputBlock = CodecUtil.BLOCK_STREAM_CODEC.decode(buf);
            Block resultBlock = CodecUtil.BLOCK_STREAM_CODEC.decode(buf);
            return new ItemInjectRecipe(ingredients, inputBlock, resultBlock);
        }
    }

    @Setter
    @Accessors(fluent = true, chain = true)
    public static class Builder extends AbstractRecipeBuilder<ItemInjectRecipe> {
        private NonNullList<Ingredient> ingredients = NonNullList.create();
        private Block inputBlock;
        private Block resultBlock;

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
        public ItemInjectRecipe buildRecipe() {
            return new ItemInjectRecipe(ingredients, inputBlock, resultBlock);
        }

        @Override
        public void validate(ResourceLocation pId) {
            if (ingredients.isEmpty() || ingredients.size() > 9) {
                throw new IllegalArgumentException("Recipe ingredients size must in 0-9, RecipeId: " + pId);
            }
            if (inputBlock == null) {
                throw new IllegalArgumentException("Recipe input block must not be null, RecipeId: " + pId);
            }
            if (resultBlock == null) {
                throw new IllegalArgumentException("Recipe result block must not be null, RecipeId: " + pId);
            }
        }

        @Override
        public String getType() {
            return "item_inject";
        }

        @Override
        public Item getResult() {
            return resultBlock.asItem();
        }
    }
}
