package dev.dubhe.anvilcraft.recipe.elements;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.util.CodecUtil;
import lombok.Getter;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import static net.minecraft.world.item.ItemStack.ITEM_NON_AIR_CODEC;

@Getter
public class OutputItem {
    public static final Codec<OutputItem> CODEC = RecordCodecBuilder.create(it -> it.group(
            ITEM_NON_AIR_CODEC.fieldOf("id").forGetter(OutputItem::getItemHolder),
            ExtraCodecs.intRange(1, 99).fieldOf("count").orElse(1).forGetter(OutputItem::getCount),
            DataComponentPatch.CODEC
                .optionalFieldOf("components", DataComponentPatch.EMPTY)
                .forGetter(OutputItem::getDataComponentPatch),
            Codec.FLOAT.fieldOf("chance").forGetter(OutputItem::getChance)
        ).apply(it, OutputItem::apply)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, OutputItem> STREAM_CODEC = StreamCodec.of(
        OutputItem::encode, OutputItem::decode
    );

    private static void encode(RegistryFriendlyByteBuf buf, OutputItem outputItem) {
        CodecUtil.ITEM_STREAM_CODEC.encode(buf, outputItem.itemStack.getItem());
        buf.writeVarInt(outputItem.itemStack.getCount());
        buf.writeFloat(outputItem.chance);
    }

    private static OutputItem decode(RegistryFriendlyByteBuf buf) {
        return new OutputItem(
            new ItemStack(CodecUtil.ITEM_STREAM_CODEC.decode(buf), buf.readVarInt()),
            buf.readFloat()
        );
    }

    final ItemStack itemStack;
    final float chance;

    public OutputItem(ItemStack itemStack, float chance) {
        this.itemStack = itemStack;
        this.chance = chance;
    }

    private static OutputItem apply(Holder<Item> item, int count, DataComponentPatch dataComponentPatch, Float aFloat) {
        return new OutputItem(
            new ItemStack(item, count, dataComponentPatch),
            aFloat
        );
    }

    public Holder<Item> getItemHolder() {
        return itemStack.getItemHolder();
    }

    public int getCount() {
        return itemStack.getCount();
    }

    public DataComponentPatch getDataComponentPatch() {
        return itemStack.getComponentsPatch();
    }

    @Nullable
    public ItemStack getResult(RandomSource randomSource) {
        if (randomSource.nextFloat() <= chance) {
            return itemStack;
        } else return null;
    }
}
