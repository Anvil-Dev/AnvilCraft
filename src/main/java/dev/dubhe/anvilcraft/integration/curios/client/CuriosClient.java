package dev.dubhe.anvilcraft.integration.curios.client;

import dev.anvilcraft.lib.v2.integration.Integration;
import dev.anvilcraft.lib.v2.integration.IntegrationHook;
import dev.anvilcraft.lib.v2.integration.IntegrationType;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.integration.curios.client.renderer.GogglesCurioRenderer;
import dev.dubhe.anvilcraft.integration.curios.client.renderer.IonocraftBackpackCurioRenderer;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.jetbrains.annotations.NotNull;
import top.theillusivec4.curios.api.client.ICurioRenderer;

@Integration(value = "curios", type = IntegrationType.CLIENT)
public class CuriosClient {
    public void applyClient() {
        IEventBus modEventBus = IntegrationHook.getModEventBus();
        modEventBus.addListener(this::onClientSetup);
        modEventBus.addListener(this::onLayerRegister);
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        ICurioRenderer.register(ModItems.ANVIL_HAMMER.get(), GogglesCurioRenderer::new);
        ICurioRenderer.register(ModItems.ROYAL_ANVIL_HAMMER.get(), GogglesCurioRenderer::new);
        ICurioRenderer.register(ModItems.FROST_ANVIL_HAMMER.get(), GogglesCurioRenderer::new);
        ICurioRenderer.register(ModItems.EMBER_ANVIL_HAMMER.get(), GogglesCurioRenderer::new);
        ICurioRenderer.register(ModItems.TRANSCENDENCE_ANVIL_HAMMER.get(), GogglesCurioRenderer::new);
        ICurioRenderer.register(ModItems.IONOCRAFT_BACKPACK.get(), IonocraftBackpackCurioRenderer::new);
    }

    private void onLayerRegister(final EntityRenderersEvent.@NotNull RegisterLayerDefinitions event) {
        event.registerLayerDefinition(
            GogglesCurioRenderer.LAYER,
            () -> LayerDefinition.create(GogglesCurioRenderer.mesh(), 1, 1)
        );
    }
}
