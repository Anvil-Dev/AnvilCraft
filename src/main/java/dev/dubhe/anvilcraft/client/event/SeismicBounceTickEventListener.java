package dev.dubhe.anvilcraft.client.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.support.AnvilParticleManager;
import dev.dubhe.anvilcraft.client.support.ScreenShakeManager;
import dev.dubhe.anvilcraft.client.support.SeismicBounceManager;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public class SeismicBounceTickEventListener {

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Pre e) {
        AnvilParticleManager.tick();
        if (Minecraft.getInstance().isPaused()) return;
        SeismicBounceManager.getInstance().tick();
        ScreenShakeManager.getInstance().tick();
    }
}
