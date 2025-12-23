package dev.dubhe.anvilcraft.item.property.component;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Optional;
import java.util.UUID;

public record ContainerStorageRef(Optional<UUID> id) {
    public static final ContainerStorageRef EMPTY = new ContainerStorageRef(Optional.empty());
    public static final MapCodec<ContainerStorageRef> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
        UUIDUtil.CODEC
            .optionalFieldOf("id")
            .forGetter(ContainerStorageRef::id)
    ).apply(ins, ContainerStorageRef::new));
    public static final StreamCodec<ByteBuf, ContainerStorageRef> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC),
        ContainerStorageRef::id,
        ContainerStorageRef::new
    );

    public ContainerStorageRef(UUID id) {
        this(Optional.of(id));
    }
}
