package dev.dubhe.anvilcraft.client.fabric;

import dev.dubhe.anvilcraft.init.ModBlocks;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.renderer.RenderType;

public class AnvilCraftClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.HOLLOW_MAGNET_BLOCK.get(), RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.CHUTE.get(), RenderType.cutout());
    }
}
