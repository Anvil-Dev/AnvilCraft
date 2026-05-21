package dev.dubhe.anvilcraft.api.recipe.data;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.api.recipe.result.ResultContext;
import dev.dubhe.anvilcraft.api.recipe.slot.RecipeInputSlot;
import dev.dubhe.anvilcraft.init.item.ModCustomDataComponents;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

@Getter
@EqualsAndHashCode
public class NormalDataComponent<T> implements ICustomDataComponent<T> {
    private final RecipeInputSlot input;
    private final DataComponentType<T> type;

    private NormalDataComponent(RecipeInputSlot input, DataComponentType<T> type) {
        this.input = input;
        this.type = type;
    }

    public static <T> NormalDataComponent<T> of(RecipeInputSlot input, DataComponentType<T> type) {
        return new NormalDataComponent<>(input, type);
    }

    public static <T> NormalDataComponent<T> of(int input, DataComponentType<T> type) {
        return new NormalDataComponent<>(RecipeInputSlot.input(input), type);
    }

    @Override
    public DataComponentType<T> getDataComponentType() {
        return this.type;
    }

    @Override
    public Type getType() {
        return ModCustomDataComponents.NORMAL.get();
    }

    @Override
    public T make(ResultContext ctx) {
        return ctx.getInput(this.input).get(this.type);
    }

    @Override
    public T merge(T oldData, T newData) {
        return newData;
    }

    public static class Type implements ICustomDataComponent.Type<NormalDataComponent<?>> {
        public static final MapCodec<NormalDataComponent<?>> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            RecipeInputSlot.CODEC
                .forGetter(NormalDataComponent::getInput),
            DataComponentType.CODEC
                .fieldOf("component")
                .forGetter(NormalDataComponent::getDataComponentType)
        ).apply(instance, NormalDataComponent::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, NormalDataComponent<?>> STREAM_CODEC = StreamCodec.composite(
            RecipeInputSlot.STREAM_CODEC,
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
