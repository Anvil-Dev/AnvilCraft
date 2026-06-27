package dev.dubhe.anvilcraft.api.sliding;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import it.unimi.dsi.fastutil.ints.IntIntPair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Objects;
import javax.annotation.Nullable;

public record SlidingBlockInfo(Vec3i offset, BlockState state, @Nullable BlockEntity blockEntity) {
    public static final Codec<SlidingBlockInfo> CODEC = RecordCodecBuilder.create(ins -> ins.group(
        Vec3i.CODEC
            .fieldOf("offset")
            .forGetter(SlidingBlockInfo::offset),
        BlockState.CODEC
            .fieldOf("state")
            .forGetter(SlidingBlockInfo::state),
        CompoundTag.CODEC
            .optionalFieldOf("entityData", new CompoundTag())
            .forGetter(SlidingBlockInfo::beTag)
    ).apply(ins, (offset, state, tag) -> new SlidingBlockInfo(offset, state, null)));

    public static final StreamCodec<RegistryFriendlyByteBuf, SlidingBlockInfo> STREAM_CODEC = StreamCodec.of(
        (buf, info) -> {
            StreamCodecUtil.VEC3I.encode(buf, info.offset());
            StreamCodecUtil.BLOCK_STATE.encode(buf, info.state());
            ByteBufCodecs.COMPOUND_TAG.encode(buf, info.beTag());
        },
        buf -> {
            Vec3i offset = StreamCodecUtil.VEC3I.decode(buf);
            BlockState state = StreamCodecUtil.BLOCK_STATE.decode(buf);
            return new SlidingBlockInfo(
                offset,
                state,
                SlidingBlockInfo.fromTag(buf, state, ByteBufCodecs.COMPOUND_TAG.decode(buf))
            );
        }
    );

    public BlockPos getPos(BlockPos center) {
        return center.offset(this.offset);
    }

    public int offsetX() {
        return this.offset.getX();
    }

    public int offsetY() {
        return this.offset.getY();
    }

    public int offsetZ() {
        return this.offset.getZ();
    }

    public CompoundTag beTag() {
        if (this.blockEntity == null) return new CompoundTag();
        return this.blockEntity.saveWithFullMetadata(Objects.requireNonNull(this.blockEntity.getLevel()).registryAccess());
    }

    public IntIntPair getPos2D(Direction side) {
        return switch (side.getAxis()) {
            case X -> IntIntPair.of(this.offsetY(), this.offsetZ());
            case Y -> IntIntPair.of(this.offsetX(), this.offsetZ());
            case Z -> IntIntPair.of(this.offsetX(), this.offsetY());
        };
    }

    private static @Nullable BlockEntity fromTag(RegistryFriendlyByteBuf buf, BlockState state, CompoundTag tag) {
        RegistryAccess registries = buf.registryAccess();
        DataResult<BlockEntityType<?>> entityType = BuiltInRegistries.BLOCK_ENTITY_TYPE.byNameCodec()
            .decode(registries.createSerializationContext(NbtOps.INSTANCE), tag.get("id"))
            .map(Pair::getFirst);
        if (entityType.isError()) return null;
        int x = !tag.contains("x") ? 0 : tag.getInt("x");
        int y = !tag.contains("y") ? 0 : tag.getInt("y");
        int z = !tag.contains("z") ? 0 : tag.getInt("z");
        BlockEntityType<?> blockEntityType = entityType.getOrThrow();
        return blockEntityType.create(new BlockPos(x, y, z), state);
    }
}
