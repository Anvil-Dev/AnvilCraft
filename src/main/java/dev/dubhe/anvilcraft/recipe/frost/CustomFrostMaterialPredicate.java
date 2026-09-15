package dev.dubhe.anvilcraft.recipe.frost;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.init.recipe.ModFrostMaterialPredicateTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * 自定义材料，直接使用物品原料谓词判断。
 */
public record CustomFrostMaterialPredicate(ItemIngredientPredicate predicate) implements IFrostMaterialPredicate {
    public static final MapCodec<CustomFrostMaterialPredicate> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
        ItemIngredientPredicate.CODEC
            .fieldOf("predicate")
            .forGetter(CustomFrostMaterialPredicate::predicate)
    ).apply(ins, CustomFrostMaterialPredicate::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, CustomFrostMaterialPredicate> STREAM_CODEC
        = StreamCodec.composite(
        ItemIngredientPredicate.STREAM_CODEC,
        CustomFrostMaterialPredicate::predicate,
        CustomFrostMaterialPredicate::new
    );

    public static CustomFrostMaterialPredicate of(ItemIngredientPredicate predicate) {
        return new CustomFrostMaterialPredicate(predicate);
    }

    @Override
    public boolean test(IFrostSmithingRecipe recipe, FrostSmithingRecipeInput input) {
        return this.predicate.test(input.material());
    }

    @Override
    public int cost(IFrostSmithingRecipe recipe, FrostSmithingRecipeInput input) {
        return this.test(recipe, input) ? this.predicate.count() : 0;
    }

    @Override
    public Type type() {
        return ModFrostMaterialPredicateTypes.CUSTOM.get();
    }

    public static class Type implements IFrostMaterialPredicate.Type<CustomFrostMaterialPredicate> {
        @Override
        public MapCodec<CustomFrostMaterialPredicate> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, CustomFrostMaterialPredicate> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
