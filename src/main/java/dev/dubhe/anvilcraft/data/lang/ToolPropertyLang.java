package dev.dubhe.anvilcraft.data.lang;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumLangProvider;

public class ToolPropertyLang {
    @SuppressWarnings("checkstyle:LineLength")
    public static void init(RegistrumLangProvider provider) {
        provider.add("tooltip.anvilcraft.property.fire_reforging", "Reforging: mending in fire or lava");
        provider.add("tooltip.anvilcraft.property.multiphase", "Multiphase: press [%1$s] to switch phases, hold [%1$s] to open phases wheel");
        provider.add("tooltip.anvilcraft.property.multiphase.id", "Multiphase Stored ID: %s");
        provider.add("tooltip.anvilcraft.property.multiphase.name.0", "α");
        provider.add("tooltip.anvilcraft.property.multiphase.name.1", "β");
        provider.add("tooltip.anvilcraft.property.multiphase.name.2", "γ");
        provider.add("tooltip.anvilcraft.property.multiphase.name.3", "δ");
        provider.add("tooltip.anvilcraft.property.multiphase.suffix.0", "-α");
        provider.add("tooltip.anvilcraft.property.multiphase.suffix.1", "-β");
        provider.add("tooltip.anvilcraft.property.multiphase.suffix.2", "-γ");
        provider.add("tooltip.anvilcraft.property.multiphase.suffix.3", "-δ");
        provider.add("tooltip.anvilcraft.property.merciless", "Merciless: disable all enchantments and convert them into attack damage and mining efficiency");
        provider.add("tooltip.anvilcraft.property.ferocious", "Ferocious: enhance attack damage and mining efficiency based on the level of all enchantments");
        provider.add("tooltip.anvilcraft.property.eternal", "Eternal: unbreakable, immune fire, explode, cactus, even the time and the void");
        provider.add("tooltip.anvilcraft.property.providence", "Providence: has chance to trigger [Hold %s] enchantments multiple times");
        provider.add("tooltip.anvilcraft.property.providence.shifting", "Providence: has chance to trigger (%s) enchantments multiple times");
        provider.add("tooltip.anvilcraft.property.stored_energy", "Remaining Energy: %s");
    }
}
