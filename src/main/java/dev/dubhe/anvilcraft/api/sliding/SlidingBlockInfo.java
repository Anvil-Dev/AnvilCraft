package dev.dubhe.anvilcraft.api.sliding;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.util.CodecUtil;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.ints.IntIntPair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.block.state.BlockState;

public record SlidingBlockInfo(Vec3i offset, BlockState state, CompoundTag entityData) {
    public static final Codec<SlidingBlockInfo> CODEC = RecordCodecBuilder.create(ins -> ins.group(
        Vec3i.CODEC.fieldOf("offset").forGetter(SlidingBlockInfo::offset),
        BlockState.CODEC.fieldOf("state").forGetter(SlidingBlockInfo::state),
        CompoundTag.CODEC.fieldOf("entityData").forGetter(SlidingBlockInfo::entityData)
    ).apply(ins, SlidingBlockInfo::new));
    public static final StreamCodec<ByteBuf, SlidingBlockInfo> SIMPLE_STREAM_CODEC = StreamCodec.composite(
        CodecUtil.VEC3I_STREAM_CODEC, SlidingBlockInfo::offset,
        CodecUtil.BLOCK_STATE_STREAM_CODEC, SlidingBlockInfo::state,
        SlidingBlockInfo::new
    );
    public static final StreamCodec<ByteBuf, SlidingBlockInfo> STREAM_CODEC = StreamCodec.composite(
        CodecUtil.VEC3I_STREAM_CODEC, SlidingBlockInfo::offset,
        CodecUtil.BLOCK_STATE_STREAM_CODEC, SlidingBlockInfo::state,
        ByteBufCodecs.COMPOUND_TAG, SlidingBlockInfo::entityData,
        SlidingBlockInfo::new
    );

    public SlidingBlockInfo(Vec3i offset, BlockState state) {
        this(offset, state, new CompoundTag());
    }

    public BlockPos getPos(BlockPos center) {
        return center.offset(this.offset);
    }

    public int x() {
        return this.offset.getX();
    }

    public int y() {
        return this.offset.getY();
    }

    public int z() {
        return this.offset.getZ();
    }

    public IntIntPair getPos2D(Direction side) {
        return switch (side.getAxis()) {
            case X -> IntIntPair.of(this.y(), this.z());
            case Y -> IntIntPair.of(this.x(), this.z());
            case Z -> IntIntPair.of(this.x(), this.y());
        };
    }
}
