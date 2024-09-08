package dev.dubhe.anvilcraft.event.anvil;

import dev.anvilcraft.lib.event.SubscribeEvent;
import dev.dubhe.anvilcraft.api.event.entity.AnvilFallOnLandEvent;
import dev.dubhe.anvilcraft.block.PiezoelectricCrystalBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

public class AnvilHitPiezoelectricCrystalBlockEventListener {
    /**
     * 侦听铁砧落地事件
     * 用于检测压电晶体
     *
     * @param event 铁砧落地事件
     */
    @SuppressWarnings("unused")
    @SubscribeEvent
    public void onLand(@NotNull AnvilFallOnLandEvent event) {
        BlockPos anvilPos = event.getPos();
        Level level = event.getLevel();
        Block block = level.getBlockState(anvilPos.below()).getBlock();
        if (block instanceof PiezoelectricCrystalBlock piezoelectricCrystalBlock) {
            piezoelectricCrystalBlock.onHitByAnvil(event.getFallDistance(), level, anvilPos.below());
        }
    }
}
