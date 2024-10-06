package dev.dubhe.anvilcraft.event.anvil;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.event.anvil.AnvilFallOnLandEvent;
import dev.dubhe.anvilcraft.block.PiezoelectricCrystalBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;

import net.neoforged.fml.common.EventBusSubscriber;
import org.jetbrains.annotations.NotNull;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class AnvilHitPiezoelectricCrystalBlockEventListener {
    /**
     * 侦听铁砧落地事件
     * 用于检测压电晶体
     *
     * @param event 铁砧落地事件
     */
    @SubscribeEvent
    public static void onLand(@NotNull AnvilFallOnLandEvent event) {
        BlockPos anvilPos = event.getPos();
        Level level = event.getLevel();
        Block block = level.getBlockState(anvilPos.below()).getBlock();
        if (block instanceof PiezoelectricCrystalBlock piezoelectricCrystalBlock) {
            piezoelectricCrystalBlock.onHitByAnvil(event.getFallDistance(), level, anvilPos.below());
        }
    }
}
