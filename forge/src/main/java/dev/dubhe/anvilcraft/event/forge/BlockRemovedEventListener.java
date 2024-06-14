package dev.dubhe.anvilcraft.event.forge;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.chargecollector.HeatedBlockRecorder;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class BlockRemovedEventListener {
    @SubscribeEvent
    void onBlockBreak(BlockEvent.BreakEvent breakEvent) {
        HeatedBlockRecorder.INSTANCE.delete(breakEvent.getPos());
    }
}
