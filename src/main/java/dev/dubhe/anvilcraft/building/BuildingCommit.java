package dev.dubhe.anvilcraft.building;

import dev.dubhe.anvilcraft.block.RedstoneWireBlock;
import dev.dubhe.anvilcraft.block.RedstoneWireNetworkManager;
import dev.dubhe.anvilcraft.block.entity.PulseGeneratorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.ObserverBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

/** 移植悦灵建造的静默提交，全部状态就位后再恢复方块实体，避免红石和多方块中途更新。 */
public final class BuildingCommit {
    private static final ThreadLocal<Level> QUIET_LEVEL = new ThreadLocal<>();
    private static final ThreadLocal<Activation> ACTIVATION = new ThreadLocal<>();

    private record Activation(Level level, Map<BlockPos, BlockState> states) {
    }

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

    public static void activate(ServerLevel level, List<BuildingRodService.Cell> cells, List<BlueprintTicks.Entry> ticks) {
        Map<BlockPos, BlockState> states = cells.stream()
            .collect(Collectors.toMap(BuildingRodService.Cell::pos, BuildingRodService.Cell::state));
        Activation previous = ACTIVATION.get();
        ACTIVATION.set(new Activation(level, states));
        try {
            activatePlaced(level, cells, ticks, states.keySet());
        } finally {
            if (previous == null) ACTIVATION.remove();
            else ACTIVATION.set(previous);
        }
    }

    public static void activate(ServerLevel level, List<BuildingRodService.Cell> cells) {
        activatePlaced(level, cells);
    }

    public static boolean isRestoredObserverUpdate(Level level, Direction direction, BlockState source,
                                                    BlockPos pos, BlockPos sourcePos) {
        Activation activation = ACTIVATION.get();
        if (activation == null || activation.level() != level) return false;
        BlockState observer = activation.states().get(pos);
        return observer != null && observer.getBlock() instanceof ObserverBlock
            && observer.getValue(ObserverBlock.FACING) == direction && level.getBlockState(pos) == observer
            && activation.states().get(sourcePos) == source && level.getBlockState(sourcePos) == source;
    }

    private static void activatePlaced(ServerLevel level, List<BuildingRodService.Cell> cells,
                                       List<BlueprintTicks.Entry> ticks, Set<BlockPos> placed) {
        for (BlockPos pos : placed) {
            BoundingBox bounds = new BoundingBox(pos);
            level.getBlockTicks().clearArea(bounds);
            level.getFluidTicks().clearArea(bounds);
        }
        BlueprintTicks.restore(level, ticks, placed);
        activatePlaced(level, cells);
    }

    private static void activatePlaced(ServerLevel level, List<BuildingRodService.Cell> cells) {
        for (var cell : cells) {
            BlockPos pos = cell.pos();
            BlockState state = level.getBlockState(pos);
            state.onPlace(level, pos, Blocks.AIR.defaultBlockState(), false);
            if (level.getBlockEntity(pos) instanceof PulseGeneratorBlockEntity pulse
                && pulse.isProcessing() && !level.getBlockTicks().hasScheduledTick(pos, state.getBlock())) {
                level.scheduleTick(pos, state.getBlock(), Math.max(1, pulse.getPhaseRemainingTicks()));
            }
            if (!state.getFluidState().isEmpty()) {
                var fluid = state.getFluidState().getType();
                level.scheduleTick(pos, fluid, fluid.getTickDelay(level));
            }
        }
        for (var cell : cells) {
            BlockPos pos = cell.pos();
            BlockState state = level.getBlockState(pos);
            state.updateNeighbourShapes(level, pos, Block.UPDATE_ALL);
            state.updateIndirectNeighbourShapes(level, pos, Block.UPDATE_ALL);
            level.updateNeighborsAt(pos, state.getBlock());
            level.updateNeighbourForOutputSignal(pos, state.getBlock());
            for (var direction : Direction.values()) {
                BlockPos neighbor = pos.relative(direction);
                level.neighborChanged(pos, level.getBlockState(neighbor).getBlock(), neighbor);
            }
        }
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
            serverLevel.onBlockStateChange(pos, previous, state);
            state.onBlockStateChange(level, pos, previous);
            serverLevel.getChunkSource().getLightEngine().checkBlock(pos);
            serverLevel.sendBlockUpdated(pos, previous, state,
                Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS);
        }
    }

}
