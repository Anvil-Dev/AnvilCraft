package dev.dubhe.anvilcraft.item.property.component;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Optional;
import java.util.UUID;

@Getter
public class CrateStorage {
    public static final MapCodec<CrateStorage> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
        UUIDUtil.CODEC
            .fieldOf("id")
            .forGetter(CrateStorage::getId)
    ).apply(ins, CrateStorage::new));
    public static final StreamCodec<ByteBuf, CrateStorage> STREAM_CODEC = StreamCodec.composite(
        UUIDUtil.STREAM_CODEC,
        CrateStorage::getId,
        CrateStorage::new
    );
    private final UUID id;
    private int entryLimit = 54;
    private int stackPower = 4;

    public CrateStorage(UUID id) {
        this.id = id;
    }

    public record Simple(Optional<UUID> id) {
        public static final Simple EMPTY = new Simple(Optional.empty());
        public static final MapCodec<Simple> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
            UUIDUtil.CODEC
                .optionalFieldOf("id")
                .forGetter(Simple::id)
        ).apply(ins, Simple::new));
        public static final StreamCodec<ByteBuf, Simple> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC),
            Simple::id,
            Simple::new
        );
    }
}
