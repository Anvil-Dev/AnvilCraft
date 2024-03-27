package dev.dubhe.anvilcraft.client;

import dev.dubhe.anvilcraft.api.network.Networking;
import dev.dubhe.anvilcraft.client.gui.screen.inventory.AutoCrafterScreen;
import dev.dubhe.anvilcraft.client.init.ModNetworks;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.RenderType;

public class AnvilCraftClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.HOLLOW_MAGNET_BLOCK, RenderType.cutout());
        MenuScreens.register(ModMenuTypes.AUTO_CRAFTER, AutoCrafterScreen::new);
        ModNetworks.register();
        Networking.CLIENT.register();
    }
}
