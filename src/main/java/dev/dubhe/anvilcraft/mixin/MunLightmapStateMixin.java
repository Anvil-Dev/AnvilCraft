package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.client.renderer.mun.MunLightmapState;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LightmapRenderState.class)
public class MunLightmapStateMixin implements MunLightmapState {
    @Unique
    private boolean anvilcraft$munLighting;

    @Override
    public boolean anvilcraft$isMunLighting() {
        return this.anvilcraft$munLighting;
    }

    @Override
    public void anvilcraft$setMunLighting(boolean enabled) {
        this.anvilcraft$munLighting = enabled;
    }
}
