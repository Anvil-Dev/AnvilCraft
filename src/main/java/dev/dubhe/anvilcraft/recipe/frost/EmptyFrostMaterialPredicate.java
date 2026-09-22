package dev.dubhe.anvilcraft.recipe.frost;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.init.recipe.ModFrostMaterialPredicateTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * 空材料，要求材料槽为空。
 */
public record EmptyFrostMaterialPredicate() implements IFrostMaterialPredicate {
    public static final MapCodec<EmptyFrostMaterialPredicate> CODEC = MapCodec.unit(new EmptyFrostMaterialPredicate());
    public static final StreamCodec<RegistryFriendlyByteBuf, EmptyFrostMaterialPredicate> STREAM_CODEC
        = StreamCodec.unit(new EmptyFrostMaterialPredicate());

    @Override
    public boolean test(IFrostSmithingRecipe recipe, FrostSmithingRecipeInput input) {
        return input.material().isEmpty();
    }

    @Override
    public int cost(IFrostSmithingRecipe recipe, FrostSmithingRecipeInput input) {
        return 0;
    }

    @Override
    public Type type() {
        return ModFrostMaterialPredicateTypes.EMPTY.get();
    }

    public static class Type implements IFrostMaterialPredicate.Type<EmptyFrostMaterialPredicate> {
        @Override
        public MapCodec<EmptyFrostMaterialPredicate> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, EmptyFrostMaterialPredicate> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
