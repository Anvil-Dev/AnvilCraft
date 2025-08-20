package dev.dubhe.anvilcraft.item.property.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;

public record HeliostatsData(BlockPos pos) {
    public static final Codec<HeliostatsData> CODEC = RecordCodecBuilder.create(ins -> ins.group(
        BlockPos.CODEC
            .fieldOf("pos")
            .forGetter(HeliostatsData::pos)
    ).apply(ins, HeliostatsData::new));

    public static final StreamCodec<ByteBuf, HeliostatsData> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC,
        HeliostatsData::pos,
        HeliostatsData::new
    );
}
