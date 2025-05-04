package dev.dubhe.anvilcraft.recipe.multiple;

import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import dev.dubhe.anvilcraft.api.item.IMultipleToOneSmithingRecipeResult;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.builder.AbstractRecipeBuilder;
import dev.dubhe.anvilcraft.util.CodecUtil;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public abstract class BaseMultipleToOneSmithingRecipe<T extends Item & IMultipleToOneSmithingRecipeResult>
    implements Recipe<MultipleToOneSmithingRecipeInput> {

    @SuppressWarnings({"unchecked", "checkstyle:MethodName"})
    protected static <T extends Item & IMultipleToOneSmithingRecipeResult> Codec<T> RESULT_CODEC() {
        return CodecUtil.ITEM_CODEC.flatXmap(
            item -> item instanceof IMultipleToOneSmithingRecipeResult
                    ? DataResult.success((T) item)
                    : DataResult.error(() -> "Item " + item + " is not instance of IMultipleToOneSmithingRecipeResult"),
            DataResult::success
        );
    }

    protected final Ingredient template;
    protected final Ingredient material;
    protected final T result;
    protected final int recipeId;

    protected BaseMultipleToOneSmithingRecipe(Ingredient template, Ingredient material, T result, int recipeId) {
        this.template = template;
        this.material = material;
        this.result = result;
        this.recipeId = recipeId;
    }

    public abstract int inputSize();

    @Override
    public boolean matches(MultipleToOneSmithingRecipeInput input, Level level) {
        return this.isTemplateIngredient(input.template())
               && this.isMaterialIngredient(input.material())
               && this.matchesInput(input, level);
    }

    public abstract boolean matchesInput(MultipleToOneSmithingRecipeInput input, Level level);

    @Override
    public ItemStack assemble(MultipleToOneSmithingRecipeInput input, HolderLookup.Provider registries) {
        return this.result.assemble(this.recipeId, input, registries);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= this.inputSize() + 2 && height >= 1;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return this.result.getDefaultInstance();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.MULTIPLE_TO_ONE_SMITHING_TYPE.get();
    }

    @Override
    public ItemStack getToastSymbol() {
        return ModBlocks.EMBER_SMITHING_TABLE.asStack();
    }

    public boolean isTemplateIngredient(ItemStack template) {
        return this.template.test(template);
    }

    public boolean isMaterialIngredient(ItemStack material) {
        return this.material.test(material);
    }

    public abstract boolean isInputIngredient(int id, ItemStack input);

    /**
     * 用于子类的Serializer
     */
    @SuppressWarnings("unchecked")
    protected static <T extends Item & IMultipleToOneSmithingRecipeResult> Data<T> fromNetwork(RegistryFriendlyByteBuf buf) {
        Ingredient template = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
        Ingredient material = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
        T result = (T) CodecUtil.ITEM_STREAM_CODEC.decode(buf);
        int recipeId = ByteBufCodecs.INT.decode(buf);
        return new Data<>(template, material, result, recipeId);
    }

    protected record Data<T extends Item & IMultipleToOneSmithingRecipeResult>(
        Ingredient template, Ingredient material, T result, int recipeId
    ) {
    }

    /**
     * 用于子类的Serializer
     */
    protected static <R extends BaseMultipleToOneSmithingRecipe<?>> void toNetwork(RegistryFriendlyByteBuf buf, R recipe) {
        Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.template);
        Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.material);
        CodecUtil.ITEM_STREAM_CODEC.encode(buf, recipe.result);
        ByteBufCodecs.INT.encode(buf, recipe.recipeId);
    }

    public abstract static class Builder<T extends Item & IMultipleToOneSmithingRecipeResult, R extends BaseMultipleToOneSmithingRecipe<T>>
        extends AbstractRecipeBuilder<R> {

        protected Ingredient template = template();
        protected Ingredient material;
        protected final T result;
        protected final int recipeId;

        protected Builder(NonNullSupplier<T> resultGetter, int recipeId) {
            this(resultGetter.get(), recipeId);
        }

        protected Builder(T result, int recipeId) {
            this.result = result;
            this.recipeId = recipeId;
        }

        protected abstract int inputSize();

        protected abstract Ingredient template();

        @SafeVarargs
        public final Builder<T, R> material(NonNullSupplier<? extends Item>... materialGetters) {
            this.material = Ingredient.of(Collections2.transform(List.of(materialGetters), NonNullSupplier::get).toArray(new Item[0]));
            return this;
        }

        public Builder<T, R> material(ItemStack... materials) {
            this.material = Ingredient.of(materials);
            return this;
        }

        public Builder<T, R> material(Ingredient material) {
            this.material = material;
            return this;
        }

        @SafeVarargs
        public final Builder<T, R> input(NonNullSupplier<? extends Item>... materialGetters) {
            for (int i = 0; i < this.inputSize(); i++) {
                this.input(i, materialGetters);
            }
            return this;
        }

        public Builder<T, R> input(ItemStack... materials) {
            for (int i = 0; i < this.inputSize(); i++) {
                this.input(i, materials);
            }
            return this;
        }

        public Builder<T, R> input(Ingredient material) {
            for (int i = 0; i < this.inputSize(); i++) {
                this.input(i, material);
            }
            return this;
        }

        @SafeVarargs
        public final Builder<T, R> input(int id, NonNullSupplier<? extends Item>... materialGetters) {
            return this.input(
                id, Ingredient.of(Collections2.transform(Lists.newArrayList(materialGetters), NonNullSupplier::get).toArray(new Item[0])));
        }

        public Builder<T, R> input(int id, ItemStack... materials) {
            return this.input(id, Ingredient.of(materials));
        }

        public abstract Builder<T, R> input(int id, Ingredient material);

        @Override
        public void validate(ResourceLocation pId) {
            if (this.template.isEmpty()) {
                throw new IllegalArgumentException("The template of multiple to one recipe must not be empty, RecipeId: " + pId);
            }
            if (this.material.isEmpty()) {
                throw new IllegalArgumentException("The material of multiple to one recipe must not be empty, RecipeId: " + pId);
            }
            validateInput(pId);
        }

        protected abstract void validateInput(ResourceLocation pId);

        @Override
        public Item getResult() {
            return this.result;
        }
    }
}
