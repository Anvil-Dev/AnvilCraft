package dev.dubhe.anvilcraft.data.lang;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumLangProvider;

public class FluidLang {
    public static void init(RegistrumLangProvider provider) {
        provider.add("fluid.anvilcraft.fire", "Burning Oil (The content of Fire Cauldron)");

        // Non-placeable fluids
        provider.add("block.anvilcraft.milk", "Milk");
        provider.add("block.anvilcraft.honey", "Honey");
        provider.add("block.anvilcraft.primordial_matter", "Primordial Matter");
        provider.add("block.anvilcraft.liquid_enchantment", "Liquid Enchantment");
        provider.add("block.anvilcraft.liquid_enchantment.enchanted", "Liquid Enchantment (%s)");
    }
}
