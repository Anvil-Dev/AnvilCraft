package dev.dubhe.anvilcraft.recipe.multiple.result.modifier;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.api.data.ICustomDataComponent;
import dev.dubhe.anvilcraft.api.data.NormalDataComponent;
import dev.dubhe.anvilcraft.init.ModResultModifierTypes;
import dev.dubhe.anvilcraft.recipe.multiple.result.ResultContext;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
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
        Int2ObjectMap<ItemStack> cache = new Int2ObjectOpenHashMap<>();
        for (ICustomDataComponent<?> type : this.types) {
            MergeData.wrapModify(ctx, type, cache);
        }
    }

    @Override
    public Type type() {
        return ModResultModifierTypes.MERGE_DATA.get();
    }

    private static <T> void wrapModify(
        ResultContext ctx, ICustomDataComponent<T> type, Int2ObjectMap<ItemStack> cache
    ) {
        var required = type.getRequiredOthers();
        List<Object> data = new ArrayList<>();
        for (var entry : required.keySet()) {
            ItemStack source = cache.computeIfAbsent(entry.getFirst(), input -> IResultModifier.getInput(ctx, input));
            Object value = source.get(entry.getSecond());
            if (value == null && !required.getBoolean(entry)) throw new IllegalArgumentException(
                "The value of type %s cannot be null in the No.%d input.".formatted(entry.getSecond(), entry.getFirst()));
            data.add(value);
        }
        T newData = type.make(data);
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
            return this.withTypes(Arrays.asList(types));
        }

        public MergeData build() {
            return new MergeData(this.types.build());
        }
    }
}
