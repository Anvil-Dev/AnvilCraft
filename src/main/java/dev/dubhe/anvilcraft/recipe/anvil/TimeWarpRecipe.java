package dev.dubhe.anvilcraft.recipe.anvil;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.ChanceItemStack;
import dev.dubhe.anvilcraft.recipe.anvil.builder.AbstractRecipeBuilder;
import dev.dubhe.anvilcraft.recipe.anvil.input.IItemsInput;
import dev.dubhe.anvilcraft.util.CauldronUtil;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Getter
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class TimeWarpRecipe implements Recipe<TimeWarpRecipe.Input> {
    private final NonNullList<Ingredient> ingredients;
    private final List<Ingredient> exactIngredients;
    private final List<Object2IntMap.Entry<Ingredient>> mergedIngredients;
    private final Block cauldron;
    private final List<ChanceItemStack> results;
    private final boolean produceFluid;
    private final boolean consumeFluid;
    private final boolean isSimple;
    private final int requiredFluidLevel;
    private Input cacheInput;
    private int cacheMaxCraftTime;

    public TimeWarpRecipe(
        NonNullList<Ingredient> ingredients,
        Optional<List<Ingredient>> exactIngredients,
        Block cauldron,
        List<ChanceItemStack> results,
        boolean produceFluid,
        boolean consumeFluid,
        int requiredFluidLevel
    ) {
        this.ingredients = ingredients;
        this.mergedIngredients = RecipeUtil.mergeIngredient(ingredients);
        this.exactIngredients = exactIngredients.orElseGet(List::of);
        this.cauldron = cauldron;
        this.results = results;
        this.produceFluid = produceFluid;
        this.consumeFluid = consumeFluid;
        this.isSimple = ingredients.stream().allMatch(Ingredient::isSimple);
        this.requiredFluidLevel = requiredFluidLevel;
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
        return results.isEmpty() ? ItemStack.EMPTY : results.getFirst().getStack();
    }

    @Override
    public ItemStack assemble(Input input, HolderLookup.Provider provider) {
        return results.isEmpty() ? ItemStack.EMPTY : results.getFirst().getStack();
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public boolean matches(Input input, Level level) {
        if (this.requiredFluidLevel > 0 && !CauldronUtil.compatibleForDrain(input.cauldronState, this.cauldron, this.requiredFluidLevel)) {
            return false;
        }
        if (this.produceFluid && !CauldronUtil.compatibleForFill(input.cauldronState, this.cauldron, this.requiredFluidLevel)) {
            return false;
        }
        int normalCraftCount = getMaxCraftTime(input);
        if (exactIngredients.isEmpty()) {
            return normalCraftCount >= 1;
        }
        int exactCraftCount = getMaxCraftTime(input, exactIngredients);
        return exactCraftCount >= 1 && normalCraftCount == exactCraftCount;
    }

    public int getMaxCraftTime(Input pInput, List<Ingredient> ingredient) {
        int times = RecipeUtil.getMaxCraftTime(pInput, ingredient);
        if (produceFluid || consumeFluid) {
            times = Math.min(times, 1);
        }
        return times;
    }

    @SuppressWarnings("DuplicatedCode")
    public int getMaxCraftTime(Input pInput) {
        if (cacheInput == pInput) {
            return cacheMaxCraftTime;
        }
        int times = RecipeUtil.getMaxCraftTime(pInput, ingredients);
        if (produceFluid || consumeFluid) {
            times = Math.min(times, 1);
        }
        cacheInput = pInput;
        cacheMaxCraftTime = times < AnvilCraft.config.anvilEfficiency ? times : AnvilCraft.config.anvilEfficiency;
        return cacheMaxCraftTime;
    }

    public record Input(List<ItemStack> items, BlockState cauldronState) implements RecipeInput, IItemsInput {

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
                CodecUtil.createIngredientListCodec("ingredients", 64, "time_warp")
                    .forGetter(TimeWarpRecipe::getIngredients),
                Ingredient.CODEC_NONEMPTY
                    .listOf()
                    .optionalFieldOf("exactIngredients")
                    .forGetter(o -> o.exactIngredients.isEmpty() ? Optional.empty() : Optional.of(o.exactIngredients)),
                CodecUtil.BLOCK_CODEC.fieldOf("cauldron").forGetter(TimeWarpRecipe::getCauldron),
                ChanceItemStack.CODEC.listOf()
                    .optionalFieldOf("results", List.of())
                    .forGetter(TimeWarpRecipe::getResults),
                Codec.BOOL.fieldOf("produce_fluid").forGetter(TimeWarpRecipe::isProduceFluid),
                Codec.BOOL.fieldOf("consume_fluid").forGetter(TimeWarpRecipe::isConsumeFluid),
                Codec.INT.optionalFieldOf("requiredFluidLevel", 0).forGetter(TimeWarpRecipe::getRequiredFluidLevel)
            )
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
            buf.writeVarInt(recipe.exactIngredients.size());
            for (Ingredient ingredient : recipe.exactIngredients) {
                Ingredient.CONTENTS_STREAM_CODEC.encode(buf, ingredient);
            }
            CodecUtil.BLOCK_STREAM_CODEC.encode(buf, recipe.getCauldron());
            buf.writeVarInt(recipe.results.size());
            for (ChanceItemStack stack : recipe.results) {
                ChanceItemStack.STREAM_CODEC.encode(buf, stack);
            }
            buf.writeBoolean(recipe.produceFluid);
            buf.writeBoolean(recipe.consumeFluid);
            buf.writeInt(recipe.requiredFluidLevel);
        }

        private static TimeWarpRecipe decode(RegistryFriendlyByteBuf buf) {
            int size = buf.readVarInt();
            NonNullList<Ingredient> ingredients = NonNullList.withSize(size, Ingredient.EMPTY);
            ingredients.replaceAll(i -> Ingredient.CONTENTS_STREAM_CODEC.decode(buf));
            List<Ingredient> exactIngredients = new ArrayList<>();
            size = buf.readVarInt();
            for (int i = 0; i < size; i++) {
                exactIngredients.add(Ingredient.CONTENTS_STREAM_CODEC.decode(buf));
            }
            Block cauldron = CodecUtil.BLOCK_STREAM_CODEC.decode(buf);
            size = buf.readVarInt();
            List<ChanceItemStack> results = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                results.add(ChanceItemStack.STREAM_CODEC.decode(buf));
            }
            boolean produceFluid = buf.readBoolean();
            boolean consumeFluid = buf.readBoolean();
            int requiredFluidLevel = buf.readInt();
            return new TimeWarpRecipe(
                ingredients,
                Optional.of(exactIngredients),
                cauldron,
                results,
                produceFluid,
                consumeFluid,
                requiredFluidLevel
            );
        }
    }

    @Setter
    @Accessors(fluent = true, chain = true)
    public static class Builder extends AbstractRecipeBuilder<TimeWarpRecipe> {

        private NonNullList<Ingredient> ingredients = NonNullList.create();
        private List<Ingredient> exactIngredients = new ArrayList<>();
        private Block cauldron = Blocks.CAULDRON;
        private List<ChanceItemStack> results = new ArrayList<>();
        private boolean produceFluid = false;
        private boolean consumeFluid = false;
        private int requiredFluidLevel = 0;

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

        public Builder requiresExactly(ItemLike item, int count) {
            for (int i = 0; i < count; i++) {
                exactIngredients.add(Ingredient.of(item));
            }
            return this;
        }

        public Builder requiresExactly(TagKey<Item> item, int count) {
            for (int i = 0; i < count; i++) {
                exactIngredients.add(Ingredient.of(item));
            }
            return this;
        }


        public Builder requiresExactly(ItemLike item) {
            exactIngredients.add(Ingredient.of(item));
            return this;
        }

        public Builder requiresExactly(TagKey<Item> item) {
            exactIngredients.add(Ingredient.of(item));
            return this;
        }

        public Builder result(ItemStack stack) {
            results.add(ChanceItemStack.of(stack));
            return this;
        }

        public Builder result(ItemStack stack, float chance) {
            results.add(ChanceItemStack.of(stack).withChance(chance));
            return this;
        }

        public Builder result(ChanceItemStack stack) {
            results.add(stack);
            return this;
        }

        @Override
        public TimeWarpRecipe buildRecipe() {
            if (consumeFluid) {
                requiredFluidLevel = Math.max(requiredFluidLevel, 1);
            }
            return new TimeWarpRecipe(ingredients,
                Optional.of(exactIngredients),
                cauldron,
                results,
                produceFluid,
                consumeFluid,
                requiredFluidLevel
            );
        }

        @Override
        public void validate(ResourceLocation pId) {
            if (ingredients.isEmpty() || ingredients.size() > 64) {
                throw new IllegalArgumentException("Recipe ingredients size must in 0-64, RecipeId: " + pId);
            }
            if (cauldron == null) {
                throw new IllegalArgumentException("Recipe cauldron must not be null, RecipeId: " + pId);
            }
            if (results.isEmpty() && !produceFluid) {
                throw new IllegalArgumentException(
                    "Recipe must produce any item or cauldron content, RecipeId: " + pId);
            }
        }

        @Override
        public String getType() {
            return "time_warp";
        }

        @Override
        public Item getResult() {
            if (results.isEmpty()) {
                return cauldron.asItem();
            }
            return results.getFirst().getStack().getItem();
        }
    }
}
