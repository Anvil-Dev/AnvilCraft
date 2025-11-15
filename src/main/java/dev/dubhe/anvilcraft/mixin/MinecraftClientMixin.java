package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.api.rendering.pipeline.cached.CacheableBERenderingPipeline;
import net.minecraft.client.Minecraft;
import net.minecraft.client.main.GameConfig;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL43.GL_DEBUG_OUTPUT_SYNCHRONOUS;

@Mixin(Minecraft.class)
abstract class MinecraftClientMixin {
    @Inject(
        method = "<init>",
        at = @At("TAIL")
    )
    void applyWorkaround(GameConfig gameConfig, CallbackInfo ci) {
        glEnable(GL_DEBUG_OUTPUT_SYNCHRONOUS);
    }

    @Inject(
        method = "updateLevelInEngines",
        at = @At("HEAD")
    )
    void updateLevel(ClientLevel level, CallbackInfo ci) {
        CacheableBERenderingPipeline.updateLevel(level);
    }
}
