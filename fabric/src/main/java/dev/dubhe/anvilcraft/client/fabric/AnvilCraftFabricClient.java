package dev.dubhe.anvilcraft.client.fabric;

import dev.dubhe.anvilcraft.event.TooltipEventListener;
import dev.dubhe.anvilcraft.event.fabric.CommonEventHandlerListener;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.util.IBlockHighlightUtil;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

public class AnvilCraftFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.CORRUPTED_BEACON.get(), RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.RESIN_BLOCK.get(), RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.AMBER_BLOCK.get(), RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.MOB_AMBER_BLOCK.get(), RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.RESENTFUL_AMBER_BLOCK.get(), RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.TEMPERING_GLASS.get(), RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.HEATER.get(), RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.CRAB_TRAP.get(), RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.JEWEL_CRAFTING_TABLE.get(), RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.PIEZOELECTRIC_CRYSTAL.get(), RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.BLOCK_PLACER.get(), RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.BLOCK_DEVOURER.get(), RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.RUBY_PRISM.get(), RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.RUBY_LASER.get(), RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.GIANT_ANVIL.get(), RenderType.solid());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.MENGER_SPONGE.get(), RenderType.cutout());
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            if (IBlockHighlightUtil.SUBCHUNKS.isEmpty()) return;
            MultiBufferSource consumers = context.consumers();
            if (consumers == null) return;
            IBlockHighlightUtil.render(context.world(), consumers, context.matrixStack(), context.camera());
        });

        ItemTooltipCallback.EVENT.register((stack, context, lines) -> TooltipEventListener.addTooltip(stack, lines));
        CommonEventHandlerListener.clientInit();
    }
}
