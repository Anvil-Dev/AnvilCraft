package dev.dubhe.anvilcraft.api.recipe.data;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.api.recipe.result.ResultContext;
import dev.dubhe.anvilcraft.api.recipe.slot.RecipeInputSlot;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModCustomDataComponents;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

@Getter
@EqualsAndHashCode
public class ItemEnchantmentsData implements ICustomDataComponent<ItemEnchantments> {
    private final RecipeInputSlot input;
    private final DataComponentType<ItemEnchantments> type;

    private ItemEnchantmentsData(RecipeInputSlot input, DataComponentType<?> type) {
        this.input = input;
        this.type = Util.cast(type);
    }

    private ItemEnchantmentsData(int input, DataComponentType<?> type) {
        this(RecipeInputSlot.input(input), type);
    }

    public static ItemEnchantmentsData custom(RecipeInputSlot input, DataComponentType<ItemEnchantments> type) {
        return new ItemEnchantmentsData(input, type);
    }

    public static ItemEnchantmentsData custom(int input, DataComponentType<ItemEnchantments> type) {
        return new ItemEnchantmentsData(input, type);
    }

    public static ItemEnchantmentsData enchantments(RecipeInputSlot input) {
        return new ItemEnchantmentsData(input, DataComponents.ENCHANTMENTS);
    }

    public static ItemEnchantmentsData enchantments(int input) {
        return new ItemEnchantmentsData(input, DataComponents.ENCHANTMENTS);
    }

    public static ItemEnchantmentsData storedEnchantments(RecipeInputSlot input) {
        return new ItemEnchantmentsData(input, DataComponents.STORED_ENCHANTMENTS);
    }

    public static ItemEnchantmentsData storedEnchantments(int input) {
        return new ItemEnchantmentsData(input, DataComponents.STORED_ENCHANTMENTS);
    }

    public static ItemEnchantmentsData mercilessEnchantments(RecipeInputSlot input) {
        return new ItemEnchantmentsData(input, ModComponents.MERCILESS_ENCHANTMENTS);
    }

    public static ItemEnchantmentsData mercilessEnchantments(int input) {
        return new ItemEnchantmentsData(input, ModComponents.MERCILESS_ENCHANTMENTS);
    }

    @Override
    public DataComponentType<ItemEnchantments> getDataComponentType() {
        return this.type;
    }

    @Override
    public Type getType() {
        return ModCustomDataComponents.ITEM_ENCHANTMENTS.get();
    }

    @Override
    public ItemEnchantments make(ResultContext ctx) {
        return ctx.getInput(this.input).get(this.type);
    }

    @Override
    public ItemEnchantments merge(ItemEnchantments oldData, ItemEnchantments newData) {
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(oldData);
        for (var entry : newData.entrySet()) {
            Holder<Enchantment> holder = entry.getKey();
            mutable.set(holder, Math.max(oldData.getLevel(holder), entry.getIntValue()));
        }
        return mutable.toImmutable();
    }

    public static class Type implements ICustomDataComponent.Type<ItemEnchantmentsData> {
        public static final MapCodec<ItemEnchantmentsData> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            RecipeInputSlot.CODEC
                .forGetter(ItemEnchantmentsData::getInput),
            DataComponentType.CODEC
                .fieldOf("component")
                .forGetter(ItemEnchantmentsData::getDataComponentType)
        ).apply(inst, ItemEnchantmentsData::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, ItemEnchantmentsData> STREAM_CODEC = StreamCodec.composite(
            RecipeInputSlot.STREAM_CODEC,
            ItemEnchantmentsData::getInput,
            DataComponentType.STREAM_CODEC,
            ItemEnchantmentsData::getDataComponentType,
            ItemEnchantmentsData::new
        );

        @Override
        public MapCodec<ItemEnchantmentsData> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ItemEnchantmentsData> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
