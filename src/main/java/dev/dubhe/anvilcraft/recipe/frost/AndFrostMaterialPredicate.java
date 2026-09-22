package dev.dubhe.anvilcraft.recipe.frost;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.recipe.ModFrostMaterialPredicateTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;

/**
 * 要求所有子项都满足，消耗数量取子项中的最大值。
 */
public record AndFrostMaterialPredicate(List<IFrostMaterialPredicate> predicates) implements IFrostMaterialPredicate {
    public static final MapCodec<AndFrostMaterialPredicate> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
        IFrostMaterialPredicate.CODEC
            .listOf()
            .fieldOf("predicates")
            .forGetter(AndFrostMaterialPredicate::predicates)
    ).apply(ins, AndFrostMaterialPredicate::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, AndFrostMaterialPredicate> STREAM_CODEC
        = StreamCodec.composite(
        IFrostMaterialPredicate.STREAM_CODEC.apply(ByteBufCodecs.list()),
        AndFrostMaterialPredicate::predicates,
        AndFrostMaterialPredicate::new
    );

    public AndFrostMaterialPredicate {
        predicates = List.copyOf(predicates);
    }

    public static AndFrostMaterialPredicate of(IFrostMaterialPredicate... predicates) {
        return new AndFrostMaterialPredicate(List.of(predicates));
    }

    @Override
    public boolean test(IFrostSmithingRecipe recipe, FrostSmithingRecipeInput input) {
        return this.predicates.stream().allMatch(predicate -> predicate.test(recipe, input));
    }

    @Override
    public int cost(IFrostSmithingRecipe recipe, FrostSmithingRecipeInput input) {
        if (!this.test(recipe, input)) {
            return 0;
        }
        return this.predicates.stream().mapToInt(predicate -> predicate.cost(recipe, input)).max().orElse(0);
    }

    @Override
    public Type type() {
        return ModFrostMaterialPredicateTypes.AND.get();
    }

    public static class Type implements IFrostMaterialPredicate.Type<AndFrostMaterialPredicate> {
        @Override
        public MapCodec<AndFrostMaterialPredicate> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, AndFrostMaterialPredicate> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
