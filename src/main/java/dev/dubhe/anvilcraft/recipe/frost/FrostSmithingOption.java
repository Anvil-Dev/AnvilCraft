package dev.dubhe.anvilcraft.recipe.frost;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.api.recipe.result.RecipeResult;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * 浮霜锻造的一种锻造结果及其对材料槽的要求。
 */
public record FrostSmithingOption(IFrostMaterialPredicate material, RecipeResult result) {
    public static final Codec<FrostSmithingOption> CODEC = RecordCodecBuilder.create(ins -> ins.group(
        IFrostMaterialPredicate.CODEC
            .fieldOf("material")
            .forGetter(FrostSmithingOption::material),
        RecipeResult.CODEC
            .fieldOf("result")
            .forGetter(FrostSmithingOption::result)
    ).apply(ins, FrostSmithingOption::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, FrostSmithingOption> STREAM_CODEC = StreamCodec.composite(
        IFrostMaterialPredicate.STREAM_CODEC,
        FrostSmithingOption::material,
        RecipeResult.STREAM_CODEC,
        FrostSmithingOption::result,
        FrostSmithingOption::new
    );

    /**
     * 不需要材料的锻造结果。
     */
    public static FrostSmithingOption of(RecipeResult result) {
        return new FrostSmithingOption(new EmptyFrostMaterialPredicate(), result);
    }

    public boolean isAvailable(IFrostSmithingRecipe recipe, FrostSmithingRecipeInput input) {
        return this.material.test(recipe, input);
    }

    public int cost(IFrostSmithingRecipe recipe, FrostSmithingRecipeInput input) {
        return this.material.cost(recipe, input);
    }
}
