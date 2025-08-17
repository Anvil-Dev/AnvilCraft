package dev.dubhe.anvilcraft.api.sliding;

import com.google.common.collect.Lists;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;

import java.util.List;

/**
 * No-pistonPos-Check PistonStructureResolver
 */
public class SlidingBlockStructureResolver {
    public static int MAX_PUSH_DEPTH = 12;
    private final Level level;
    private final boolean extending;
    private final BlockPos startPos;
    @Getter
    private final Direction pushDirection;
    /**
     * All block positions to be moved by the piston
     */
    @Getter
    private final List<BlockPos> toPush = Lists.newArrayList();
    /**
     * All blocks to be destroyed by the piston
     */
    @Getter
    private final List<BlockPos> toDestroy = Lists.newArrayList();
    private final Direction pistonDirection;

    public SlidingBlockStructureResolver(Level level, BlockPos startPos, Direction pistonDirection, boolean extending) {
        this.level = level;
        this.startPos = startPos;
        this.pistonDirection = pistonDirection;
        this.extending = extending;
        if (extending) {
            this.pushDirection = pistonDirection;
        } else {
            this.pushDirection = pistonDirection.getOpposite();
        }
    }

    public boolean resolve() {
        this.toPush.clear();
        this.toDestroy.clear();
        BlockState blockstate = this.level.getBlockState(this.startPos);
        if (!PistonBaseBlock.isPushable(blockstate, this.level, this.startPos, this.pushDirection, false, this.pistonDirection)) {
            if (!this.extending || blockstate.getPistonPushReaction() != PushReaction.DESTROY) return false;
            this.toDestroy.add(this.startPos);
            return true;
        } else if (!this.addBlockLine(this.startPos, this.pushDirection)) {
            return false;
        } else {
            // 若替换为增强的for循环，则会报ConcurrentModificationException错误
            //noinspection ForLoopReplaceableByForEach
            for (int i = 0; i < this.toPush.size(); i++) {
                BlockPos blockpos = this.toPush.get(i);
                if (this.level.getBlockState(blockpos).isStickyBlock() && !this.addBranchingBlocks(blockpos)) return false;
            }
            return true;
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean addBlockLine(BlockPos originPos, Direction direction) {
        BlockState nowState = this.level.getBlockState(originPos);
        if (nowState.isAir()
            || !PistonBaseBlock.isPushable(nowState, this.level, originPos, this.pushDirection, false, direction)
            || this.toPush.contains(originPos)
        ) return true;

        int toPushSize = 1;
        if (toPushSize + this.toPush.size() > MAX_PUSH_DEPTH) return false;

        BlockState oldState;
        while (nowState.isStickyBlock()) {
            BlockPos nowPos = originPos.relative(this.pushDirection.getOpposite(), toPushSize);
            BlockPos oldPos = nowPos.relative(this.pushDirection);
            oldState = nowState;
            nowState = this.level.getBlockState(nowPos);
            if (nowState.isAir()
                || !(oldState.canStickTo(oldPos, nowPos, nowState)
                    && nowState.canStickTo(nowPos, oldPos, oldState))
                || !PistonBaseBlock.isPushable(
                    nowState, this.level, nowPos, this.pushDirection, false, this.pushDirection.getOpposite())
            ) break;

            if (++toPushSize + this.toPush.size() > MAX_PUSH_DEPTH) return false;
        }

        int addedCount = 0;

        for (int i = toPushSize - 1; i >= 0; i--) {
            this.toPush.add(originPos.relative(this.pushDirection.getOpposite(), i));
            addedCount++;
        }

        int addingIndex = 1;

        while (true) {
            BlockPos addingPos = originPos.relative(this.pushDirection, addingIndex);
            int addingToPushExist = this.toPush.indexOf(addingPos);
            if (addingToPushExist > -1) {
                this.reorderListAtCollision(addedCount, addingToPushExist);

                for (int i = 0; i <= addingToPushExist + addedCount; i++) {
                    BlockPos toPushPos = this.toPush.get(i);
                    if (this.level.getBlockState(toPushPos).isStickyBlock() && !this.addBranchingBlocks(toPushPos)) return false;
                }

                return true;
            }

            nowState = this.level.getBlockState(addingPos);
            if (nowState.isAir()) return true;

            if (!PistonBaseBlock.isPushable(
                nowState, this.level, addingPos, this.pushDirection, true, this.pushDirection)
            ) return false;

            if (nowState.getPistonPushReaction() == PushReaction.DESTROY) {
                this.toDestroy.add(addingPos);
                return true;
            }

            if (this.toPush.size() >= MAX_PUSH_DEPTH) return false;

            this.toPush.add(addingPos);
            addedCount++;
            addingIndex++;
        }
    }

    private void reorderListAtCollision(int offsets, int index) {
        List<BlockPos> list = Lists.newArrayList();
        List<BlockPos> list1 = Lists.newArrayList();
        List<BlockPos> list2 = Lists.newArrayList();
        list.addAll(this.toPush.subList(0, index));
        list1.addAll(this.toPush.subList(this.toPush.size() - offsets, this.toPush.size()));
        list2.addAll(this.toPush.subList(index, this.toPush.size() - offsets));
        this.toPush.clear();
        this.toPush.addAll(list);
        this.toPush.addAll(list1);
        this.toPush.addAll(list2);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean addBranchingBlocks(BlockPos fromPos) {
        BlockState fromState = this.level.getBlockState(fromPos);

        for (Direction dir : Direction.values()) {
            if (dir.getAxis() != this.pushDirection.getAxis()) {
                BlockPos branchPos = fromPos.relative(dir);
                BlockState branchState = this.level.getBlockState(branchPos);
                if (branchState.canStickTo(branchPos, fromPos, fromState)
                    && fromState.canStickTo(fromPos, branchPos, branchState)
                    && !this.addBlockLine(branchPos, dir)
                ) return false;
            }
        }

        return true;
    }

}
