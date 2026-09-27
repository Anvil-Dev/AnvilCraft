package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class LaserFogProbe {
    private static boolean logged;

    @SubscribeEvent
    public static void fog(ViewportEvent.RenderFog event) {
        if (!Boolean.getBoolean("anvilcraft.portLaserScene")) return;
        var client = Minecraft.getInstance();
        if (client.player == null || client.player.getY() < 164) return;
        var fog = event.getFogData();
        if (!logged) {
            AnvilCraft.LOGGER.info("PORT_LASER_FOG: environmental={}..{}, render={}..{}, color={}",
                fog.environmentalStart, fog.environmentalEnd, fog.renderDistanceStart, fog.renderDistanceEnd, fog.color);
            logged = true;
        }
        if (Boolean.getBoolean("anvilcraft.portLaserNoFog")) {
            fog.environmentalStart = 10000;
            fog.environmentalEnd = 20000;
            fog.renderDistanceStart = 10000;
            fog.renderDistanceEnd = 20000;
        }
    }
}
