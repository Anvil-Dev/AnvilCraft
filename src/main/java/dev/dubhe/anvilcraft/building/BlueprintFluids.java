package dev.dubhe.anvilcraft.building;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.Shapes;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

/** 只读检查液体源的可达范围，不模拟液位、不产生任何流体更新。 */
public final class BlueprintFluids {
    private BlueprintFluids() {
    }

    public static boolean isFlowing(BlockState state) {
        return state.getBlock() instanceof LiquidBlock && !state.getFluidState().isSource();
    }

    public static Set<BlockPos> reachable(Map<BlockPos, BlockState> blocks) {
        Set<BlockPos> reached = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        blocks.forEach((pos, state) -> {
            if (state.getFluidState().isSource()) {
                reached.add(pos);
                queue.add(pos);
            }
        });
        BlockGetter view = new View(blocks);
        while (!queue.isEmpty()) {
            BlockPos pos = queue.removeFirst();
            BlockState state = blocks.get(pos);
            for (Direction direction : Direction.values()) {
                if (direction == Direction.UP) continue;
                BlockPos next = pos.relative(direction);
                BlockState target = blocks.get(next);
                if (target == null || reached.contains(next) || target.getFluidState().isEmpty()
                    || !state.getFluidState().getType().isSame(target.getFluidState().getType())) continue;
                if (Shapes.mergedFaceOccludes(state.getCollisionShape(view, pos), target.getCollisionShape(view, next), direction)) {
                    continue;
                }
                reached.add(next);
                queue.addLast(next);
            }
        }
        return reached;
    }

    private record View(Map<BlockPos, BlockState> blocks) implements BlockGetter {
        @Override
        public BlockState getBlockState(BlockPos pos) {
            return this.blocks.getOrDefault(pos, Blocks.AIR.defaultBlockState());
        }

        @Override
        public FluidState getFluidState(BlockPos pos) {
            return this.getBlockState(pos).getFluidState();
        }

        @Nullable
        @Override
        public BlockEntity getBlockEntity(BlockPos pos) {
            return null;
        }

        @Override
        public int getHeight() {
            return 4096;
        }

        @Override
        public int getMinBuildHeight() {
            return -2048;
        }
    }
}
