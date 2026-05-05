package dev.dubhe.anvilcraft.util;

import com.google.common.collect.EvictingQueue;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.util.Util;

import java.util.List;

public class CodecUtil {
    public static <T> MapCodec<EvictingQueue<T>> evictingQueueMapCodec(Codec<T> valueCodec) {
        return RecordCodecBuilder.mapCodec(inst -> inst.group(
            Codec.INT
                .fieldOf("max_size")
                .forGetter(queue -> queue.remainingCapacity() + queue.size()),
            valueCodec.listOf()
                .fieldOf("values")
                .forGetter(List::copyOf)
        ).apply(inst, (maxSize, values) -> Util.run(EvictingQueue.create(maxSize), queue -> values.forEach(queue::offer))));
    }
}
