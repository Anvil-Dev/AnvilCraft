package dev.dubhe.anvilcraft.api.sliding;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.util.AabbUtil;
import dev.dubhe.anvilcraft.util.MathUtil;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.ints.IntIntPair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public final class SlidingBlockSection {
    public static final SlidingBlockSection EMPTY = new SlidingBlockSection(List.of());
    public static final Codec<SlidingBlockSection> CODEC = RecordCodecBuilder.create(ins -> ins.group(
        SlidingBlockInfo.CODEC.listOf().fieldOf("blocks").forGetter(SlidingBlockSection::blocks)
    ).apply(ins, SlidingBlockSection::new));
    public static final StreamCodec<ByteBuf, SlidingBlockSection> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.collection(ArrayList::new, SlidingBlockInfo.STREAM_CODEC), SlidingBlockSection::blocks,
        SlidingBlockSection::new
    );
    private final List<SlidingBlockInfo> blocks;
    private final EnumMap<Direction, Pair<List<Vec3i>, AABB>> sideCache = new EnumMap<>(Direction.class);

    public SlidingBlockSection(List<SlidingBlockInfo> blocks) {
        this.blocks = blocks;
    }

    public static SlidingBlockSection create(BlockPos center, Iterable<Triple<BlockPos, BlockState, Optional<CompoundTag>>> infos) {
        ImmutableList.Builder<SlidingBlockInfo> builder = ImmutableList.builder();
        for (var infoRaw : infos) {
            BlockPos otherPos = infoRaw.getLeft();
            BlockState other = infoRaw.getMiddle();
            Optional<CompoundTag> data = infoRaw.getRight();
            Vec3i pos = MathUtil.dist(otherPos, center);
            SlidingBlockInfo info = new SlidingBlockInfo(pos, other, data.orElse(new CompoundTag()));
            builder.add(info);
        }
        return new SlidingBlockSection(builder.build());
    }

    public int size() {
        return this.blocks.size();
    }

    public boolean isEmpty() {
        return this.blocks.isEmpty();
    }

    public Vec3 findCollide(Vec3 center, AABB box) {
        Vec3 vector = Vec3.ZERO;
        for (SlidingBlockInfo info : this.blocks) {
            Vec3 min = new Vec3(info.x() + center.x, info.y() + center.y, info.z() + center.z);
            Vec3 max = min.add(1, 1, 1);
            box.clip(min, max).ifPresent(vector::add);
        }
        return vector;
    }

    public List<Vec3i> getWallsOnSide(Direction side) {
        if (this.sideCache.containsKey(side)) return this.sideCache.get(side).getFirst();
        this.calculateSide(side);
        return this.sideCache.get(side).getFirst();
    }

    public AABB getBoundsOnSide(Direction side) {
        if (this.sideCache.containsKey(side)) return this.sideCache.get(side).getSecond();
        this.calculateSide(side);
        return this.sideCache.get(side).getSecond();
    }

    private void calculateSide(Direction side) {
        AABB bounds = null;
        Multimap<IntIntPair, Vec3i> cache = HashMultimap.create();
        for (SlidingBlockInfo info : this.blocks) {
            cache.put(info.getPos2D(side), info.offset());
            if (bounds == null) {
                bounds = AabbUtil.create(info.offset(), info.offset());
                continue;
            }
            bounds = AabbUtil.minmax(bounds, info.offset());
        }
        List<Vec3i> result = new ArrayList<>();
        for (IntIntPair key : cache.keySet()) {
            Stream<Vec3i> stream = cache.get(key).stream();
            Optional<Vec3i> vec3Op;
            if (side.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
                vec3Op = stream.max(Vec3i::compareTo);
            } else {
                vec3Op = stream.min(Vec3i::compareTo);
            }
            vec3Op.ifPresent(result::add);
        }
        this.sideCache.put(side, new Pair<>(ImmutableList.copyOf(result), bounds));
    }

    public void setBlock(Level level, BlockPos center, Entity entity) {
        if (level.isClientSide) return;
        for (SlidingBlockInfo info : this.blocks) {
            BlockPos pos = info.getPos(center);
            BlockState state = info.state();
            if (level.isOutsideBuildHeight(pos)) {
                Block.dropResources(state, level, pos);
                continue;
            }
            if (level.getFluidState(pos).getType() == Fluids.WATER) {
                state = state.setValue(BlockStateProperties.WATERLOGGED, true);
            }

            boolean canBeReplaced = level.getBlockState(pos).canBeReplaced(
                new DirectionalPlaceContext(level, pos, Direction.DOWN, ItemStack.EMPTY, Direction.UP));
            boolean canSurvive = state.canSurvive(level, pos);
            if (!canBeReplaced || !canSurvive) {
                Block.dropResources(state, level, pos);
                continue;
            }

            state = Block.updateFromNeighbourShapes(state, level, pos);
            if (!level.setBlock(pos, state, Block.UPDATE_ALL)) continue;
            Optional.ofNullable(level.getBlockEntity(pos))
                .ifPresent(entity1 -> entity1.loadCustomOnly(info.entityData(), level.registryAccess()));
            level.neighborChanged(pos, state.getBlock(), pos);

            ((ServerLevel) level)
                .getChunkSource()
                .chunkMap
                .broadcast(entity, new ClientboundBlockUpdatePacket(pos, level.getBlockState(pos)));
        }
        entity.discard();
    }

    @Override
    public @NotNull String toString() {
        return "Section[" + this.blocks + ']';
    }

    public List<SlidingBlockInfo> blocks() {
        return blocks;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof SlidingBlockSection that)) return false;
        return Objects.equals(this.blocks, that.blocks);
    }

    @Override
    public int hashCode() {
        return Objects.hash(blocks);
    }
}
