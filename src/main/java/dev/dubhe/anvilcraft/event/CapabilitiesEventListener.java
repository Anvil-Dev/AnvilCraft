package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.itemhandler.HoneyCauldronWrapper;
import dev.dubhe.anvilcraft.init.ModBlockEntities;
import dev.dubhe.anvilcraft.init.ModBlocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import java.util.List;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class CapabilitiesEventListener {
    @SubscribeEvent
    public static void registerCapabilities(final RegisterCapabilitiesEvent event) {
        List.of(ModBlockEntities.BATCH_CRAFTER.get(),
            ModBlockEntities.CHARGER.get(),
            ModBlockEntities.CHUTE.get(),
            ModBlockEntities.SIMPLE_CHUTE.get(),
            ModBlockEntities.ITEM_COLLECTOR.get(),
            ModBlockEntities.MAGNETIC_CHUTE.get(),
            ModBlockEntities.CONFINEMENT_CHAMBER.get()
        ).forEach(type -> event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            type,
            (be, side) -> be.getItemHandler())
        );

        event.registerBlock(
            Capabilities.ItemHandler.BLOCK,
            ((level, pos, state, blockEntity, side) -> new HoneyCauldronWrapper(level, pos)),
            ModBlocks.HONEY_CAULDRON.get()
        );
    }
}
