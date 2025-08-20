package dev.dubhe.anvilcraft.item.property.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record DiskData(CompoundTag tag) {
    public static final Codec<DiskData> CODEC = RecordCodecBuilder.create(ins -> ins.group(
        CompoundTag.CODEC
            .fieldOf("tag")
            .forGetter(DiskData::tag)
    ).apply(ins, DiskData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, DiskData> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.COMPOUND_TAG,
        DiskData::tag,
        DiskData::new
    );
}
