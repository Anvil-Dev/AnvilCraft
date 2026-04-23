package dev.dubhe.anvilcraft.api.recipe.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.init.item.ModCustomDataComponents;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;

@Getter
@EqualsAndHashCode
public class NormalDataComponent<T> implements ICustomDataComponent<T> {
    private final List<RequiredEntry> required;
    private final int input;
    private final DataComponentType<T> dataComponentType;

    private NormalDataComponent(int input, DataComponentType<T> type) {
        this.input = input;
        this.dataComponentType = type;
        this.required = List.of(new RequiredEntry(input, type, true));
    }

    public static <T> NormalDataComponent<T> of(int input, DataComponentType<T> type) {
        return new NormalDataComponent<>(input, type);
    }

    @Override
    public Type getType() {
        return ModCustomDataComponents.NORMAL.get();
    }

    @Override
    public T make(List<Object> data) {
        return Util.cast(data.getFirst());
    }

    @Override
    public T merge(T oldData, T newData) {
        return newData;
    }

    public static class Type implements ICustomDataComponent.Type<NormalDataComponent<?>> {
        public static final MapCodec<NormalDataComponent<?>> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT
                .fieldOf("input")
                .forGetter(NormalDataComponent::getInput),
            DataComponentType.CODEC
                .fieldOf("component")
                .forGetter(NormalDataComponent::getDataComponentType)
        ).apply(instance, NormalDataComponent::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, NormalDataComponent<?>> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            NormalDataComponent::getInput,
            DataComponentType.STREAM_CODEC,
            NormalDataComponent::getDataComponentType,
            NormalDataComponent::new
        );

        @Override
        public MapCodec<NormalDataComponent<?>> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, NormalDataComponent<?>> streamCodec() {
            return STREAM_CODEC;
        }
    }

    public static ICustomDataComponent<?>[] frostFour() {
        return new ICustomDataComponent[] {
            NormalDataComponent.of(0, DataComponents.CUSTOM_NAME),
            ItemEnchantmentsData.enchantments(0),
            ItemEnchantmentsData.mercilessEnchantments(0),
            ItemEnchantmentsData.enchantments(1),
            ItemEnchantmentsData.mercilessEnchantments(1),
            ItemEnchantmentsData.enchantments(2),
            ItemEnchantmentsData.mercilessEnchantments(2),
            ItemEnchantmentsData.enchantments(3),
            ItemEnchantmentsData.mercilessEnchantments(3),
        };
    }

    public static ICustomDataComponent<?>[] emberFour() {
        return new ICustomDataComponent[] {
            NormalDataComponent.of(0, DataComponents.CUSTOM_NAME),
            ItemEnchantmentsData.enchantments(0),
            ItemEnchantmentsData.enchantments(1),
            ItemEnchantmentsData.enchantments(2),
            ItemEnchantmentsData.enchantments(3),
        };
    }

    public static ICustomDataComponent<?>[] normalEight() {
        return new ICustomDataComponent[] {
            ItemEnchantmentsData.enchantments(0),
            ItemEnchantmentsData.enchantments(1),
            ItemEnchantmentsData.enchantments(2),
            ItemEnchantmentsData.enchantments(3),
            ItemEnchantmentsData.enchantments(4),
            ItemEnchantmentsData.enchantments(5),
            ItemEnchantmentsData.enchantments(6),
            ItemEnchantmentsData.enchantments(7),
        };
    }
}
