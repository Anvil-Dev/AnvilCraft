package dev.dubhe.anvilcraft.item.property.component;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Optional;
import java.util.UUID;

public record CrateStorageReference(Optional<UUID> id) {
    public static final CrateStorageReference EMPTY = new CrateStorageReference(Optional.empty());
    public static final MapCodec<CrateStorageReference> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
        UUIDUtil.CODEC
            .optionalFieldOf("id")
            .forGetter(CrateStorageReference::id)
    ).apply(ins, CrateStorageReference::new));
    public static final StreamCodec<ByteBuf, CrateStorageReference> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC),
        CrateStorageReference::id,
        CrateStorageReference::new
    );
}
