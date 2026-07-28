package dev.dubhe.anvilcraft.data.lang;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumLangProvider;

public class KeyMappingLang {
    public static void init(RegistrumLangProvider provider) {
        provider.add("key.categories.anvilcraft", "AnvilCraft");
        provider.add("key.anvilcraft.switch_phase", "Switch Phase");
        provider.add("key.anvilcraft.toggle_goggle", "Toggle Goggle Mode");
        provider.add("key.anvilcraft.switch_tool_mode", "Switch Tool Mode");
        provider.add("key.anvilcraft.use_pill_box", "Use Pill Box");
        provider.add("key.anvilcraft.thought", "Thought");
    }
}
