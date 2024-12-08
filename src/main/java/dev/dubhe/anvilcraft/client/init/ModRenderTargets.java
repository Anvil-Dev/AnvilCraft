package dev.dubhe.anvilcraft.client.init;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import dev.dubhe.anvilcraft.client.renderer.RenderState;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderStateShard;

import static dev.dubhe.anvilcraft.client.init.ModShaders.MINECRAFT;

public class ModRenderTargets {
    @Getter
    static RenderTarget bloomTarget;
    @Getter
    static RenderTarget tempTarget;

    public static final RenderStateShard.OutputStateShard LASER_TARGET = new RenderStateShard.OutputStateShard(
        "anvilcraft:laser",
        () -> {
            if (RenderState.isEnhancedRenderingAvailable() && RenderState.isBloomRenderStage()) {
                bloomTarget.bindWrite(false);
            } else {
                MINECRAFT.getMainRenderTarget().bindWrite(false);
            }
        },
        () -> {
            if (RenderState.isEnhancedRenderingAvailable() && RenderState.isBloomRenderStage()) {
                bloomTarget.unbindWrite();
            }
            MINECRAFT.getMainRenderTarget().bindWrite(false);
        }
    );

    public static final RenderStateShard.OutputStateShard LINE_BLOOM_TARGET = new RenderStateShard.OutputStateShard(
        "anvilcraft:line_bloom",
        () -> {
            if (RenderState.isEnhancedRenderingAvailable()) {
                bloomTarget.bindWrite(false);
            } else {
                MINECRAFT.getMainRenderTarget().bindWrite(false);
            }
        },
        () -> {
            if (RenderState.isEnhancedRenderingAvailable()) {
                bloomTarget.unbindWrite();
            }
            MINECRAFT.getMainRenderTarget().bindWrite(false);
        }
    );

    public static void clear() {
        bloomTarget.clear(Minecraft.ON_OSX);
        tempTarget.clear(Minecraft.ON_OSX);
    }

    public static void renderTargetLoaded(
        RenderTarget laserTarget
    ){
        ModRenderTargets.bloomTarget = laserTarget;
        ModRenderTargets.tempTarget = new TextureTarget(
            laserTarget.width,
            laserTarget.height,
            true,
            Minecraft.ON_OSX
        );
    }

}
