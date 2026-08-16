package dev.dubhe.anvilcraft.util;

import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.util.HashSet;
import java.util.Set;

public final class EnchantedGoldBlockPositions {
    private static final Set<BlockPos> POSITIONS = new HashSet<>();

    private EnchantedGoldBlockPositions() {
    }

    public static void clear() {
        POSITIONS.clear();
    }

    public static void onBlockChanged(BlockPos pos, BlockState oldState, BlockState newState) {
        boolean wasEnchanted = oldState.is(ModBlocks.ENCHANTED_GOLD_BLOCK);
        boolean isEnchanted = newState.is(ModBlocks.ENCHANTED_GOLD_BLOCK);
        if (wasEnchanted == isEnchanted) {
            return;
        }
        if (isEnchanted) {
            POSITIONS.add(pos.immutable());
        } else {
            POSITIONS.remove(pos);
        }
    }

    public static void scanChunk(LevelChunk chunk) {
        LevelChunkSection[] sections = chunk.getSections();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int chunkX = chunk.getPos().getMinBlockX();
        int chunkZ = chunk.getPos().getMinBlockZ();
        for (int i = 0; i < sections.length; i++) {
            LevelChunkSection section = sections[i];
            if (section.hasOnlyAir() || !section.maybeHas(state -> state.is(ModBlocks.ENCHANTED_GOLD_BLOCK))) {
                continue;
            }
            int sectionY = chunk.getSectionYFromSectionIndex(i) << 4;
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        cursor.set(chunkX + x, sectionY + y, chunkZ + z);
                        if (chunk.getBlockState(cursor).is(ModBlocks.ENCHANTED_GOLD_BLOCK)) {
                            POSITIONS.add(cursor.immutable());
                        }
                    }
                }
            }
        }
    }

    public static Set<BlockPos> getPositions() {
        return POSITIONS;
    }
}
