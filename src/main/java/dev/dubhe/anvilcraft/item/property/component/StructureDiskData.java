package dev.dubhe.anvilcraft.item.property.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
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
    int sizeZ
) {
    public static final Codec<StructureDiskData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.fieldOf("file").forGetter(StructureDiskData::file),
        Codec.STRING.fieldOf("name").forGetter(StructureDiskData::name),
        UUIDUtil.CODEC.fieldOf("uuid").forGetter(StructureDiskData::uuid),
        Direction.CODEC.optionalFieldOf("direction", Direction.NORTH).forGetter(StructureDiskData::direction),
        Codec.INT.fieldOf("sizeX").forGetter(StructureDiskData::sizeX),
        Codec.INT.fieldOf("sizeY").forGetter(StructureDiskData::sizeY),
        Codec.INT.fieldOf("sizeZ").forGetter(StructureDiskData::sizeZ)
    ).apply(instance, StructureDiskData::new));

    public static final StreamCodec<FriendlyByteBuf, StructureDiskData> STREAM_CODEC = StreamCodecUtil.composite(
        ByteBufCodecs.STRING_UTF8,
        StructureDiskData::file,
        ByteBufCodecs.STRING_UTF8,
        StructureDiskData::name,
        UUIDUtil.STREAM_CODEC,
        StructureDiskData::uuid,
        Direction.STREAM_CODEC,
        StructureDiskData::direction,
        ByteBufCodecs.VAR_INT,
        StructureDiskData::sizeX,
        ByteBufCodecs.VAR_INT,
        StructureDiskData::sizeY,
        ByteBufCodecs.VAR_INT,
        StructureDiskData::sizeZ,
        StructureDiskData::new
    );
}
