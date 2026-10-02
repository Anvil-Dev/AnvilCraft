package dev.dubhe.anvilcraft.client.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialTravelManager;
import dev.dubhe.anvilcraft.client.support.OverworldLikeClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

/** Applies the synchronized eclipse to the overworld-like atmospheric fog. */
@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class OverworldLikeVisualEventListener {
    private OverworldLikeVisualEventListener() {
    }

    @SubscribeEvent
    public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        if (event.getCamera().getFluidInCamera() != FogType.NONE) return;
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || !CelestialTravelManager.isOverworldLike(level.dimension())) return;
        float multiplier = OverworldLikeClientState.environmentColorMultiplier(level);
        event.setRed(event.getRed() * multiplier);
        event.setGreen(event.getGreen() * multiplier);
        event.setBlue(event.getBlue() * multiplier);
    }
}
