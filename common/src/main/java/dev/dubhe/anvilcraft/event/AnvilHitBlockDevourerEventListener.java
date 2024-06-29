package dev.dubhe.anvilcraft.event;

import dev.anvilcraft.lib.event.SubscribeEvent;
import dev.dubhe.anvilcraft.api.event.entity.AnvilFallOnLandEvent;
import dev.dubhe.anvilcraft.block.BlockDevourerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class AnvilHitBlockDevourerEventListener {
    /**
     * 侦听铁砧击中方块放置器事件
     *
     * @param event 铁砧落地事件
     */
    @SuppressWarnings("unused")
    @SubscribeEvent
    public void onLand(@NotNull AnvilFallOnLandEvent event) {
        Level level = event.getLevel();
        BlockPos pos = event.getPos().below();
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (block instanceof BlockDevourerBlock devourerBlock && !state.getValue(BlockDevourerBlock.TRIGGERED)) {
            int range = (int) event.getFallDistance() + 2;
            range = Math.min(range, 3);
            level.setBlock(pos, state.setValue(BlockDevourerBlock.TRIGGERED, true), 2);
            if (state.getValue(BlockDevourerBlock.FACING) == Direction.DOWN
                    && level.isOutsideBuildHeight(pos.below())) {
                level.scheduleTick(pos, devourerBlock, 4);
                return;
            }
            devourerBlock.devourBlock(serverLevel, pos,
                    state.getValue(BlockDevourerBlock.FACING), range);
            if (state.getValue(BlockDevourerBlock.FACING) == Direction.DOWN
                    && level.getBlockState(pos.below()).getBlock().defaultDestroyTime() >= 0) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                level.setBlock(pos.below(), state.setValue(BlockDevourerBlock.TRIGGERED, true), 2);
                level.scheduleTick(pos.below(), devourerBlock, 4);
            }
            level.scheduleTick(pos, devourerBlock, 4);
        }
    }
}
