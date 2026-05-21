package dev.dubhe.anvilcraft.api.recipe.result.modifier;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.api.recipe.data.ICustomDataComponent;
import dev.dubhe.anvilcraft.api.recipe.data.NormalDataComponent;
import dev.dubhe.anvilcraft.api.recipe.result.ResultContext;
import dev.dubhe.anvilcraft.init.recipe.ModResultModifierTypes;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;

/**
 * 复制并合并指定输入物品的数据。
 *
 * @param types 包含指定的输入物品和将要复制的数据组件类型的自定义数据组件。
 */
public record MergeData(List<ICustomDataComponent<?>> types) implements IResultModifier {
    public static final MapCodec<MergeData> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
        ICustomDataComponent.CODEC.listOf()
            .fieldOf("types")
            .forGetter(MergeData::types)
    ).apply(ins, MergeData::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, MergeData> STREAM_CODEC = StreamCodec.composite(
        ICustomDataComponent.STREAM_CODEC.apply(ByteBufCodecs.list()),
        MergeData::types,
        MergeData::new
    );

    public static Builder builder() {
        return new Builder();
    }

    public static Builder mergeData(ICustomDataComponent<?>... types) {
        return new Builder().withTypes(types);
    }

    @Override
    public void modify(ResultContext ctx) {
        for (ICustomDataComponent<?> type : this.types) {
            MergeData.wrappedMake(ctx, type);
        }
    }

    @Override
    public Type type() {
        return ModResultModifierTypes.MERGE_DATA.get();
    }

    private static <T> void wrappedMake(ResultContext ctx, ICustomDataComponent<T> type) {
        T newData = type.make(ctx);
        T oldData = ctx.getResult().get(type.getDataComponentType());
        if (oldData != null && newData != null) {
            newData = type.merge(oldData, newData);
        } else if (newData == null) {
            newData = oldData;
        }
        type.applyToStack(ctx.getResult(), newData);
    }

    public static class Type implements IResultModifier.Type<MergeData> {
        @Override
        public MapCodec<MergeData> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, MergeData> streamCodec() {
            return STREAM_CODEC;
        }
    }

    public static class Builder {
        private final ImmutableList.Builder<ICustomDataComponent<?>> types = ImmutableList.builder();

        public Builder withType(int input, DataComponentType<?> type) {
            this.types.add(NormalDataComponent.of(input, type));
            return this;
        }

        public Builder withTypes(int input, DataComponentType<?>... types) {
            for (DataComponentType<?> type : types) {
                this.types.add(NormalDataComponent.of(input, type));
            }
            return this;
        }

        public Builder withTypes(List<ICustomDataComponent<?>> types) {
            this.types.addAll(types);
            return this;
        }

        public Builder withTypes(ICustomDataComponent<?>... types) {
            for (ICustomDataComponent<?> type : types) {
                this.types.add(type);
            }
            return this;
        }

        public MergeData build() {
            return new MergeData(this.types.build());
        }
    }
}
