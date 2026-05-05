package dev.dubhe.anvilcraft.api.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record PowerComponentInfo(
    BlockPos pos,
    int consumes,
    int produces,
    int stores,
    int capacity,
    int range,
    PowerComponentType type
) {
    public static final Codec<PowerComponentInfo> CODEC = RecordCodecBuilder.create(ins -> ins.group(
        BlockPos.CODEC
            .fieldOf("pos")
            .forGetter(PowerComponentInfo::pos),
        Codec.INT
            .fieldOf("consumes")
            .forGetter(PowerComponentInfo::consumes),
        Codec.INT
            .fieldOf("produces")
            .forGetter(PowerComponentInfo::produces),
        Codec.INT
            .fieldOf("stores")
            .forGetter(PowerComponentInfo::stores),
        Codec.INT
            .fieldOf("capacity")
            .forGetter(PowerComponentInfo::capacity),
        Codec.INT
            .fieldOf("range")
            .forGetter(PowerComponentInfo::range),
        PowerComponentType.CODEC
            .fieldOf("type")
            .forGetter(PowerComponentInfo::type)
    ).apply(ins, PowerComponentInfo::new));
    public static final StreamCodec<ByteBuf, PowerComponentInfo> STREAM_CODEC = StreamCodecUtil.composite(
        BlockPos.STREAM_CODEC,
        PowerComponentInfo::pos,
        ByteBufCodecs.VAR_INT,
        PowerComponentInfo::consumes,
        ByteBufCodecs.VAR_INT,
        PowerComponentInfo::consumes,
        ByteBufCodecs.VAR_INT,
        PowerComponentInfo::consumes,
        ByteBufCodecs.VAR_INT,
        PowerComponentInfo::consumes,
        ByteBufCodecs.VAR_INT,
        PowerComponentInfo::consumes,
        StreamCodecUtil.enumStreamCodec(PowerComponentType.class),
        PowerComponentInfo::type,
        PowerComponentInfo::new
    );
}
