package dev.dubhe.anvilcraft.api.sliding;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import it.unimi.dsi.fastutil.ints.IntIntPair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
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
    ).apply(ins, (offset, state, tag) -> new SlidingBlockInfo(offset, state, loadBlockEntity(state, tag))));

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
        if (this.blockEntity.getLevel() != null) {
            return this.blockEntity.saveWithFullMetadata(this.blockEntity.getLevel().registryAccess());
        }
        CompoundTag tag = this.blockEntity.saveWithoutMetadata(RegistryAccess.EMPTY);
        tag.putString("id", Objects.requireNonNull(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(this.blockEntity.getType())).toString());
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

    private static @Nullable BlockEntity loadBlockEntity(BlockState state, CompoundTag tag) {
        if (tag.isEmpty()) return null;
        String id = tag.getString("id");
        if (id.isEmpty()) return null;
        BlockEntityType<?> type = BuiltInRegistries.BLOCK_ENTITY_TYPE.get(ResourceLocation.parse(id));
        if (type == null) return null;
        BlockEntity be = type.create(BlockPos.ZERO, state);
        if (be == null) return null;
        be.loadWithComponents(tag, RegistryAccess.EMPTY);
        return be;
    }

    private static @Nullable BlockEntity fromTag(RegistryFriendlyByteBuf buf, BlockState state, CompoundTag tag) {
        String id = tag.getString("id");
        if (id.isEmpty()) return null;
        BlockEntityType<?> blockEntityType = BuiltInRegistries.BLOCK_ENTITY_TYPE.get(ResourceLocation.parse(id));
        if (blockEntityType == null) return null;
        int x = !tag.contains("x") ? 0 : tag.getInt("x");
        int y = !tag.contains("y") ? 0 : tag.getInt("y");
        int z = !tag.contains("z") ? 0 : tag.getInt("z");
        BlockEntity be = blockEntityType.create(new BlockPos(x, y, z), state);
        if (be == null) return null;
        try {
            be.loadWithComponents(tag, buf.registryAccess());
        } catch (RuntimeException ignored) {
            // ignored: 方块实体数据解码失败时保留已加载的部分数据，
            // 避免滑动方块因为单个方块实体解码失败而丢失整个内容。
        }
        return be;
    }
}
