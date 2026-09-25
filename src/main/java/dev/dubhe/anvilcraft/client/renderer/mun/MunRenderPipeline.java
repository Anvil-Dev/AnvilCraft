package dev.dubhe.anvilcraft.client.renderer.mun;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.config.AnvilCraftClientConfig.MunLightingQuality;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.config.ModConfigs;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Common persistent OFF fallback for Moon rendering resources. */
final class MunRenderPipeline {
    private MunRenderPipeline() {
    }

    static void fail(RuntimeException exception) {
        AnvilCraft.CLIENT_CONFIG.munLightingQuality = MunLightingQuality.OFF;
        AnvilCraft.LOGGER.warn("Mun rendering unavailable; using native fallback", exception);
        try {
            for (var config : ModConfigs.getModConfigs(AnvilCraft.MOD_ID)) {
                if (config.getType() != ModConfig.Type.CLIENT || !(config.getSpec() instanceof ModConfigSpec spec)) continue;
                ModConfigSpec.EnumValue<MunLightingQuality> value = spec.getValues().get("mun_lighting_quality");
                if (value == null || config.getLoadedConfig() == null) continue;
                value.set(MunLightingQuality.OFF);
                spec.save();
            }
        } catch (RuntimeException ignored) {
            // Keep OFF active even if a configuration file cannot be saved.
        }
    }
}
