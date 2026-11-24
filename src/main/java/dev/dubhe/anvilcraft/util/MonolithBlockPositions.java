package dev.dubhe.anvilcraft.util;

import dev.dubhe.anvilcraft.block.GiantMonolithCoreBlock;
import dev.dubhe.anvilcraft.block.MonolithBlock;
import dev.dubhe.anvilcraft.block.MonolithLineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public final class MonolithBlockPositions {
    // 客户端缩短视距时会直接丢弃部分区块，弱键使索引随区块一同释放。
    private static final Map<LevelChunk, Set<BlockPos>> CHUNKS = new WeakHashMap<>();

    private MonolithBlockPositions() {
    }

    public static boolean isMonolith(BlockState state) {
        return state.getBlock() instanceof MonolithBlock || state.getBlock() instanceof MonolithLineBlock
            || state.getBlock() instanceof GiantMonolithCoreBlock;
    }

    public static void clear() {
        CHUNKS.clear();
    }

    public static void onBlockChanged(LevelChunk chunk, BlockPos pos, BlockState oldState, BlockState newState) {
        boolean wasMonolith = isMonolith(oldState);
        boolean isMonolith = isMonolith(newState);
        if (wasMonolith == isMonolith) {
            return;
        }
        if (isMonolith) {
            CHUNKS.computeIfAbsent(chunk, ignored -> new HashSet<>()).add(pos.immutable());
        } else {
            Set<BlockPos> positions = CHUNKS.get(chunk);
            if (positions == null) {
                return;
            }
            positions.remove(pos);
            if (positions.isEmpty()) {
                CHUNKS.remove(chunk);
            }
        }
    }

    public static void scanChunk(LevelChunk chunk) {
        Set<BlockPos> positions = new HashSet<>();
        var sections = chunk.getSections();
        for (int index = 0; index < sections.length; index++) {
            var section = sections[index];
            if (section.hasOnlyAir() || !section.maybeHas(MonolithBlockPositions::isMonolith)) {
                continue;
            }
            int bottom = chunk.getSectionYFromSectionIndex(index) << 4;
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        if (isMonolith(section.getBlockState(x, y, z))) {
                            positions.add(new BlockPos(chunk.getPos().getMinBlockX() + x, bottom + y,
                                chunk.getPos().getMinBlockZ() + z));
                        }
                    }
                }
            }
        }
        if (positions.isEmpty()) {
            CHUNKS.remove(chunk);
        } else {
            CHUNKS.put(chunk, positions);
        }
    }

    public static void unload(LevelChunk chunk) {
        CHUNKS.remove(chunk);
    }

    public static Iterable<Map.Entry<LevelChunk, Set<BlockPos>>> chunks() {
        return CHUNKS.entrySet();
    }
}
