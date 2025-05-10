package dev.dubhe.anvilcraft.integration.appeng;

import appeng.api.AECapabilities;
import appeng.api.implementations.blockentities.ICraftingMachine;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.integration.Integration;
import dev.dubhe.anvilcraft.init.ModBlockEntities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

@Integration("ae2")
public class AppEngCompat {
    public void apply() {
        AnvilCraft.MOD_BUS.addListener(AppEngCompat::registerAECapabilities);
    }

    public static void registerAECapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
            AECapabilities.CRAFTING_MACHINE,
            ModBlockEntities.BATCH_CRAFTER.get(),
            (blockEntity, side) -> (ICraftingMachine) blockEntity
        );
    }
}
