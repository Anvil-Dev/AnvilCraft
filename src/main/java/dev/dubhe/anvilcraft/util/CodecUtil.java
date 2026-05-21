package dev.dubhe.anvilcraft.util;

import com.google.common.collect.EvictingQueue;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.util.Util;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class CodecUtil {
    public static final Codec<AABB> AABB_CODEC = RecordCodecBuilder.create(ins ->
        ins.group(
            Vec3.CODEC.fieldOf("from").forGetter(AABB::getMinPosition),
            Vec3.CODEC.fieldOf("to").forGetter(AABB::getMaxPosition)
        ).apply(ins, AABB::new)
    );

    public static final StreamCodec<? super FriendlyByteBuf, AABB> AABB_STREAM_CODEC = StreamCodec.of(
        (buf, aabb) -> {
            buf.writeVector3f(aabb.getMinPosition().toVector3f());
            buf.writeVector3f(aabb.getMaxPosition().toVector3f());
        },
        (buf) -> new AABB(new Vec3(buf.readVector3f()), new Vec3(buf.readVector3f()))
    );

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
