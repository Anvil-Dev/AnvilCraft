package dev.dubhe.anvilcraft.item.property.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.UUID;

public record StructureDiskData(
    String file,
    String name,
    UUID uuid,
    Direction direction,
    int sizeX,
    int sizeY,
    int sizeZ,
    boolean upsideDown
) {
    public static final Codec<StructureDiskData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.fieldOf("file").forGetter(StructureDiskData::file),
        Codec.STRING.fieldOf("name").forGetter(StructureDiskData::name),
        UUIDUtil.CODEC.fieldOf("uuid").forGetter(StructureDiskData::uuid),
        Direction.CODEC.optionalFieldOf("direction", Direction.NORTH).forGetter(StructureDiskData::direction),
        Codec.INT.fieldOf("sizeX").forGetter(StructureDiskData::sizeX),
        Codec.INT.fieldOf("sizeY").forGetter(StructureDiskData::sizeY),
        Codec.INT.fieldOf("sizeZ").forGetter(StructureDiskData::sizeZ),
        Codec.BOOL.optionalFieldOf("upsideDown", false).forGetter(StructureDiskData::upsideDown)
    ).apply(instance, StructureDiskData::new));

    public static final StreamCodec<FriendlyByteBuf, StructureDiskData> STREAM_CODEC = StreamCodec.of(
        StructureDiskData::write,
        StructureDiskData::read
    );

    private static void write(FriendlyByteBuf buffer, StructureDiskData data) {
        ByteBufCodecs.STRING_UTF8.encode(buffer, data.file);
        ByteBufCodecs.STRING_UTF8.encode(buffer, data.name);
        UUIDUtil.STREAM_CODEC.encode(buffer, data.uuid);
        Direction.STREAM_CODEC.encode(buffer, data.direction);
        buffer.writeVarInt(data.sizeX);
        buffer.writeVarInt(data.sizeY);
        buffer.writeVarInt(data.sizeZ);
        buffer.writeBoolean(data.upsideDown);
    }

    private static StructureDiskData read(FriendlyByteBuf buffer) {
        return new StructureDiskData(
            ByteBufCodecs.STRING_UTF8.decode(buffer),
            ByteBufCodecs.STRING_UTF8.decode(buffer),
            UUIDUtil.STREAM_CODEC.decode(buffer),
            Direction.STREAM_CODEC.decode(buffer),
            buffer.readVarInt(),
            buffer.readVarInt(),
            buffer.readVarInt(),
            buffer.readBoolean()
        );
    }
}
