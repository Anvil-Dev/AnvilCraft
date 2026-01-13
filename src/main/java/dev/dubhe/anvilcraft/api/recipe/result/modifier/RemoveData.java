package dev.dubhe.anvilcraft.api.recipe.result.modifier;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.api.recipe.result.ResultContext;
import dev.dubhe.anvilcraft.init.recipe.ModResultModifierTypes;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;

/**
 * 删除指定输入物品的数据。
 *
 * @param types 包含指定的输入物品和将要删除的数据组件类型。
 */
public record RemoveData(List<DataComponentType<?>> types) implements IResultModifier {
    public static final MapCodec<RemoveData> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
        DataComponentType.CODEC
            .listOf()
            .fieldOf("types")
            .forGetter(RemoveData::types)
    ).apply(ins, RemoveData::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, RemoveData> STREAM_CODEC = StreamCodec.composite(
        DataComponentType.STREAM_CODEC.apply(ByteBufCodecs.list()),
        RemoveData::types,
        RemoveData::new
    );

    public static Builder builder() {
        return new Builder();
    }

    public static Builder removeData(DataComponentType<?>... types) {
        return new Builder().withTypes(types);
    }

    @Override
    public void modify(ResultContext ctx) {
        for (var type : this.types) {
            ctx.getResult().set(type, null);
        }
    }

    @Override
    public Type type() {
        return ModResultModifierTypes.REMOVE_DATA.get();
    }

    public static class Type implements IResultModifier.Type<RemoveData> {
        @Override
        public MapCodec<RemoveData> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, RemoveData> streamCodec() {
            return STREAM_CODEC;
        }
    }

    public static class Builder {
        private final ImmutableList.Builder<DataComponentType<?>> types = ImmutableList.builder();

        public Builder withType(DataComponentType<?> type) {
            this.types.add(type);
            return this;
        }

        public Builder withTypes(DataComponentType<?>... types) {
            for (DataComponentType<?> type : types) {
                this.withType(type);
            }
            return this;
        }

        public Builder withTypes(List<DataComponentType<?>> types) {
            this.types.addAll(types);
            return this;
        }

        public RemoveData build() {
            return new RemoveData(this.types.build());
        }
    }
}
