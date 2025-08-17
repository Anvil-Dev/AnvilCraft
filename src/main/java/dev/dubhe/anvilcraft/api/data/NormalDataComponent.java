package dev.dubhe.anvilcraft.api.data;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.ModCustomDataComponents;
import dev.dubhe.anvilcraft.util.Util;
import it.unimi.dsi.fastutil.objects.Object2BooleanArrayMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import lombok.Getter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Getter
public class NormalDataComponent<T> implements ICustomDataComponent<T> {
    private final Object2BooleanMap<Pair<Integer, DataComponentType<?>>> map = new Object2BooleanArrayMap<>();
    private final int input;
    private final DataComponentType<T> dataComponentType;

    private NormalDataComponent(int input, DataComponentType<T> type) {
        this.input = input;
        this.dataComponentType = type;
        this.map.put(new Pair<>(input, type), true);
    }

    public static <T> NormalDataComponent<T> of(int input, DataComponentType<T> type) {
        return new NormalDataComponent<>(input, type);
    }

    @Override
    public Type getType() {
        return ModCustomDataComponents.NORMAL.get();
    }

    @Override
    public Object2BooleanMap<Pair<Integer, DataComponentType<?>>> getRequiredOthers() {
        return this.map;
    }

    @Override
    public T make(List<Object> data) {
        return Util.cast(data.getFirst());
    }

    public static class Type implements ICustomDataComponent.Type<NormalDataComponent<?>> {
        public static final MapCodec<NormalDataComponent<?>> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.fieldOf("input").forGetter(NormalDataComponent::getInput),
            DataComponentType.CODEC.fieldOf("type").forGetter(NormalDataComponent::getDataComponentType)
        ).apply(instance, NormalDataComponent::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, NormalDataComponent<?>> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, NormalDataComponent::getInput,
            DataComponentType.STREAM_CODEC, NormalDataComponent::getDataComponentType,
            NormalDataComponent::new
        );

        @Override
        public @NotNull MapCodec<NormalDataComponent<?>> codec() {
            return CODEC;
        }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, NormalDataComponent<?>> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
