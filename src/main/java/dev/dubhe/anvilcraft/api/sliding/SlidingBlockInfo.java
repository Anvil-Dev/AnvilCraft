package dev.dubhe.anvilcraft.api.sliding;

import com.google.common.collect.Streams;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import it.unimi.dsi.fastutil.ints.IntIntPair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.stream.Stream;

public record SlidingBlockInfo(Vec3i offset, BlockState state, @Nullable BlockEntity blockEntity) {
    public static final MapCodec<SlidingBlockInfo> CODEC = new MapCodec<>() {
        private final MapCodec<Vec3i> offsetCodec = Vec3i.CODEC.fieldOf("offset");
        private final MapCodec<BlockState> stateCodec = BlockState.CODEC.fieldOf("state");
        private final MapCodec<CompoundTag> entityDataCodec = CompoundTag.CODEC.fieldOf("entity_data");

        @Override
        public <T> RecordBuilder<T> encode(SlidingBlockInfo input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
            this.offsetCodec.encode(input.offset, ops, prefix);
            this.stateCodec.encode(input.state, ops, prefix);
            this.entityDataCodec.encode(input.beTag(), ops, prefix);
            return prefix;
        }

        @Override
        public <T> DataResult<SlidingBlockInfo> decode(DynamicOps<T> ops, MapLike<T> input) {
            Vec3i offset = this.offsetCodec.decode(ops, input).getOrThrow();
            BlockState state = this.stateCodec.decode(ops, input).getOrThrow();

            DataResult<CompoundTag> entityData = this.entityDataCodec.decode(ops, input);
            if (entityData.isError()) {
                return DataResult.error(() -> "No valid entity data", new SlidingBlockInfo(offset, state));
            }
            if (!(ops instanceof RegistryOps<T> registry)) {
                return DataResult.error(() -> "Cannot decode entity data when no registry", new SlidingBlockInfo(offset, state));
            }
            return DataResult.success(new SlidingBlockInfo(
                offset,
                state,
                SlidingBlockInfo.fromTag(registry.withParent(NbtOps.INSTANCE), state, entityData.getPartialOrThrow())
            ));
        }

        @Override
        public <T> Stream<T> keys(DynamicOps<T> ops) {
            return Streams.concat(
                this.offsetCodec.keys(ops), this.stateCodec.keys(ops), this.entityDataCodec.keys(ops)
            );
        }
    };
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
                SlidingBlockInfo.fromTag(
                    buf.registryAccess().createSerializationContext(NbtOps.INSTANCE),
                    state,
                    ByteBufCodecs.COMPOUND_TAG.decode(buf)
                )
            );
        }
    );

    public SlidingBlockInfo(Vec3i offset, BlockState state) {
        this(offset, state, null);
    }

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
        if (this.blockEntity.getLevel() != null) {
            return this.blockEntity.saveWithFullMetadata(this.blockEntity.getLevel().registryAccess());
        }
        CompoundTag tag = this.blockEntity.saveWithoutMetadata(RegistryAccess.EMPTY);
        tag.putString(
            "id",
            Objects.requireNonNull(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(this.blockEntity.getType())).toString()
        );
        tag.putInt("x", this.blockEntity.getBlockPos().getX());
        tag.putInt("y", this.blockEntity.getBlockPos().getY());
        tag.putInt("z", this.blockEntity.getBlockPos().getZ());
        return tag;
    }

    public IntIntPair getPos2D(Direction side) {
        return switch (side.getAxis()) {
            case X -> IntIntPair.of(this.offsetY(), this.offsetZ());
            case Y -> IntIntPair.of(this.offsetX(), this.offsetZ());
            case Z -> IntIntPair.of(this.offsetX(), this.offsetY());
        };
    }

    private static @Nullable BlockEntity fromTag(RegistryOps<Tag> ops, BlockState state, CompoundTag tag) {
        DataResult<BlockEntityType<?>> entityType = BuiltInRegistries.BLOCK_ENTITY_TYPE.byNameCodec()
            .decode(ops, tag.get("id"))
            .map(Pair::getFirst);
        if (entityType.isError()) return null;
        int x = tag.getIntOr("x", 0);
        int y = tag.getIntOr("y", 0);
        int z = tag.getIntOr("z", 0);
        BlockEntityType<?> blockEntityType = entityType.getOrThrow();
        return blockEntityType.create(new BlockPos(x, y, z), state);
    }
}
