package dev.dubhe.anvilcraft.building;

import dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

/** 蓝图只保存核心；部件按变换后的核心重新组装，展开范围不受蓝图记录尺寸裁剪。 */
public final class BlueprintMultiblocks {
    private BlueprintMultiblocks() {
    }

    public record PlacedBlock(BlockPos pos, BlockState state, Optional<CompoundTag> nbt) {
    }

    public static boolean shouldRecord(BlockState state) {
        return !(state.getBlock() instanceof AbstractMultiPartBlock<?> block) || block.isMainPart(state);
    }

    public static void forEachPart(BlockPos pos, BlockState state, BiConsumer<BlockPos, BlockState> consumer) {
        forEachPart(pos, state, new BlueprintPlacement(BlockPos.ZERO, Rotation.NONE, Mirror.NONE), consumer);
    }

    public static void forEachPart(
        BlockPos pos, BlockState state, BlueprintPlacement placement, BiConsumer<BlockPos, BlockState> consumer
    ) {
        BlockPos transformedPos = placement.worldOf(pos);
        BlockState transformedState = placement.stateOf(state);
        if (state.getBlock() instanceof AbstractMultiPartBlock<?> block && block.isMainPart(state)) {
            addParts(block.getMainPartPos(transformedPos, transformedState), state, transformedState, block, consumer);
        } else {
            consumer.accept(transformedPos, transformedState);
        }
    }

    private static <P extends Enum<P>> void addParts(
        BlockPos pos, BlockState original, BlockState transformed, AbstractMultiPartBlock<P> block,
        BiConsumer<BlockPos, BlockState> consumer
    ) {
        BlockState core = block.placedState(original.getValue(block.getPart()), transformed);
        for (P part : block.getParts()) {
            consumer.accept(pos.offset(block.offsetFrom(core, part)), block.placedState(part, core));
        }
    }

    private static <P extends Enum<P>> BlockState partState(
        BlockState recorded, BlockState generated, AbstractMultiPartBlock<P> block
    ) {
        return recorded.setValue(block.getPart(), generated.getValue(block.getPart()));
    }

    public static List<PlacedBlock> expand(StructureSnapshot snapshot, BlueprintPlacement placement, int layer) {
        Map<BlockPos, PlacedBlock> blocks = new LinkedHashMap<>();
        Map<BlockPos, BlockPos> owners = new HashMap<>();
        for (var entry : snapshot.blocks()) {
            if (layer >= 0 && entry.pos().getY() != layer) continue;
            BlockState original = snapshot.stateOf(entry);
            if (OrdinaryBlockAdapter.mapping(original) == OrdinaryBlockAdapter.Mapping.AIR) continue;
            BlockPos pos = placement.worldOf(entry.pos());
            blocks.put(pos, new PlacedBlock(pos, placement.stateOf(original), entry.nbt()));
            if (original.getBlock() instanceof AbstractMultiPartBlock<?> block) {
                owners.put(pos, block.getMainPartPos(entry.pos(), original));
            }
        }
        for (var entry : snapshot.blocks()) {
            if (layer >= 0 && entry.pos().getY() != layer) continue;
            BlockState original = snapshot.stateOf(entry);
            if (!(original.getBlock() instanceof AbstractMultiPartBlock<?> block) || !block.isMainPart(original)) continue;
            BlockPos originalPos = placement.worldOf(entry.pos());
            forEachPart(entry.pos(), original, placement, (pos, state) -> {
                PlacedBlock previous = blocks.get(pos);
                if (previous != null && (!previous.state().is(block) || !entry.pos().equals(owners.get(pos)))) {
                    throw new IllegalArgumentException("Overlapping blueprint multiblocks at " + pos);
                }
                Optional<CompoundTag> nbt = previous == null || pos.equals(originalPos) ? Optional.empty() : previous.nbt();
                if (block.isMainPart(state)) nbt = entry.nbt();
                blocks.put(pos, new PlacedBlock(pos, previous == null ? state : partState(previous.state(), state, block), nbt));
                owners.put(pos, entry.pos());
            });
        }
        return List.copyOf(blocks.values());
    }
}
