package dev.dubhe.anvilcraft.recipe.frost;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.recipe.ModFrostMaterialPredicateTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;

/**
 * 满足任意一个子项即可，消耗数量取第一个满足的子项。
 */
public record OrFrostMaterialPredicate(List<IFrostMaterialPredicate> predicates) implements IFrostMaterialPredicate {
    public static final MapCodec<OrFrostMaterialPredicate> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
        IFrostMaterialPredicate.CODEC
            .listOf()
            .fieldOf("predicates")
            .forGetter(OrFrostMaterialPredicate::predicates)
    ).apply(ins, OrFrostMaterialPredicate::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, OrFrostMaterialPredicate> STREAM_CODEC
        = StreamCodec.composite(
        IFrostMaterialPredicate.STREAM_CODEC.apply(ByteBufCodecs.list()),
        OrFrostMaterialPredicate::predicates,
        OrFrostMaterialPredicate::new
    );

    public OrFrostMaterialPredicate {
        predicates = List.copyOf(predicates);
    }

    public static OrFrostMaterialPredicate of(IFrostMaterialPredicate... predicates) {
        return new OrFrostMaterialPredicate(List.of(predicates));
    }

    @Override
    public boolean test(IFrostSmithingRecipe recipe, FrostSmithingRecipeInput input) {
        return this.predicates.stream().anyMatch(predicate -> predicate.test(recipe, input));
    }

    @Override
    public int cost(IFrostSmithingRecipe recipe, FrostSmithingRecipeInput input) {
        return this.predicates.stream()
            .filter(predicate -> predicate.test(recipe, input))
            .findFirst()
            .map(predicate -> predicate.cost(recipe, input))
            .orElse(0);
    }

    @Override
    public Type type() {
        return ModFrostMaterialPredicateTypes.OR.get();
    }

    public static class Type implements IFrostMaterialPredicate.Type<OrFrostMaterialPredicate> {
        @Override
        public MapCodec<OrFrostMaterialPredicate> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, OrFrostMaterialPredicate> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
