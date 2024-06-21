package dev.dubhe.anvilcraft.event.forge;

import dev.dubhe.anvilcraft.api.tooltip.forge.AnvilCraftGuiOverlay;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "forge")
public class OverlayRegisterEvent {
    public static final AnvilCraftGuiOverlay OVERLAY = new AnvilCraftGuiOverlay();

    @SubscribeEvent
    void onRegisterOverlay(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("anvilcraftoverlay", OVERLAY);
    }
}
