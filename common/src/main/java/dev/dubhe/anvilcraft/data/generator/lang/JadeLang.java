package dev.dubhe.anvilcraft.data.generator.lang;

import com.tterrag.registrate.providers.RegistrateLangProvider;

public class JadeLang {
    /**
     * @param provider 提供器
     */
    public static void init(RegistrateLangProvider provider) {
        provider.add("config.jade.plugin_anvilcraft.power_provider", "Anvil Craft Power");
        provider.add("tooltip.anvilcraft.jade.power_information", "Power Grid: %d/%d MW");
    }
}
