package dev.dubhe.anvilcraft.event.forge;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.event.entity.LightningStrikeEvent;
import dev.dubhe.anvilcraft.api.event.forge.LightningBoltStrikeEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;

@Mod.EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class LightningEvent {
    /**
     * @param event 雷击事件
     */
    @SubscribeEvent
    public static void onLightningStrike(@NotNull LightningBoltStrikeEvent event) {
        AnvilCraft.EVENT_BUS.post(new LightningStrikeEvent(event.getEntity(), event.getPos(), event.getLevel()));
    }
}
