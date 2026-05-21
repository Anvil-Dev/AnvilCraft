package dev.dubhe.anvilcraft.recipe.frost;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.api.recipe.result.RecipeResult;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.List;

public record PermutationRecipe(
    ItemIngredientPredicate template,
    ItemIngredientPredicate material,
    List<RecipeResult> inputs
) implements IFrostSmithingRecipe {
    public static final ItemIngredientPredicate DEFAULT_TEMPLATE = ItemIngredientPredicate.of(ModItems.PERMUTATION_TEMPLATE_ITEM).build();

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean isTemplate(ItemStack template) {
        return this.template.test(template);
    }

    @Override
    public boolean isMaterial(ItemStack material) {
        return this.material.test(material);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeTypes.PERMUTATION_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.PERMUTATION_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<PermutationRecipe> {
        private static final MapCodec<PermutationRecipe> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
            ItemIngredientPredicate.CODEC
                .optionalFieldOf("template", PermutationRecipe.DEFAULT_TEMPLATE)
                .forGetter(PermutationRecipe::template),
            ItemIngredientPredicate.CODEC
                .fieldOf("material")
                .forGetter(PermutationRecipe::material),
            RecipeResult.LIST_CODEC
                .fieldOf("inputs")
                .forGetter(PermutationRecipe::inputs)
        ).apply(ins, PermutationRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, PermutationRecipe> STREAM_CODEC = StreamCodec.composite(
            ItemIngredientPredicate.STREAM_CODEC,
            PermutationRecipe::template,
            ItemIngredientPredicate.STREAM_CODEC,
            PermutationRecipe::material,
            RecipeResult.STREAM_CODEC.apply(ByteBufCodecs.list()),
            PermutationRecipe::inputs,
            PermutationRecipe::new
        );

        @Override
        public MapCodec<PermutationRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, PermutationRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }

    public static class Builder extends BaseBuilder<Builder, PermutationRecipe> {
        public Builder() {
            this.template(PermutationRecipe.DEFAULT_TEMPLATE);
        }

        @Override
        protected Builder getThis() {
            return this;
        }

        @Override
        public PermutationRecipe build(ItemIngredientPredicate template, ItemIngredientPredicate material, List<RecipeResult> inputs) {
            return new PermutationRecipe(template, material, inputs);
        }

        @Override
        public String getType() {
            return "permutation";
        }
    }
}
