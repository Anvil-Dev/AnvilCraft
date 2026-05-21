package dev.dubhe.anvilcraft.item.property.predicate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.advancements.criterion.SingleComponentItemPredicate;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.enchantment.ItemEnchantments;

public record ItemEnchCountPredicate(int min, int max) implements SingleComponentItemPredicate<ItemEnchantments> {
    public static final MapCodec<ItemEnchCountPredicate> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
        Codec.INT
            .fieldOf("min")
            .forGetter(ItemEnchCountPredicate::min),
        Codec.INT
            .fieldOf("max")
            .forGetter(ItemEnchCountPredicate::max)
    ).apply(inst, ItemEnchCountPredicate::new));
    public static final StreamCodec<ByteBuf, ItemEnchCountPredicate> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        ItemEnchCountPredicate::min,
        ByteBufCodecs.VAR_INT,
        ItemEnchCountPredicate::max,
        ItemEnchCountPredicate::new
    );

    public static ItemEnchCountPredicate count(int count) {
        return new ItemEnchCountPredicate(count, count);
    }

    @Override
    public DataComponentType<ItemEnchantments> componentType() {
        return DataComponents.ENCHANTMENTS;
    }

    @Override
    public boolean matches(ItemEnchantments value) {
        int size = value.size();
        return size >= this.min && size <= this.max;
    }
}
