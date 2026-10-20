package dev.dubhe.anvilcraft.data.lang;

import dev.anvilcraft.lib.v2.config.ConfigData;
import dev.anvilcraft.lib.v2.registrum.providers.RegistrumLangProvider;
import dev.dubhe.anvilcraft.config.AnvilCraftClientConfig;
import dev.dubhe.anvilcraft.config.AnvilCraftServerConfig;

public class ConfigScreenLang {
    /**
     * 初始化配置语言
     *
     * @param provider 提供器
     */
    public static void init(RegistrumLangProvider provider) {
        ConfigData.readConfigClass(provider, AnvilCraftServerConfig.class);
        ConfigData.readConfigClass(provider, AnvilCraftClientConfig.class);
    }
}
