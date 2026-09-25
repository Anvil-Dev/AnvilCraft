package dev.dubhe.anvilcraft.api.sliding;

import com.google.common.collect.Streams;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import it.unimi.dsi.fastutil.ints.IntIntPair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.stream.Stream;

public record SlidingBlockInfo(Vec3i offset, BlockState state, @Nullable BlockEntity blockEntity) {
    public static final MapCodec<SlidingBlockInfo> CODEC = new MapCodec<>() {
        private final MapCodec<Vec3i> offsetCodec = Vec3i.CODEC.fieldOf("offset");
        private final MapCodec<BlockState> stateCodec = BlockState.CODEC.fieldOf("state");
        private final MapCodec<CompoundTag> entityDataCodec = CompoundTag.CODEC.optionalFieldOf("entity_data", new CompoundTag());
        private final MapCodec<CompoundTag> legacyEntityDataCodec = CompoundTag.CODEC.optionalFieldOf("entityData", new CompoundTag());

        @Override
        public <T> RecordBuilder<T> encode(SlidingBlockInfo input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
            this.offsetCodec.encode(input.offset, ops, prefix);
            this.stateCodec.encode(input.state, ops, prefix);
            CompoundTag data = ops instanceof RegistryOps<?> registry
                && registry.lookupProvider instanceof RegistryOps.HolderLookupAdapter adapter
                ? input.beTag(adapter.lookupProvider) : input.beTag();
            this.entityDataCodec.encode(data, ops, prefix);
            return prefix;
        }

        @Override
        public <T> DataResult<SlidingBlockInfo> decode(DynamicOps<T> ops, MapLike<T> input) {
            Vec3i offset = this.offsetCodec.decode(ops, input).getOrThrow();
            BlockState state = this.stateCodec.decode(ops, input).getOrThrow();

            DataResult<CompoundTag> entityData = input.get("entity_data") != null
                ? this.entityDataCodec.decode(ops, input) : this.legacyEntityDataCodec.decode(ops, input);
            if (entityData.isError()) {
                return DataResult.error(() -> "No valid entity data", new SlidingBlockInfo(offset, state));
            }
            if (entityData.getOrThrow().isEmpty()) return DataResult.success(new SlidingBlockInfo(offset, state));
            if (!(ops instanceof RegistryOps<T> registry)
                || !(registry.lookupProvider instanceof RegistryOps.HolderLookupAdapter adapter)) {
                return DataResult.error(() -> "Cannot decode entity data when no registry", new SlidingBlockInfo(offset, state));
            }
            return DataResult.success(new SlidingBlockInfo(
                offset,
                state,
                SlidingBlockInfo.fromTag(adapter.lookupProvider, state, entityData.getPartialOrThrow())
            ));
        }

        @Override
        public <T> Stream<T> keys(DynamicOps<T> ops) {
            return Streams.concat(
                this.offsetCodec.keys(ops), this.stateCodec.keys(ops), this.entityDataCodec.keys(ops), this.legacyEntityDataCodec.keys(ops)
            );
        }
    };
    public static final StreamCodec<RegistryFriendlyByteBuf, SlidingBlockInfo> STREAM_CODEC = StreamCodec.of(
        (buf, info) -> {
            StreamCodecUtil.VEC3I.encode(buf, info.offset());
            StreamCodecUtil.BLOCK_STATE.encode(buf, info.state());
            ByteBufCodecs.COMPOUND_TAG.encode(buf, info.beTag(buf.registryAccess()));
        },
        buf -> {
            Vec3i offset = StreamCodecUtil.VEC3I.decode(buf);
            BlockState state = StreamCodecUtil.BLOCK_STATE.decode(buf);
            return new SlidingBlockInfo(
                offset,
                state,
                SlidingBlockInfo.fromTag(
                    buf.registryAccess(),
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
        return this.beTag(this.blockEntity != null && this.blockEntity.getLevel() != null
            ? this.blockEntity.getLevel().registryAccess() : RegistryAccess.EMPTY);
    }

    public CompoundTag beTag(HolderLookup.Provider registries) {
        return this.blockEntity == null ? new CompoundTag() : this.blockEntity.saveWithFullMetadata(registries);
    }

    public IntIntPair getPos2D(Direction side) {
        return switch (side.getAxis()) {
            case X -> IntIntPair.of(this.offsetY(), this.offsetZ());
            case Y -> IntIntPair.of(this.offsetX(), this.offsetZ());
            case Z -> IntIntPair.of(this.offsetX(), this.offsetY());
        };
    }

    private static @Nullable BlockEntity fromTag(HolderLookup.Provider registries, BlockState state, CompoundTag tag) {
        if (tag.isEmpty() || !tag.contains("id")) return null;
        BlockPos pos = new BlockPos(tag.getIntOr("x", 0), tag.getIntOr("y", 0), tag.getIntOr("z", 0));
        return BlockEntity.loadStatic(pos, state, tag, registries);
    }
}
