package dev.dubhe.anvilcraft.data.lang;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumLangProvider;

public class StatLang {
    public static void init(RegistrumLangProvider provider) {
        provider.add("stat.anvilcraft.enter_power_grid", "Power Grid Entries");
        provider.add("stat.anvilcraft.place_power_component", "Power Components Placed");
    }
}
