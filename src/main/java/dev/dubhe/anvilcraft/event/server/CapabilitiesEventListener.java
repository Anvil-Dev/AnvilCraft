package dev.dubhe.anvilcraft.event.server;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * Registers capabilities for block entities.
 * Phase 10: basic item/fluid handler registrations. Extended registrations deferred.
 */
@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class CapabilitiesEventListener {
    @SubscribeEvent
    public static void registerCapabilities(final RegisterCapabilitiesEvent event) {
        // CFA interface capabilities are registered in ModCapabilities.java
        // along with all other block entity capability registrations.
        // Additional extended registrations (energy, custom APIs) can be added here.
    }
}
