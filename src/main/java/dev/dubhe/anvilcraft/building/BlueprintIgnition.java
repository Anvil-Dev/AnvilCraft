package dev.dubhe.anvilcraft.building;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.Map;

final class BlueprintIgnition {
    private BlueprintIgnition() {
    }

    static boolean isIgnition(BlockState state) {
        return state.getBlock() instanceof BaseFireBlock || state.is(Blocks.NETHER_PORTAL);
    }

    static Map<BlockPos, BlockPos> portalCores(Map<BlockPos, BlockState> blocks) {
        Map<BlockPos, BlockPos> cores = new LinkedHashMap<>();
        for (var entry : blocks.entrySet()) {
            if (!entry.getValue().is(Blocks.NETHER_PORTAL) || cores.containsKey(entry.getKey())) continue;
            BlockPos core = entry.getKey();
            Direction.Axis axis = entry.getValue().getValue(NetherPortalBlock.AXIS);
            ArrayDeque<BlockPos> pending = new ArrayDeque<>();
            cores.put(core, core);
            pending.add(core);
            while (!pending.isEmpty()) {
                BlockPos pos = pending.removeFirst();
                for (Direction direction : Direction.values()) {
                    if (direction.getAxis() != Direction.Axis.Y && direction.getAxis() != axis) continue;
                    BlockPos next = pos.relative(direction);
                    BlockState state = blocks.get(next);
                    if (cores.containsKey(next) || state == null || !state.is(Blocks.NETHER_PORTAL)
                        || state.getValue(NetherPortalBlock.AXIS) != axis) continue;
                    cores.put(next, core);
                    pending.addLast(next);
                }
            }
        }
        return cores;
    }
}
