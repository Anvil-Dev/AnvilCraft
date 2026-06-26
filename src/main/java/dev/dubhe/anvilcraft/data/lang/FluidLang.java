package dev.dubhe.anvilcraft.data.lang;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumLangProvider;

public class FluidLang {
    @SuppressWarnings("checkstyle:LineLength")
    public static void init(RegistrumLangProvider provider) {
        provider.add("fluid.anvilcraft.fire", "Burning Oil (The content of Fire Cauldron)");

        // Non-placeable fluids
        provider.add("block.anvilcraft.milk", "Milk");
        provider.add("block.anvilcraft.honey", "Honey");
        provider.add("block.anvilcraft.primordial_matter", "Primordial Matter");
    }
}
