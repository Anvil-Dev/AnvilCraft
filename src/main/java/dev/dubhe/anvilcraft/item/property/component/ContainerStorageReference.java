package dev.dubhe.anvilcraft.item.property.component;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Optional;
import java.util.UUID;

public record ContainerStorageReference(Optional<UUID> id) {
    public static final ContainerStorageReference EMPTY = new ContainerStorageReference(Optional.empty());
    public static final MapCodec<ContainerStorageReference> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
        UUIDUtil.CODEC
            .optionalFieldOf("id")
            .forGetter(ContainerStorageReference::id)
    ).apply(ins, ContainerStorageReference::new));
    public static final StreamCodec<ByteBuf, ContainerStorageReference> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC),
        ContainerStorageReference::id,
        ContainerStorageReference::new
    );
}
