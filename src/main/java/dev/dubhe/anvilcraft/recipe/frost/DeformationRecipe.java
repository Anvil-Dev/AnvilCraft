package dev.dubhe.anvilcraft.recipe.frost;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.recipe.component.ItemIngredientPredicate;
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

public record DeformationRecipe(
    ItemIngredientPredicate template,
    ItemIngredientPredicate material,
    List<RecipeResult> inputs
) implements IFrostSmithingRecipe {
    public static final ItemIngredientPredicate DEFAULT_TEMPLATE = ItemIngredientPredicate.of(ModItems.DEFORMATION_TEMPLATE_ITEM).build();
    public static final ItemIngredientPredicate DEFAULT_MATERIAL = ItemIngredientPredicate.of(ModItems.FROST_METAL_INGOT).build();

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean isTemplate(ItemStack template) {
        return this.template.test(template);
    }

    @Override
    public boolean isMaterial(ItemStack material) {
        return material.is(ModItems.FROST_METAL_INGOT);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeTypes.DEFORMATION_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.DEFORMATION_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<DeformationRecipe> {
        private static final MapCodec<DeformationRecipe> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
            ItemIngredientPredicate.CODEC
                .optionalFieldOf("template", DeformationRecipe.DEFAULT_TEMPLATE)
                .forGetter(DeformationRecipe::template),
            ItemIngredientPredicate.CODEC
                .optionalFieldOf("material", DeformationRecipe.DEFAULT_MATERIAL)
                .forGetter(DeformationRecipe::material),
            RecipeResult.LIST_CODEC
                .fieldOf("inputs")
                .forGetter(DeformationRecipe::inputs)
        ).apply(ins, DeformationRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, DeformationRecipe> STREAM_CODEC = StreamCodec.composite(
            ItemIngredientPredicate.STREAM_CODEC,
            DeformationRecipe::template,
            ItemIngredientPredicate.STREAM_CODEC,
            DeformationRecipe::material,
            RecipeResult.STREAM_CODEC.apply(ByteBufCodecs.list()),
            DeformationRecipe::inputs,
            DeformationRecipe::new
        );

        @Override
        public MapCodec<DeformationRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, DeformationRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }

    public static class Builder extends BaseBuilder<Builder, DeformationRecipe> {
        public Builder() {
            this.template(DeformationRecipe.DEFAULT_TEMPLATE).material(DeformationRecipe.DEFAULT_MATERIAL);
        }

        @Override
        protected Builder getThis() {
            return this;
        }

        @Override
        public DeformationRecipe build(ItemIngredientPredicate template, ItemIngredientPredicate material, List<RecipeResult> inputs) {
            return new DeformationRecipe(template, material, inputs);
        }

        @Override
        public String getType() {
            return "deformation";
        }
    }
}
