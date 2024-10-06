package dev.dubhe.anvilcraft.event.anvil;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.event.anvil.AnvilFallOnLandEvent;
import dev.dubhe.anvilcraft.block.BlockPlacerBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;

import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class AnvilHitBlockPlacerEventListener {
    /**
     * 侦听铁砧击中方块放置器事件
     *
     * @param event 铁砧落地事件
     */
    @SubscribeEvent
    public static void onLand(AnvilFallOnLandEvent event) {
        Level level = event.getLevel();
        BlockPos pos = event.getPos().below();
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        if (block instanceof BlockPlacerBlock blockPlacerBlock) {
            int distance = (int) event.getFallDistance() + 2;
            distance = Math.min(distance, 5);
            level.setBlock(pos, state.setValue(BlockPlacerBlock.TRIGGERED, true), 2);
            blockPlacerBlock.placeBlock(distance, level, pos, state.getValue(BlockPlacerBlock.ORIENTATION));
            level.scheduleTick(pos, blockPlacerBlock, 4);
        }
    }
}
