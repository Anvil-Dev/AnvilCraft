package dev.dubhe.anvilcraft.mixin.accessor;

import com.mojang.blaze3d.systems.RenderSystem;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RenderSystem.class)
public interface RenderSystemAccessor {
    @Accessor("shaderLightDirections")
    static Vector3f[] anvilcraft$getShaderLightDirections() {
        throw new AssertionError();
    }
}
