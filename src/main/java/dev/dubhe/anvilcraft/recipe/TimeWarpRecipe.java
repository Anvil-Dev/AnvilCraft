package dev.dubhe.anvilcraft.recipe;

import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.builder.AbstractRecipeBuilder;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
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
import org.jetbrains.annotations.NotNull;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

@Getter
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class TimeWarpRecipe implements Recipe<TimeWarpRecipe.Input> {
    public final NonNullList<Ingredient> ingredients;
    public final Block cauldron;
    public final ItemStack result;
    public final boolean produceFluid;
    public final boolean consumeFluid;
    public final boolean isSimple;
    private Input cacheInput;
    private int cacheMaxCraftTime;

    public TimeWarpRecipe(
            NonNullList<Ingredient> ingredients,
            Block cauldron,
            ItemStack result,
            boolean produceFluid,
            boolean consumeFluid) {
        this.ingredients = ingredients;
        this.cauldron = cauldron;
        this.result = result;
        this.produceFluid = produceFluid;
        this.consumeFluid = consumeFluid;
        this.isSimple = ingredients.stream().allMatch(Ingredient::isSimple);
    }

    @Contract(" -> new")
    public static @NotNull Builder builder() {
        return new Builder();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.TIME_WARP_TYPE.get();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeTypes.TIME_WARP_SERIALIZER.get();
    }

    @Override
    public boolean canCraftInDimensions(int i, int i1) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider provider) {
        return result;
    }

    @Override
    public ItemStack assemble(Input input, HolderLookup.Provider provider) {
        return result.copy();
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public boolean matches(Input input, Level level) {

        if (consumeFluid) {
            if (!input.cauldronState.is(cauldron)) {
                return false;
            }
        }
        if (produceFluid) {
            if (!input.cauldronState.is(cauldron) && !input.cauldronState.is(Blocks.CAULDRON)) {
                return false;
            }
            if (input.cauldronState.is(cauldron)) {
                if (input.cauldronState.hasProperty(LayeredCauldronBlock.LEVEL)) {
                    if (input.cauldronState.getValue(LayeredCauldronBlock.LEVEL) >= 3) {
                        return false;
                    }
                } else {
                    return false;
                }
            }
        }
        return getMaxCraftTime(input) >= 1;
    }

    @SuppressWarnings("DuplicatedCode")
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
                if (produceFluid || consumeFluid) {
                    return times >= 1 ? 1 : 0;
                } else {
                    cacheInput = pInput;
                    cacheMaxCraftTime = times;
                    return times;
                }
            }
        }
    }

    public record Input(List<ItemStack> items, BlockState cauldronState) implements RecipeInput {

        @Override
        public ItemStack getItem(int i) {
            return items.get(i);
        }

        @Override
        public int size() {
            return items.size();
        }
    }

    public static class Serializer implements RecipeSerializer<TimeWarpRecipe> {
        private static final MapCodec<TimeWarpRecipe> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
                        Ingredient.CODEC_NONEMPTY
                                .listOf(1, 64)
                                .fieldOf("ingredients")
                                .flatXmap(
                                        i -> {
                                            Ingredient[] ingredients = i.toArray(Ingredient[]::new);
                                            if (ingredients.length == 0) {
                                                return DataResult.error(() -> "No ingredients for time_warp recipe");
                                            } else {
                                                return ingredients.length > 64
                                                        ? DataResult.error(
                                                                () ->
                                                                        "Too many ingredients for time_warp recipe. The maximum is: 64")
                                                        : DataResult.success(
                                                                NonNullList.of(Ingredient.EMPTY, ingredients));
                                            }
                                        },
                                        DataResult::success)
                                .forGetter(TimeWarpRecipe::getIngredients),
                        Codec.STRING
                                .fieldOf("cauldron")
                                .flatXmap(
                                        i -> DataResult.success(BuiltInRegistries.BLOCK.get(ResourceLocation.parse(i))),
                                        b -> DataResult.success(BuiltInRegistries.BLOCK
                                                .getKey(b)
                                                .toString()))
                                .forGetter(TimeWarpRecipe::getCauldron),
                        ItemStack.OPTIONAL_CODEC
                                .optionalFieldOf("result", ItemStack.EMPTY)
                                .forGetter(TimeWarpRecipe::getResult),
                        Codec.BOOL.fieldOf("produce_fluid").forGetter(TimeWarpRecipe::isProduceFluid),
                        Codec.BOOL.fieldOf("consume_fluid").forGetter(TimeWarpRecipe::isConsumeFluid))
                .apply(ins, TimeWarpRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, TimeWarpRecipe> STREAM_CODEC =
                StreamCodec.of(Serializer::encode, Serializer::decode);

        @Override
        public MapCodec<TimeWarpRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, TimeWarpRecipe> streamCodec() {
            return STREAM_CODEC;
        }

        private static void encode(RegistryFriendlyByteBuf buf, TimeWarpRecipe recipe) {
            buf.writeVarInt(recipe.ingredients.size());
            for (Ingredient ingredient : recipe.ingredients) {
                Ingredient.CONTENTS_STREAM_CODEC.encode(buf, ingredient);
            }
            buf.writeUtf(BuiltInRegistries.BLOCK.getKey(recipe.cauldron).toString());
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, recipe.result);
            buf.writeBoolean(recipe.produceFluid);
            buf.writeBoolean(recipe.consumeFluid);
        }

        private static TimeWarpRecipe decode(RegistryFriendlyByteBuf buf) {
            int size = buf.readVarInt();
            NonNullList<Ingredient> ingredients = NonNullList.withSize(size, Ingredient.EMPTY);
            ingredients.replaceAll(i -> Ingredient.CONTENTS_STREAM_CODEC.decode(buf));
            Block cauldron = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(buf.readUtf()));
            ItemStack result = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
            boolean needFluid = buf.readBoolean();
            boolean isConsume = buf.readBoolean();
            return new TimeWarpRecipe(ingredients, cauldron, result, needFluid, isConsume);
        }
    }

    @Setter
    @Accessors(fluent = true, chain = true)
    public static class Builder extends AbstractRecipeBuilder<TimeWarpRecipe> {

        private NonNullList<Ingredient> ingredients = NonNullList.create();
        private Block cauldron = Blocks.CAULDRON;
        private ItemStack result;
        private boolean produceFluid = false;
        private boolean consumeFluid = false;

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
        public TimeWarpRecipe buildRecipe() {
            if (result == null) {
                result = ItemStack.EMPTY;
            }
            return new TimeWarpRecipe(ingredients, cauldron, result, produceFluid, consumeFluid);
        }

        @Override
        public void validate(ResourceLocation pId) {
            if (ingredients.isEmpty() || ingredients.size() > 64) {
                throw new IllegalArgumentException("Recipe ingredients size must in 0-64, RecipeId: " + pId);
            }
            if (cauldron == null) {
                throw new IllegalArgumentException("Recipe cauldron must not be null, RecipeId: " + pId);
            }
            if (result == null && !produceFluid) {
                throw new IllegalArgumentException(
                        "Recipe result must not be null when need fluid is false, RecipeId: " + pId);
            }
        }

        @Override
        public String getType() {
            return "time_warp";
        }

        @Override
        public Item getResult() {
            if (result == null) {
                return cauldron.asItem();
            }
            return result.getItem();
        }
    }
}
