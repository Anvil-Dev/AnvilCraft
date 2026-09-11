package dev.dubhe.anvilcraft.client.renderer.mun;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.config.AnvilCraftClientConfig.MunLightingQuality;
import net.minecraft.client.Minecraft;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.config.ModConfigs;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.io.IOException;

public final class MunRenderPipeline {
    private static final MunRenderState STATE = new MunRenderState();
    private static boolean reloadRequested;
    private static boolean reloading;
    private static boolean refreshRequested;
    private static boolean saveRequested;

    private MunRenderPipeline() {
    }

    public static boolean requested() {
        return AnvilCraft.CLIENT_CONFIG.munLightingQuality != MunLightingQuality.OFF && STATE.requested();
    }

    public static boolean enabled() {
        return requested() && STATE.enabled();
    }

    public static void registerShaders(RegisterShadersEvent event) {
        STATE.beginReload(AnvilCraft.CLIENT_CONFIG.munLightingQuality);
        release(MunSurfaceRenderer::resetShaders);
        MunSkyRenderer.resetShader();
        if (!requested()) return;
        try (MunShaderRegistration shaders = new MunShaderRegistration(event.getResourceProvider())) {
            MunSkyRenderer.registerShaders(shaders);
            MunSurfaceRenderer.registerShaders(shaders);
            shaders.register(event, STATE::completeReload);
        } catch (IOException | RuntimeException exception) {
            fail();
        }
    }

    public static void fail() {
        if (!STATE.fail()) return;
        AnvilCraft.CLIENT_CONFIG.munLightingQuality = MunLightingQuality.OFF;
        reloadRequested = true;
        refreshRequested = true;
        saveRequested = true;
    }

    public static void tick() {
        if (STATE.configure(AnvilCraft.CLIENT_CONFIG.munLightingQuality)) {
            reloadRequested = true;
            refreshRequested = true;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (saveRequested) {
            saveRequested = false;
            release(MunRenderPipeline::saveOff);
        }
        if (reloading || minecraft.getOverlay() != null) return;
        if (refreshRequested) {
            refreshRequested = false;
            release(MunSurfaceRenderer::clear);
            if (minecraft.level != null) {
                minecraft.levelRenderer.allChanged();
                minecraft.gameRenderer.lightTexture().tick();
            }
        }
        if (!reloadRequested) return;
        reloadRequested = false;
        reloading = true;
        minecraft.reloadResourcePacks().whenComplete((unused, failure) -> {
            reloading = false;
            refreshRequested = true;
        });
    }

    private static void saveOff() {
        for (ModConfig config : ModConfigs.getModConfigs(AnvilCraft.MOD_ID)) {
            if (config.getType() != ModConfig.Type.CLIENT || !(config.getSpec() instanceof ModConfigSpec spec)) continue;
            ModConfigSpec.EnumValue<MunLightingQuality> value = spec.getValues().get("mun_lighting_quality");
            if (value == null || config.getLoadedConfig() == null) continue;
            value.set(MunLightingQuality.OFF);
            spec.save();
        }
    }

    static void release(Runnable cleanup) {
        try {
            cleanup.run();
        } catch (RuntimeException exception) {
            // Keep OFF latched even if cleanup or saving fails.
        }
    }
}
