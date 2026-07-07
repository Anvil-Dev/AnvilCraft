package dev.dubhe.anvilcraft.client.init;

import dev.anvilcraft.lib.v2.rendering.event.MainTargetResizeEvent;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.renderer.post.GravitationalLensPostEffect;
import lombok.Getter;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.jspecify.annotations.Nullable;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public class ModPostEffects {
    @Nullable
    private static GravitationalLensPostEffect gravitationalLensPostEffect = null;

    public static void createPostEffects() {
        gravitationalLensPostEffect = new GravitationalLensPostEffect();
    }

    public static @Nullable GravitationalLensPostEffect getGravitationalLensPostEffect() {
        if (gravitationalLensPostEffect == null) {
            createPostEffects();
        }
        return gravitationalLensPostEffect;
    }

    @SubscribeEvent
    public static void on(MainTargetResizeEvent event) {
        if (gravitationalLensPostEffect == null) {
            createPostEffects();
        }
        gravitationalLensPostEffect.resize(event.getNewWidth(), event.getNewHeight());
    }
}
