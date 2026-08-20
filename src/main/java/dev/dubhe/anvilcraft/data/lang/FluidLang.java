package dev.dubhe.anvilcraft.data.lang;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumLangProvider;

public class FluidLang {
    @SuppressWarnings("checkstyle:LineLength")
    public static void init(RegistrumLangProvider provider) {
        provider.add("tooltip.anvilcraft.fluid.flowing", "%s (Flowing)");
        provider.add("tooltip.anvilcraft.fluid.amount", "%s / %s");

        // Non-placeable fluids
        provider.add("block.anvilcraft.milk", "Milk");
        provider.add("block.anvilcraft.honey", "Honey");
        provider.add("block.anvilcraft.primordial_matter", "Primordial Matter");
        provider.add("block.anvilcraft.liquid_enchantment", "Liquid Enchantment");
        provider.add("block.anvilcraft.liquid_enchantment.enchanted", "Liquid Enchantment (%s)");

        // Gases
        provider.add("block.anvilcraft.hydrogen", "Hydrogen");
        provider.add("block.anvilcraft.oxygen", "Oxygen");
        provider.add("block.anvilcraft.helium", "Helium");
        provider.add("block.anvilcraft.deuterium", "Deuterium");
    }
}
