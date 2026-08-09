package dev.dubhe.anvilcraft.api.recipe.result.modifier;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.api.recipe.number.ConstantValue;
import dev.dubhe.anvilcraft.api.recipe.number.INumberProvider;
import dev.dubhe.anvilcraft.api.recipe.result.ResultContext;
import dev.dubhe.anvilcraft.init.recipe.ModResultModifierTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

public record ModifyCount(INumberProvider count) implements IResultModifier {
    public static final MapCodec<ModifyCount> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
        INumberProvider.CODEC
            .fieldOf("count")
            .forGetter(ModifyCount::count)
    ).apply(ins, ModifyCount::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, ModifyCount> STREAM_CODEC = StreamCodec.composite(
        INumberProvider.STREAM_CODEC,
        ModifyCount::count,
        ModifyCount::new
    );

    public static Builder of() {
        return new Builder();
    }

    @Override
    public void modify(ResultContext ctx) {
        ctx.updateResult(this.count.getInt(ctx));
    }

    @Override
    public Type type() {
        return ModResultModifierTypes.MODIFY_COUNT.get();
    }

    public static class Type implements IResultModifier.Type<ModifyCount> {
        @Override
        public MapCodec<ModifyCount> codec() {
            return ModifyCount.CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ModifyCount> streamCodec() {
            return ModifyCount.STREAM_CODEC;
        }
    }

    public static class Builder {
        private @Nullable INumberProvider count;

        public Builder() {
        }

        public Builder count(int count) {
            this.count = ConstantValue.exactly(count);
            return this;
        }

        public ModifyCount build() {
            return new ModifyCount(Objects.requireNonNull(this.count, "The count in ModifyCount should not be null!"));
        }
    }
}
