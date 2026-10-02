package dev.dubhe.anvilcraft.util;

import dev.dubhe.anvilcraft.block.GiantMonolithCoreBlock;
import dev.dubhe.anvilcraft.block.MonolithBlock;
import dev.dubhe.anvilcraft.block.MonolithLineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class MonolithBlockPositions {
    private static final Map<Long, Set<BlockPos>> CHUNKS = new HashMap<>();

    private MonolithBlockPositions() {
    }

    public static boolean isMonolith(BlockState state) {
        return state.getBlock() instanceof MonolithBlock || state.getBlock() instanceof MonolithLineBlock
            || state.getBlock() instanceof GiantMonolithCoreBlock;
    }

    public static void clear() {
        CHUNKS.clear();
    }

    public static void onBlockChanged(BlockPos pos, BlockState oldState, BlockState newState) {
        boolean wasMonolith = isMonolith(oldState);
        boolean isMonolith = isMonolith(newState);
        if (wasMonolith == isMonolith) return;
        long chunk = ChunkPos.pack(pos);
        if (isMonolith) {
            CHUNKS.computeIfAbsent(chunk, ignored -> new HashSet<>()).add(pos.immutable());
        } else {
            Set<BlockPos> positions = CHUNKS.get(chunk);
            if (positions == null) return;
            positions.remove(pos);
            if (positions.isEmpty()) CHUNKS.remove(chunk);
        }
    }

    public static void scanChunk(LevelChunk chunk) {
        Set<BlockPos> positions = new HashSet<>();
        var sections = chunk.getSections();
        for (int index = 0; index < sections.length; index++) {
            var section = sections[index];
            if (section.hasOnlyAir() || !section.maybeHas(MonolithBlockPositions::isMonolith)) continue;
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
        long key = chunk.getPos().pack();
        if (positions.isEmpty()) CHUNKS.remove(key);
        else CHUNKS.put(key, positions);
    }

    public static void unload(ChunkPos chunk) {
        CHUNKS.remove(chunk.pack());
    }

    public static Iterable<Set<BlockPos>> chunks() {
        return CHUNKS.values();
    }
}
