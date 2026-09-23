package dev.dubhe.anvilcraft.building;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.piston.MovingPistonBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** 扫描、导入和旧磁盘共用的完整化入口，所有修复发生在写入世界之前。 */
public final class BlueprintNormalizer {
    private static final Cache<LoadKey, StructureSnapshot> CACHE = CacheBuilder.newBuilder().maximumSize(32).build();

    private BlueprintNormalizer() {
    }

    private record LoadKey(CompoundTag tag, HolderLookup.Provider registries, Direction facing, boolean upsideDown) {
    }

    public record Result(StructureSnapshot snapshot, BlockPos offset, int added, int removed) {
    }

    public static StructureSnapshot load(CompoundTag tag, HolderLookup.Provider registries, Direction facing, boolean upsideDown)
        throws ConstructionBlueprintException {
        LoadKey key = new LoadKey(tag, registries, facing, upsideDown);
        StructureSnapshot cached = CACHE.getIfPresent(key);
        if (cached != null) return cached;
        StructureSnapshot parsed = StructureSnapshotCodec.parse(tag, registries).snapshot();
        StructureSnapshot normalized = normalize(ScannerDiskNormalizer.normalize(parsed, facing, upsideDown)).snapshot();
        CACHE.put(new LoadKey(tag.copy(), registries, facing, upsideDown), normalized);
        return normalized;
    }

    public static Result normalize(StructureSnapshot snapshot) {
        Map<BlockPos, BlueprintMultiblocks.PlacedBlock> original = new LinkedHashMap<>();
        for (var entry : snapshot.blocks()) {
            BlockState state = snapshot.stateOf(entry);
            if (OrdinaryBlockAdapter.mapping(state) != OrdinaryBlockAdapter.Mapping.AIR) {
                original.put(entry.pos(), new BlueprintMultiblocks.PlacedBlock(entry.pos(), state, entry.nbt()));
            }
        }
        Map<BlockPos, BlueprintMultiblocks.PlacedBlock> complete = new LinkedHashMap<>();
        for (var entry : original.values()) {
            if (!BlueprintMultiblocks.shouldRecord(entry.state())) continue;
            BlueprintMultiblocks.forEachPart(entry.pos(), entry.state(), (pos, generated) -> {
                var previous = original.get(pos);
                if (previous != null && previous.state().getBlock() instanceof MovingPistonBlock
                    && !pos.equals(entry.pos())) return;
                if (complete.containsKey(pos) || previous != null && !pos.equals(entry.pos())
                    && (BlueprintMultiblocks.shouldRecord(previous.state()) || !sameFamily(previous.state(), generated))) {
                    throw new IllegalArgumentException("Overlapping blueprint parts at " + pos.toShortString());
                }
                BlockState state = previous == null || pos.equals(entry.pos()) ? generated : repair(previous.state(), generated);
                Optional<CompoundTag> nbt = previous == null || previous.state().getBlock() != generated.getBlock()
                    ? Optional.empty() : previous.nbt();
                complete.put(pos, new BlueprintMultiblocks.PlacedBlock(pos, state, nbt));
            });
        }
        Map<BlockPos, BlockState> states = new LinkedHashMap<>();
        Map<Long, BlockState> packed = new LinkedHashMap<>();
        complete.forEach((pos, block) -> {
            states.put(pos, block.state());
            packed.put(pos.asLong(), block.state());
        });
        var reachable = BlueprintFluids.reachable(states);
        complete.entrySet().removeIf(entry -> BlueprintFluids.isFlowing(entry.getValue().state()) && !reachable.contains(entry.getKey()));
        complete.replaceAll((pos, block) -> new BlueprintMultiblocks.PlacedBlock(pos,
            OrdinaryBlockAdapter.projectionState(block.state(), pos, packed), block.nbt()));
        BlockPos min = BlockPos.ZERO;
        BlockPos max = new BlockPos(snapshot.size()).offset(-1, -1, -1);
        for (BlockPos pos : complete.keySet()) {
            min = new BlockPos(Math.min(min.getX(), pos.getX()), Math.min(min.getY(), pos.getY()), Math.min(min.getZ(), pos.getZ()));
            max = new BlockPos(Math.max(max.getX(), pos.getX()), Math.max(max.getY(), pos.getY()), Math.max(max.getZ(), pos.getZ()));
            if (max.getX() - min.getX() >= StructureSnapshotCodec.MAX_AXIS
                || max.getY() - min.getY() >= StructureSnapshotCodec.MAX_AXIS
                || max.getZ() - min.getZ() >= StructureSnapshotCodec.MAX_AXIS) {
                throw new IllegalArgumentException("Complete blueprint exceeds 16x16x16 at " + pos.toShortString());
            }
        }
        BlockPos offset = BlockPos.ZERO.subtract(min);
        Vec3i size = max.subtract(min).offset(1, 1, 1);
        List<BlockState> palette = new ArrayList<>();
        Map<BlockState, Integer> indices = new LinkedHashMap<>();
        List<StructureSnapshot.BlockEntry> blocks = new ArrayList<>();
        for (var block : complete.values()) {
            int index = indices.computeIfAbsent(block.state(), state -> {
                palette.add(state);
                return palette.size() - 1;
            });
            blocks.add(new StructureSnapshot.BlockEntry(block.pos().offset(offset), index,
                block.nbt().map(CompoundTag::copy)));
        }
        if (palette.isEmpty()) palette.add(Blocks.AIR.defaultBlockState());
        List<StructureSnapshot.EntityEntry> entities = snapshot.entities().stream().map(entry ->
            new StructureSnapshot.EntityEntry(entry.pos().add(Vec3.atLowerCornerOf(offset)), entry.blockPos().offset(offset),
                entry.nbt().copy())).toList();
        int added = (int) complete.keySet().stream().filter(pos -> !original.containsKey(pos)).count();
        int removed = (int) original.keySet().stream().filter(pos -> !complete.containsKey(pos)).count();
        return new Result(StructureSnapshotCodec.canonicalize(new StructureSnapshot(size, palette, blocks, entities,
            snapshot.ticks().stream().map(tick -> tick.at(tick.pos().offset(offset))).toList(), snapshot.capturedAt())),
            offset, added, removed);
    }

    private static BlockState repair(BlockState recorded, BlockState generated) {
        if (recorded.getBlock() != generated.getBlock()) return generated;
        BlockState result = recorded;
        for (Property<?> property : generated.getProperties()) {
            boolean structural = property.getName().equals("half") || property.getName().equals("part")
                || property.getName().equals("type") || property == DoorBlock.HINGE
                || generated.getValue(property) instanceof Direction || generated.getValue(property) instanceof Direction.Axis;
            if (generated.getBlock() instanceof AbstractMultiPartBlock<?> multipart && property == multipart.getPart()) structural = true;
            if (structural) result = copyProperty(result, generated, property);
        }
        return result;
    }

    private static boolean sameFamily(BlockState recorded, BlockState generated) {
        return recorded.getBlock() == generated.getBlock()
            || recorded.getBlock() instanceof BedBlock && generated.getBlock() instanceof BedBlock
            || recorded.getBlock() instanceof DoorBlock && generated.getBlock() instanceof DoorBlock
            || recorded.getBlock() instanceof DoublePlantBlock && generated.getBlock() instanceof DoublePlantBlock;
    }

    private static <T extends Comparable<T>> BlockState copyProperty(BlockState target, BlockState source, Property<T> property) {
        return target.setValue(property, source.getValue(property));
    }
}
