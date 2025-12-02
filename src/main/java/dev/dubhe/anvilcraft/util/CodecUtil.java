package dev.dubhe.anvilcraft.util;

import com.google.common.collect.EvictingQueue;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

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

    public static <B extends FriendlyByteBuf, T> StreamCodec<B, EvictingQueue<T>> evictingQueueStreamCodec(
        StreamCodec<? super B, T> valueCodec
    ) {
        return StreamCodec.of(
            (buf, queue) -> {
                buf.writeVarInt(queue.size());
                buf.writeVarInt(queue.remainingCapacity());
                for (T t : queue) {
                    valueCodec.encode(buf, t);
                }
            },
            buf -> {
                int size = buf.readVarInt();
                EvictingQueue<T> queue = EvictingQueue.create(size + buf.readVarInt());
                for (int i = 0; i < size; i++) {
                    queue.add(valueCodec.decode(buf));
                }
                return queue;
            }
        );
    }
}
