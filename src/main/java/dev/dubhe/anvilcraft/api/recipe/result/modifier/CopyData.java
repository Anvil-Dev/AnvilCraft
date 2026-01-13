package dev.dubhe.anvilcraft.api.recipe.result.modifier;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.api.recipe.data.ICustomDataComponent;
import dev.dubhe.anvilcraft.api.recipe.data.NormalDataComponent;
import dev.dubhe.anvilcraft.api.recipe.result.ResultContext;
import dev.dubhe.anvilcraft.api.recipe.slot.RecipeInputSlot;
import dev.dubhe.anvilcraft.init.recipe.ModResultModifierTypes;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 复制指定输入物品的数据。
 *
 * @param types 包含指定的输入物品和将要复制的数据组件类型的自定义数据组件。
 */
public record CopyData(List<ICustomDataComponent<?>> types) implements IResultModifier {
    public static final MapCodec<CopyData> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
        ICustomDataComponent.CODEC.listOf().fieldOf("types").forGetter(CopyData::types)
    ).apply(ins, CopyData::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, CopyData> STREAM_CODEC = StreamCodec.composite(
        ICustomDataComponent.STREAM_CODEC.apply(ByteBufCodecs.list()), CopyData::types,
        CopyData::new
    );

    public static Builder builder() {
        return new Builder();
    }

    public static Builder copyData(ICustomDataComponent<?>... types) {
        return new Builder().withTypes(types);
    }

    @Override
    public void modify(ResultContext ctx) {
        Map<RecipeInputSlot, ItemStack> cache = new HashMap<>();
        for (ICustomDataComponent<?> type : this.types) {
            CopyData.wrapModify(ctx, type, cache);
        }
    }

    @Override
    public Type type() {
        return ModResultModifierTypes.COPY_DATA.get();
    }

    private static <T> void wrapModify(
        ResultContext ctx, ICustomDataComponent<T> type, Map<RecipeInputSlot, ItemStack> cache
    ) {
        var required = type.getRequired();
        List<Object> data = new ArrayList<>();
        for (var entry : required) {
            ItemStack source = cache.computeIfAbsent(entry.slot(), slot -> IResultModifier.getInput(ctx, slot));
            Object value = source.get(entry.component());
            if (value == null && !entry.isNullable()) throw new IllegalArgumentException(
                "The value of type %s cannot be null in the %s.".formatted(entry.component(), entry.slot().getSerializedName())
            );
            data.add(value);
        }
        type.applyToStack(ctx.getResult(), type.make(data));
    }

    public static class Type implements IResultModifier.Type<CopyData> {
        @Override
        public MapCodec<CopyData> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, CopyData> streamCodec() {
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

        public CopyData build() {
            return new CopyData(this.types.build());
        }
    }
}
