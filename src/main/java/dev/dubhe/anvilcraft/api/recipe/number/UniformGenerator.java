package dev.dubhe.anvilcraft.api.recipe.number;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.api.recipe.result.ResultContext;
import dev.dubhe.anvilcraft.init.recipe.ModNumberProviderTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;

public record UniformGenerator(INumberProvider min, INumberProvider max) implements INumberProvider {
    public static final MapCodec<UniformGenerator> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
        INumberProvider.CODEC
            .fieldOf("min")
            .forGetter(UniformGenerator::min),
        INumberProvider.CODEC
            .fieldOf("max")
            .forGetter(UniformGenerator::max)
    ).apply(ins, UniformGenerator::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, UniformGenerator> STREAM_CODEC = StreamCodec.composite(
        INumberProvider.STREAM_CODEC,
        UniformGenerator::min,
        INumberProvider.STREAM_CODEC,
        UniformGenerator::max,
        UniformGenerator::new
    );

    public static UniformGenerator between(float min, float max) {
        return new UniformGenerator(ConstantValue.exactly(min), ConstantValue.exactly(max));
    }

    @Override
    public int getInt(ResultContext ctx) {
        return Mth.nextInt(ctx.getRandom(), this.min.getInt(ctx), this.max.getInt(ctx));
    }

    @Override
    public float getFloat(ResultContext ctx) {
        return Mth.nextFloat(ctx.getRandom(), this.min.getFloat(ctx), this.max.getFloat(ctx));
    }

    @Override
    public Type type() {
        return ModNumberProviderTypes.UNIFORM.get();
    }

    public static class Type implements INumberProvider.Type<UniformGenerator> {
        @Override
        public MapCodec<UniformGenerator> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, UniformGenerator> streamCodec() {
            return STREAM_CODEC.cast();
        }
    }
}
