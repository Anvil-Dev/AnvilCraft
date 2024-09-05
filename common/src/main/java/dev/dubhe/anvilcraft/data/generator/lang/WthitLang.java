package dev.dubhe.anvilcraft.data.generator.lang;

import dev.anvilcraft.lib.data.provider.LanguageProvider;

public class WthitLang {
    /**
     * WTHIT 配置文件本地化
     *
     * @param provider 提供器
     */
    public static void init(LanguageProvider provider) {
        provider.add("config.waila.plugin_anvilcraft", "Anvil Craft");
        provider.add("config.waila.plugin_anvilcraft.power_provider", "Anvil Craft Power");
        provider.add("config.waila.plugin_anvilcraft.warning_percent", "Warning Threshold");

        //The tooltip variable temporarily uses Jade's translation key name:
        //"tooltip.anvilcraft.jade.power_information"
    }
}
