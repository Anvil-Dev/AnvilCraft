package dev.dubhe.anvilcraft.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.builder.AbstractRecipeBuilder;
import dev.dubhe.anvilcraft.util.NumberProviderUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;

@Getter
@MethodsReturnNonnullByDefault
public class MeshRecipe implements Recipe<MeshRecipe.Input> {
    public final Ingredient input;
    public final ItemStack result;
    public final NumberProvider resultAmount;

    public MeshRecipe(Ingredient input, ItemStack result, NumberProvider resultAmount) {
        this.input = input;
        this.result = result;
        this.resultAmount = resultAmount;
    }

    public MeshRecipe(Ingredient input, ItemStack result) {
        this(input, result, ConstantValue.exactly(1));
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.MESH_TYPE.get();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeTypes.MESH_SERIALIZER.get();
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
        return input.test(pInput.itemStack);
    }

    @Override
    public ItemStack assemble(Input pInput, HolderLookup.Provider pRegistries) {
        return ItemStack.EMPTY;
    }

    public record Input(ItemStack itemStack) implements RecipeInput {

        @Override
        public ItemStack getItem(int pIndex) {
            return itemStack;
        }

        @Override
        public int size() {
            return 1;
        }
    }

    public static class Serializer implements RecipeSerializer<MeshRecipe> {
        private static final MapCodec<MeshRecipe> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("input").forGetter(MeshRecipe::getInput),
                ItemStack.CODEC.fieldOf("result").forGetter(MeshRecipe::getResult),
                NumberProviders.CODEC.fieldOf("result_amount").forGetter(MeshRecipe::getResultAmount)
        ).apply(ins, MeshRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, MeshRecipe> STREAM_CODEC = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, MeshRecipe::getInput,
                ItemStack.STREAM_CODEC, MeshRecipe::getResult,
                StreamCodec.of(NumberProviderUtil::toNetwork, NumberProviderUtil::fromNetwork), MeshRecipe::getResultAmount,
                MeshRecipe::new
        );

        @Override
        public MapCodec<MeshRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, MeshRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }

    @Setter
    @Accessors(fluent = true, chain = true)
    public static class Builder extends AbstractRecipeBuilder<MeshRecipe> {
        private Ingredient input;
        private ItemStack result;
        private NumberProvider resultAmount;

        @Override
        public MeshRecipe buildRecipe() {
            if (resultAmount == null) {
                return new MeshRecipe(input, result);
            } else {
                return new MeshRecipe(input, result, resultAmount);
            }
        }

        @Override
        public void validate(ResourceLocation pId) {
            if (input == null) {
                throw new IllegalArgumentException("Input cannot be null, RecipeId: " + pId);
            }
            if (result.isEmpty()) {
                throw new IllegalArgumentException("Result cannot be empty, RecipeId: " + pId);
            }
        }

        @Override
        public Item getResult() {
            return result.getItem();
        }
    }
}
