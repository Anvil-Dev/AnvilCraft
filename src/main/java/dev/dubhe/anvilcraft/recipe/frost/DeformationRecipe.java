package dev.dubhe.anvilcraft.recipe.frost;

import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.api.recipe.result.RecipeResult;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public record DeformationRecipe(
    Optional<ItemIngredientPredicate> template,
    Optional<ItemIngredientPredicate> material,
    List<RecipeResult> inputs
) implements IFrostSmithingRecipe {
    public static final RecipeSerializer<DeformationRecipe> SERIALIZER = new RecipeSerializer<>(
        RecordCodecBuilder.mapCodec(ins -> ins.group(
            ItemIngredientPredicate.CODEC
                .optionalFieldOf("template")
                .forGetter(DeformationRecipe::template),
            ItemIngredientPredicate.CODEC
                .optionalFieldOf("material")
                .forGetter(DeformationRecipe::material),
            RecipeResult.LIST_CODEC
                .fieldOf("inputs")
                .forGetter(DeformationRecipe::inputs)
        ).apply(ins, DeformationRecipe::new)),
        StreamCodec.composite(
            ByteBufCodecs.optional(ItemIngredientPredicate.STREAM_CODEC),
            DeformationRecipe::template,
            ByteBufCodecs.optional(ItemIngredientPredicate.STREAM_CODEC),
            DeformationRecipe::material,
            RecipeResult.STREAM_CODEC.apply(ByteBufCodecs.list()),
            DeformationRecipe::inputs,
            DeformationRecipe::new
        )
    );

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean isTemplate(ItemStack template) {
        return this.template.map(predicate -> predicate.test(template)).orElseGet(() -> template.is(ModItems.DEFORMATION_TEMPLATE));
    }

    @Override
    public boolean isMaterial(ItemStack material) {
        return this.material.map(predicate -> predicate.test(material)).orElseGet(() -> material.is(ModItems.FROST_METAL_INGOT));
    }

    @Override
    public RecipeType<DeformationRecipe> getType() {
        return ModRecipeTypes.DEFORMATION.get();
    }

    @Override
    public RecipeSerializer<DeformationRecipe> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public String group() {
        return "deformation";
    }

    public static class Builder extends BaseBuilder<Builder, DeformationRecipe> {
        public Builder() {
        }

        @Override
        protected Builder getThis() {
            return this;
        }

        @Override
        public DeformationRecipe build(
            @Nullable ItemIngredientPredicate template,
            @Nullable ItemIngredientPredicate material,
            List<RecipeResult> inputs
        ) {
            return new DeformationRecipe(Optional.ofNullable(template), Optional.ofNullable(material), inputs);
        }

        @Override
        public String getType() {
            return "deformation";
        }
    }
}
