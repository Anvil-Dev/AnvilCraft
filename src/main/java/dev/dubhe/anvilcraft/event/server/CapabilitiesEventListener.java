package dev.dubhe.anvilcraft.event.server;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * Registers capabilities for block entities.
 * Phase 10: basic item/fluid handler registrations. Extended registrations deferred.
 */
@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class CapabilitiesEventListener {
    @SubscribeEvent
    public static void registerCapabilities(final RegisterCapabilitiesEvent event) {
        // CFA interfaces will be registered once their blocks/items are ported (Phase 11)
        // Other block entity capabilities are registered via their respective Registrum entries
    }
}
