package dev.dubhe.anvilcraft.item.property.component;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Optional;
import java.util.UUID;

public record SCStorageRef(Optional<UUID> id) {
    public static final SCStorageRef EMPTY = new SCStorageRef(Optional.empty());
    public static final MapCodec<SCStorageRef> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
        UUIDUtil.CODEC
            .optionalFieldOf("id")
            .forGetter(SCStorageRef::id)
    ).apply(ins, SCStorageRef::new));
    public static final StreamCodec<ByteBuf, SCStorageRef> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC),
        SCStorageRef::id,
        SCStorageRef::new
    );

    public SCStorageRef(UUID id) {
        this(Optional.of(id));
    }
}
