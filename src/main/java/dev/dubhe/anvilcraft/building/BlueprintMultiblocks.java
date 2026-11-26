package dev.dubhe.anvilcraft.building;

import dev.dubhe.anvilcraft.block.cake.LargeCakeBlock;
import dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock;
import dev.dubhe.anvilcraft.item.block.LargeCakeBlockItem;
import dev.dubhe.anvilcraft.util.BlockPlacementUtil;
import dev.dubhe.anvilcraft.util.BlockPlacementUtil.MultiblockPart;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.piston.MovingPistonBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.PistonType;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;

/** 根据蓝图内的局部坐标识别多方块结构及其被替换的部件。 */
public final class BlueprintMultiblocks {
    private BlueprintMultiblocks() {
    }

    public record PlacedBlock(BlockPos pos, BlockState state, Optional<CompoundTag> nbt) {
    }

    /** 原结构保留任一部件就能恢复整组，替换物占用的空间由放置工具先行处理。 */
    public record PartialGroup(List<PlacedBlock> parts, Map<BlockPos, List<PlacedBlock>> replacementGroups) {
    }

    public static boolean hasParts(BlockState state) {
        return state.getBlock() instanceof AbstractMultiPartBlock<?> || state.getBlock() instanceof LargeCakeBlock
            || state.getBlock() instanceof BedBlock || isDoubleBlock(state) || state.getBlock() instanceof PistonHeadBlock
            || state.getBlock() instanceof PistonBaseBlock && state.getValue(PistonBaseBlock.EXTENDED);
    }

    public static List<PartialGroup> partialGroups(StructureSnapshot snapshot) {
        Map<BlockPos, StructureSnapshot.BlockEntry> byPos = new HashMap<>();
        snapshot.blocks().forEach(entry -> byPos.put(entry.pos(), entry));
        Set<BlockPos> checked = new HashSet<>();
        List<PartialGroup> groups = new ArrayList<>();
        for (var entry : snapshot.blocks()) {
            BlockState state = snapshot.stateOf(entry);
            if (entry.replacedPart().isPresent() || !hasParts(state) || checked.contains(entry.pos())) {
                continue;
            }
            var expected = expectedParts(entry.pos(), state);
            BlockPos core = core(entry.pos(), state);
            var coreEntry = byPos.get(core);
            if (coreEntry != null && coreEntry.replacedPart().isEmpty()
                && coreEntry != entry && snapshot.stateOf(coreEntry).is(state.getBlock())
                && core(core, snapshot.stateOf(coreEntry)).equals(core)) {
                expected = expectedParts(core, snapshot.stateOf(coreEntry));
            }
            var group = partialGroup(snapshot, byPos, expected, checked);
            if (group != null) {
                groups.add(group);
            }
        }
        return List.copyOf(groups);
    }

    private static @Nullable PartialGroup partialGroup(
        StructureSnapshot snapshot, Map<BlockPos, StructureSnapshot.BlockEntry> byPos, List<MultiblockPart> expected,
        Set<BlockPos> checked
    ) {
        List<PlacedBlock> parts = new ArrayList<>(expected.size());
        Map<BlockPos, List<PlacedBlock>> replacements = new LinkedHashMap<>();
        boolean missingPart = false;
        boolean markedReplacement = false;
        for (var part : expected) {
            var recorded = byPos.get(part.pos());
            if (recorded == null) {
                missingPart = true;
                parts.add(new PlacedBlock(part.pos(), part.state(), Optional.empty()));
                continue;
            }
            BlockState actual = snapshot.stateOf(recorded);
            if (recorded.replacedPart().isEmpty() && matchesPart(actual, part.state())) {
                checked.add(part.pos());
                parts.add(new PlacedBlock(part.pos(), actual, recorded.nbt()));
                continue;
            }
            BlockState original = recorded.replacedPart().orElse(part.state());
            if (!matchesPart(original, part.state())
                || actual.getBlock() instanceof MovingPistonBlock && part.state().getBlock() instanceof PistonHeadBlock) {
                return null;
            }
            markedReplacement |= recorded.replacedPart().isPresent();
            parts.add(new PlacedBlock(part.pos(), original, Optional.empty()));
            replacements.put(part.pos(), expectedParts(part.pos(), actual).stream().map(expanded ->
                expanded.pos().equals(part.pos()) ? new PlacedBlock(part.pos(), actual, recorded.nbt())
                    : new PlacedBlock(expanded.pos(), expanded.state(), Optional.empty())).toList());
        }
        if (replacements.isEmpty() || missingPart && !markedReplacement) {
            return null;
        }
        return new PartialGroup(List.copyOf(parts), Collections.unmodifiableMap(replacements));
    }

    public static boolean matchesPart(BlockState actual, BlockState expected) {
        return actual.is(expected.getBlock()) && BlueprintNormalizer.repair(actual, expected) == actual;
    }

    /** 以指定部件作为坐标基准，列出完整结构的方块状态和位置。 */
    public static List<MultiblockPart> expectedParts(BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof LargeCakeBlock) {
            List<MultiblockPart> parts = new ArrayList<>();
            LargeCakeBlockItem.forEachPlacedBlock(core(pos, state), state, (at, part) -> parts.add(new MultiblockPart(at, part)));
            return parts;
        }
        if (state.getBlock() instanceof PistonHeadBlock
            || state.getBlock() instanceof PistonBaseBlock && state.getValue(PistonBaseBlock.EXTENDED)) {
            BlockState base = state;
            if (state.getBlock() instanceof PistonHeadBlock) {
                base = (state.getValue(PistonHeadBlock.TYPE) == PistonType.STICKY ? Blocks.STICKY_PISTON : Blocks.PISTON)
                    .defaultBlockState().setValue(PistonBaseBlock.EXTENDED, true)
                    .setValue(PistonBaseBlock.FACING, state.getValue(PistonHeadBlock.FACING));
            }
            List<MultiblockPart> parts = new ArrayList<>(2);
            forEachPart(core(pos, state), base, (at, part) -> parts.add(new MultiblockPart(at, part)));
            return parts;
        }
        return BlockPlacementUtil.getExpectedMultiblockParts(pos, state);
    }

    public static boolean shouldRecord(BlockState state) {
        if (state.getBlock() instanceof AbstractMultiPartBlock<?> block) return block.isMainPart(state);
        if (isDoubleBlock(state)) return state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER;
        if (state.getBlock() instanceof BedBlock) return state.getValue(BedBlock.PART) == BedPart.FOOT;
        return !(state.getBlock() instanceof PistonHeadBlock);
    }

    public static BlockPos core(BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof LargeCakeBlock) {
            return LargeCakeBlockItem.origin(pos, state);
        }
        if (state.getBlock() instanceof AbstractMultiPartBlock<?> block) return block.getMainPartPos(pos, state);
        if (isDoubleBlock(state) && !shouldRecord(state)) return pos.below();
        if (state.getBlock() instanceof BedBlock && !shouldRecord(state)) {
            return pos.relative(state.getValue(BedBlock.FACING).getOpposite());
        }
        if (state.getBlock() instanceof PistonHeadBlock) {
            return pos.relative(state.getValue(PistonHeadBlock.FACING).getOpposite());
        }
        return pos;
    }

    private static boolean isDoubleBlock(BlockState state) {
        return state.getBlock() instanceof DoorBlock || state.getBlock() instanceof DoublePlantBlock;
    }

    public static void forEachPart(BlockPos pos, BlockState state, BiConsumer<BlockPos, BlockState> consumer) {
        if (state.getBlock() instanceof AbstractMultiPartBlock<?> block && block.isMainPart(state)) {
            addParts(pos, state, block, consumer);
            return;
        }
        consumer.accept(pos, state);
        if (!shouldRecord(state)) return;
        if (isDoubleBlock(state)) {
            consumer.accept(pos.above(), state.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER));
        } else if (state.getBlock() instanceof BedBlock) {
            consumer.accept(pos.relative(state.getValue(BedBlock.FACING)), state.setValue(BedBlock.PART, BedPart.HEAD));
        } else if (state.getBlock() instanceof PistonBaseBlock && state.getValue(PistonBaseBlock.EXTENDED)) {
            consumer.accept(pos.relative(state.getValue(PistonBaseBlock.FACING)), Blocks.PISTON_HEAD.defaultBlockState()
                .setValue(PistonHeadBlock.FACING, state.getValue(PistonBaseBlock.FACING))
                .setValue(PistonHeadBlock.TYPE, state.is(Blocks.STICKY_PISTON) ? PistonType.STICKY : PistonType.DEFAULT));
        }
    }

    public static void forEachPart(
        BlockPos pos, BlockState state, BlueprintPlacement placement, BiConsumer<BlockPos, BlockState> consumer
    ) {
        forEachPart(pos, state, (partPos, partState) -> consumer.accept(placement.worldOf(partPos), placement.stateOf(partState)));
    }

    private static <P extends Enum<P>> void addParts(
        BlockPos pos, BlockState state, AbstractMultiPartBlock<P> block, BiConsumer<BlockPos, BlockState> consumer
    ) {
        for (P part : block.getParts()) consumer.accept(pos.offset(block.offsetFrom(state, part)), block.placedState(part, state));
    }

    public static List<PlacedBlock> expand(StructureSnapshot snapshot, BlueprintPlacement placement, int layer) {
        List<PlacedBlock> blocks = new ArrayList<>();
        for (var entry : snapshot.blocks()) {
            if (layer >= 0 && entry.pos().getY() != layer) continue;
            BlockState state = snapshot.stateOf(entry);
            if (OrdinaryBlockAdapter.mapping(state) == OrdinaryBlockAdapter.Mapping.AIR) continue;
            blocks.add(new PlacedBlock(placement.worldOf(entry.pos()), placement.stateOf(state), entry.nbt()));
        }
        return List.copyOf(blocks);
    }
}
