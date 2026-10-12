package dev.dubhe.anvilcraft.mixin;

import com.mojang.blaze3d.opengl.GlRenderPass;
import com.mojang.blaze3d.opengl.GlRenderPipeline;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GlRenderPass.class)
public interface MunGlRenderPassAccessor {
    @Nullable
    @Accessor("pipeline")
    GlRenderPipeline anvilcraft$moonPipeline();
}
