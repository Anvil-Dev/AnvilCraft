package dev.dubhe.anvilcraft.data.generator.lang;

import com.tterrag.registrate.providers.RegistrateLangProvider;

public class LangHandler {
    public static void init(RegistrateLangProvider provider) {
        AdvancementLang.init(provider);
        ConfigScreenLang.init(provider);
        ScreenLang.init(provider);

        provider.add("item.anvilcraft.amethyst_pickaxe.tooltip", "Stone pickaxe quality, can mine iron ore, not diamonds!");
    }
}
