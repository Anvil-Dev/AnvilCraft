package dev.dubhe.anvilcraft.api.power;

import net.minecraft.core.BlockPos;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record PowerComponentInfo(
        BlockPos pos, int consumes, int produces, int stores, int capacity, int range) {
    public static final Codec<PowerComponentInfo> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                    BlockPos.CODEC.fieldOf("pos").forGetter(o -> o.pos),
                    Codec.INT.fieldOf("consumes").forGetter(o -> o.consumes),
                    Codec.INT.fieldOf("produces").forGetter(o -> o.produces),
                    Codec.INT.fieldOf("stores").forGetter(o -> o.stores),
                    Codec.INT.fieldOf("capacity").forGetter(o -> o.capacity),
                    Codec.INT.fieldOf("range").forGetter(o -> o.range))
            .apply(ins, PowerComponentInfo::new));
}
