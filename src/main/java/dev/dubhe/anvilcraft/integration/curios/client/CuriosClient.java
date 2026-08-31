package dev.dubhe.anvilcraft.integration.curios.client;

import dev.anvilcraft.lib.v2.integration.Integration;
import dev.anvilcraft.lib.v2.integration.IntegrationType;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.integration.curios.client.renderer.GogglesCurioRenderer;
import dev.dubhe.anvilcraft.integration.curios.client.renderer.IonocraftBackpackCurioRenderer;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;

@Integration(value = "curios", type = IntegrationType.CLIENT)
public class CuriosClient {
    public void applyClient() {
        CuriosRendererRegistry.register(ModItems.ANVIL_HAMMER.get(), GogglesCurioRenderer::new);
        CuriosRendererRegistry.register(ModItems.ROYAL_ANVIL_HAMMER.get(), GogglesCurioRenderer::new);
        CuriosRendererRegistry.register(ModItems.FROST_ANVIL_HAMMER.get(), GogglesCurioRenderer::new);
        CuriosRendererRegistry.register(ModItems.EMBER_ANVIL_HAMMER.get(), GogglesCurioRenderer::new);
        CuriosRendererRegistry.register(ModItems.TRANSCENDENCE_ANVIL_HAMMER.get(), GogglesCurioRenderer::new);
        CuriosRendererRegistry.register(ModItems.IONOCRAFT_BACKPACK.get(), IonocraftBackpackCurioRenderer::new);
    }
}