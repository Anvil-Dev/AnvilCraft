package dev.dubhe.anvilcraft.client.fabric;

import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.util.IBlockHighlightUtil;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

public class AnvilCraftFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.CORRUPTED_BEACON.get(), RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.RESIN_BLOCK.get(), RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.AMBER_BLOCK.get(), RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.TEMPERING_GLASS.get(), RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.HEATER.get(), RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.CRAB_TRAP.get(), RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.JEWEL_CRAFTING_TABLE.get(), RenderType.cutout());
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            if (IBlockHighlightUtil.SUBCHUNKS.isEmpty()) return;
            MultiBufferSource consumers = context.consumers();
            if (consumers == null) return;
            IBlockHighlightUtil.render(context.world(), consumers, context.matrixStack(), context.camera());
        });
    }
}
