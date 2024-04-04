package dev.dubhe.anvilcraft.data.generator.lang;

import com.tterrag.registrate.providers.RegistrateLangProvider;

public class LangHandler {
    public static void init(RegistrateLangProvider provider) {
        AdvancementLang.init(provider);
        ConfigurationLang.init(provider);
        ScreenLang.init(provider);

        provider.add("command.anvilcraft.config.check.success", "The config file reads as follows:");
        provider.add("command.anvilcraft.config.reload.success", "Config reloaded!");
        provider.add("item.anvilcraft.amethyst_pickaxe.tooltip", "Stone pickaxe quality, can mine iron ore, not diamonds!");
    }
}
