package dev.dubhe.anvilcraft.client.init;

import dev.anvilcraft.lib.v2.rendering.event.MainTargetResizeEvent;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.renderer.post.GravitationalLensPostEffect;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.jspecify.annotations.Nullable;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public class ModPostEffects {
    @Nullable
    private static GravitationalLensPostEffect gravitationalLensPostEffect = null;

    public static void createPostEffects() {
        ModPostEffects.gravitationalLensPostEffect = new GravitationalLensPostEffect();
    }

    public static @Nullable GravitationalLensPostEffect getGravitationalLensPostEffect() {
        if (ModPostEffects.gravitationalLensPostEffect == null) {
            ModPostEffects.createPostEffects();
        }
        return ModPostEffects.gravitationalLensPostEffect;
    }

    @SubscribeEvent
    public static void on(MainTargetResizeEvent event) {
        if (ModPostEffects.gravitationalLensPostEffect == null) {
            ModPostEffects.createPostEffects();
        }
        ModPostEffects.gravitationalLensPostEffect.resize(event.getNewWidth(), event.getNewHeight());
    }
}
