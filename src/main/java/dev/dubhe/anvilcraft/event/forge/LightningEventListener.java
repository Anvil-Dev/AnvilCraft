package dev.dubhe.anvilcraft.event.forge;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.event.entity.LightningStrikeEvent;
import dev.dubhe.anvilcraft.api.event.forge.LightningBoltStrikeEvent;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import org.jetbrains.annotations.NotNull;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class LightningEventListener {
    /**
     * @param event 雷击事件
     */
    @SubscribeEvent
    public static void onLightningStrike(@NotNull LightningBoltStrikeEvent event) {
        AnvilCraft.EVENT_BUS.post(
                new LightningStrikeEvent(event.getEntity(), event.getPos(), event.getLevel()));
    }
}
