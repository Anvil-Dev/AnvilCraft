package dev.dubhe.anvilcraft.client.init;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.dubhe.anvilcraft.AnvilCraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

@EventBusSubscriber(value = Dist.CLIENT, modid = AnvilCraft.MOD_ID)
public class ModShaders {
    private static RenderPipeline scanPreviewShader;

    public static RenderPipeline getScanPreviewShader() {
        return scanPreviewShader;
    }

    @SubscribeEvent
    public static void onRegisterPipelines(RegisterRenderPipelinesEvent event) {
        scanPreviewShader = RenderPipeline.builder(net.minecraft.client.renderer.RenderPipelines.BLOCK_SNIPPET)
            .withFragmentShader(AnvilCraft.of("core/scan_preview"))
            .withLocation(AnvilCraft.of("pipeline/scan_preview"))
            .build();
        event.registerPipeline(scanPreviewShader);
    }
}
