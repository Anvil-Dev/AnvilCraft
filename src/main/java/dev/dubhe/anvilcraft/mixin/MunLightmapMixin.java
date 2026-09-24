package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.dubhe.anvilcraft.client.renderer.mun.MunLightmapState;
import dev.dubhe.anvilcraft.client.renderer.mun.MunSurfaceRenderer;
import net.minecraft.client.renderer.Lightmap;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Lightmap.class)
public class MunLightmapMixin {
    @WrapMethod(method = "render")
    private void anvilcraft$moonLightmap(LightmapRenderState state, Operation<Void> original) {
        MunSurfaceRenderer.lightmap(((MunLightmapState) state).anvilcraft$isMunLighting());
        try {
            original.call(state);
        } finally {
            MunSurfaceRenderer.lightmap(false);
        }
    }
}
