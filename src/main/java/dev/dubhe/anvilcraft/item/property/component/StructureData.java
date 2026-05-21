package dev.dubhe.anvilcraft.item.property.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record StructureData(
    int minX, int minY, int minZ,
    int maxX, int maxY, int maxZ
) {
    public static final Codec<StructureData> CODEC = RecordCodecBuilder.create(ins -> ins.group(
        Codec.INT.fieldOf("minX").forGetter(StructureData::minX),
        Codec.INT.fieldOf("minY").forGetter(StructureData::minY),
        Codec.INT.fieldOf("minZ").forGetter(StructureData::minZ),
        Codec.INT.fieldOf("maxX").forGetter(StructureData::maxX),
        Codec.INT.fieldOf("maxY").forGetter(StructureData::maxY),
        Codec.INT.fieldOf("maxZ").forGetter(StructureData::maxZ)
    ).apply(ins, StructureData::new));

    public static final StreamCodec<ByteBuf, StructureData> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        StructureData::minX,
        ByteBufCodecs.VAR_INT,
        StructureData::minY,
        ByteBufCodecs.VAR_INT,
        StructureData::minZ,
        ByteBufCodecs.VAR_INT,
        StructureData::maxX,
        ByteBufCodecs.VAR_INT,
        StructureData::maxY,
        ByteBufCodecs.VAR_INT,
        StructureData::maxZ,
        StructureData::new
    );

    public StructureData(BlockPos initPos) {
        this(initPos.getX(), initPos.getY(), initPos.getZ(), initPos.getX(), initPos.getY(), initPos.getZ());
    }

    public StructureData addPos(BlockPos pos) {
        int newMinX = Math.min(this.minX, pos.getX());
        int newMinY = Math.min(this.minY, pos.getY());
        int newMinZ = Math.min(this.minZ, pos.getZ());
        int newMaxX = Math.max(this.maxX, pos.getX());
        int newMaxY = Math.max(this.maxY, pos.getY());
        int newMaxZ = Math.max(this.maxZ, pos.getZ());
        return new StructureData(newMinX, newMinY, newMinZ, newMaxX, newMaxY, newMaxZ);
    }

    public BlockPos minPos() {
        return new BlockPos(this.minX, this.minY, this.minZ);
    }

    public BlockPos maxPos() {
        return new BlockPos(this.maxX, this.maxY, this.maxZ);
    }

    public int getSizeX() {
        return this.maxX - this.minX + 1;
    }

    public int getSizeY() {
        return this.maxY - this.minY + 1;
    }

    public int getSizeZ() {
        return this.maxZ - this.minZ + 1;
    }

    public boolean isCube() {
        return this.getSizeX() == this.getSizeY() && this.getSizeY() == this.getSizeZ();
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isOddCubeWithinSize(int maxSize) {
        return this.isCube() && this.getSizeX() % 2 == 1 && this.getSizeX() <= maxSize;
    }
}
