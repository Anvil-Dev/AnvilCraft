package dev.dubhe.anvilcraft.integration.appeng;

import appeng.api.AECapabilities;
import appeng.api.implementations.blockentities.ICraftingMachine;
import com.tterrag.registrate.providers.ProviderType;
import dev.anvilcraft.lib.integration.Integration;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.integration.appeng.data.AppEngBlockTagLoader;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

@Integration("ae2")
public class AppEngCompat {
    public void apply() {
        AnvilCraft.MOD_BUS.addListener(AppEngCompat::registerAECapabilities);
        AnvilCraft.REGISTRATE.addDataGenerator(ProviderType.BLOCK_TAGS, AppEngBlockTagLoader::init);
    }

    public static void registerAECapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
            AECapabilities.CRAFTING_MACHINE,
            ModBlockEntities.BATCH_CRAFTER.get(),
            (blockEntity, side) -> (ICraftingMachine) blockEntity
        );
    }
}
