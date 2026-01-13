package dev.dubhe.anvilcraft.api.recipe.number;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.api.recipe.result.ResultContext;
import dev.dubhe.anvilcraft.init.recipe.ModNumberProviderTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.RandomSource;

public record BinomialDistributionGenerator(INumberProvider n, INumberProvider p) implements INumberProvider {
    public static final MapCodec<BinomialDistributionGenerator> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
        INumberProvider.CODEC
            .fieldOf("n")
            .forGetter(BinomialDistributionGenerator::n),
        INumberProvider.CODEC
            .fieldOf("p")
            .forGetter(BinomialDistributionGenerator::p)
    ).apply(ins, BinomialDistributionGenerator::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, BinomialDistributionGenerator> STREAM_CODEC = StreamCodec.composite(
        INumberProvider.STREAM_CODEC,
        BinomialDistributionGenerator::n,
        INumberProvider.STREAM_CODEC,
        BinomialDistributionGenerator::p,
        BinomialDistributionGenerator::new
    );

    public static BinomialDistributionGenerator binomial(int n, float p) {
        return new BinomialDistributionGenerator(ConstantValue.exactly((float) n), ConstantValue.exactly(p));
    }

    @Override
    public float getFloat(ResultContext ctx) {
        float p = this.p.getFloat(ctx);

        RandomSource random = ctx.getRandom();
        int result = 0;

        for (int i = this.n.getInt(ctx); i > 0; i--) {
            if (random.nextFloat() < p) result++;
        }

        return result;
    }

    @Override
    public Type type() {
        return ModNumberProviderTypes.BINOMIAL.get();
    }

    public static class Type implements INumberProvider.Type<BinomialDistributionGenerator> {
        @Override
        public MapCodec<BinomialDistributionGenerator> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, BinomialDistributionGenerator> streamCodec() {
            return STREAM_CODEC.cast();
        }
    }
}
