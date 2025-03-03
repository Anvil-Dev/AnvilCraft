package dev.dubhe.anvilcraft.api.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;

public record PowerComponentInfo(
    BlockPos pos, int consumes, int produces, int stores, int capacity, int range, PowerComponentType type) {
    public static final Codec<PowerComponentInfo> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            BlockPos.CODEC.fieldOf("pos").forGetter(o -> o.pos),
            Codec.INT.fieldOf("consumes").forGetter(o -> o.consumes),
            Codec.INT.fieldOf("produces").forGetter(o -> o.produces),
            Codec.INT.fieldOf("stores").forGetter(o -> o.stores),
            Codec.INT.fieldOf("capacity").forGetter(o -> o.capacity),
            Codec.INT.fieldOf("range").forGetter(o -> o.range),
            PowerComponentType.CODEC.fieldOf("type").forGetter(o -> o.type))
        .apply(ins, PowerComponentInfo::new));
}
