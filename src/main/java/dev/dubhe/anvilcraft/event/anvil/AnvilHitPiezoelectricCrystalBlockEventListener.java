package dev.dubhe.anvilcraft.event.anvil;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.event.AnvilEvent;
import dev.dubhe.anvilcraft.block.PiezoelectricCrystalBlock;
import dev.dubhe.anvilcraft.init.ModBlocks;
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
    public static void onLand(@NotNull AnvilEvent.OnLand event) {
        BlockPos anvilPos = event.getPos();
        Level level = event.getLevel();
        Block block = level.getBlockState(anvilPos.below()).getBlock();
        if (event.getEntity().blockState.is(ModBlocks.GIANT_ANVIL.get())) return;
        if (block instanceof PiezoelectricCrystalBlock piezoelectricCrystalBlock) {
            piezoelectricCrystalBlock.onHitByAnvil(event.getEntity(), event.getFallDistance(), level, anvilPos.below());
        }
    }

    /**
     * 侦听大铁砧落地事件
     * 用于检测大铁砧底下3*3的范围内有没有压电晶体
     *
     * @param event 大铁砧落地事件
     */
    @SubscribeEvent
    public static void onLand(@NotNull AnvilEvent.GiantOnLand event) {
        Level level = event.getLevel();
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                BlockPos anvilPos = event.getPos().below(2);
                BlockPos crystalPos = new BlockPos(anvilPos.getX() + i, anvilPos.getY(), anvilPos.getZ() + j);
                Block block = level.getBlockState(crystalPos).getBlock();
                if (block instanceof PiezoelectricCrystalBlock piezoelectricCrystalBlock) {
                    piezoelectricCrystalBlock.onHitByAnvil(event.getEntity(), event.getFallDistance(), level, crystalPos);
                }
            }
        }
    }
}
