package dev.dubhe.anvilcraft.data.generator.lang;

import com.tterrag.registrate.providers.RegistrateLangProvider;

public class WthitLang {
    public static void init(RegistrateLangProvider provider) {
        provider.add("config.waila.plugin_anvilcraft", "Anvil Craft");
        provider.add("config.waila.plugin_anvilcraft.power_provider", "Anvil Craft Power");
        provider.add("config.waila.plugin_anvilcraft.warning_percent", "Warning Threshold");

        //The tooltip variable temporarily uses Jade's translation key name:
        //"tooltip.anvilcraft.jade.power_information"
    }
}
