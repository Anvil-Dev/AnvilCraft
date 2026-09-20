package dev.dubhe.anvilcraft.building;

import dev.dubhe.anvilcraft.block.RedstoneWireBlock;
import dev.dubhe.anvilcraft.block.RedstoneWireNetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;

import javax.annotation.Nullable;

/** 移植悦灵建造的静默提交，全部状态就位后再恢复方块实体，避免红石和多方块中途更新。 */
public final class BuildingCommit {
    private static final ThreadLocal<Level> QUIET_LEVEL = new ThreadLocal<>();

    private BuildingCommit() {
    }

    /** 仅在本次蓝图提交的调用栈内抑制恢复方块实体引起的间接更新。 */
    public static void quietly(Level level, Runnable action) {
        Level previous = QUIET_LEVEL.get();
        QUIET_LEVEL.set(level);
        try {
            action.run();
        } finally {
            if (previous == null) QUIET_LEVEL.remove();
            else QUIET_LEVEL.set(previous);
        }
    }

    public static boolean isQuiet(Level level) {
        return QUIET_LEVEL.get() == level;
    }

    @Nullable
    public static Level quietLevel() {
        return QUIET_LEVEL.get();
    }

    public static void set(Level level, BlockPos pos, BlockState state) {
        if (!level.isInWorldBounds(pos)) return;
        BlockState previous = level.getBlockState(pos);
        if (previous == state) {
            return;
        }
        BuildingRodUndo.replaced(level, pos, previous, state);
        LevelChunk chunk = level.getChunkAt(pos);
        if (previous.getBlock() instanceof RedstoneWireBlock && !(state.getBlock() instanceof RedstoneWireBlock)) {
            RedstoneWireNetworkManager.wireRemoved(level, pos);
        }
        LevelChunkSection section = chunk.getSection(chunk.getSectionIndex(pos.getY()));
        int localX = pos.getX() & 15;
        int localY = pos.getY() & 15;
        int localZ = pos.getZ() & 15;
        section.setBlockState(localX, localY, localZ, state, false);
        if (previous.hasBlockEntity() && previous.getBlock() != state.getBlock()) {
            chunk.removeBlockEntity(pos);
        }
        chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.MOTION_BLOCKING).update(localX, pos.getY(), localZ, state);
        chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES)
            .update(localX, pos.getY(), localZ, state);
        chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR).update(localX, pos.getY(), localZ, state);
        chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE).update(localX, pos.getY(), localZ, state);
        if (state.hasBlockEntity() && chunk.getBlockEntity(pos) == null && state.getBlock() instanceof EntityBlock entityBlock) {
            BlockEntity created = entityBlock.newBlockEntity(pos, state);
            if (created != null) {
                chunk.addAndRegisterBlockEntity(created);
            }
        } else {
            BlockEntity existing = chunk.getBlockEntity(pos);
            if (existing != null) {
                existing.setBlockState(state);
            }
        }
        chunk.setUnsaved(true);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.getChunkSource().getLightEngine().checkBlock(pos);
            serverLevel.sendBlockUpdated(pos, previous, state,
                Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS);
        }
    }

}
