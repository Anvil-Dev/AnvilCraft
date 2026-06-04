package dev.dubhe.anvilcraft.data.lang;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumLangProvider;

public class JadeLang {
    public static void init(RegistrumLangProvider provider) {
        provider.add("config.jade.plugin_anvilcraft.power_provider", "Anvil Craft Power");
        provider.add("config.jade.plugin_anvilcraft.ruby_prism", "Ruby Prism");
        provider.add("config.jade.plugin_anvilcraft.item_detector", "Item Detector");
        provider.add("config.jade.plugin_anvilcraft.space_overcompressor", "Space Overcompressor");
        provider.add("config.jade.plugin_anvilcraft.heatable_block_provider", "Heatable Block");
        provider.add("config.jade.plugin_anvilcraft.burning_heater_provider", "Burning Heater");

        provider.add("tooltip.anvilcraft.jade.power_information", "Power Grid: %s");
        provider.add("tooltip.anvilcraft.jade.ruby_prism.power", "Laser level: %d");
        provider.add("tooltip.anvilcraft.jade.item_detector", "Detection Range: %d");

        provider.add("tooltip.anvilcraft.burning_heater.jade.can_smelt", "Can Smelt: %s");
        provider.add("tooltip.anvilcraft.burning_heater.jade.can_smelt.yes", "Yes");
        provider.add("tooltip.anvilcraft.burning_heater.jade.can_smelt.no", "No");
    }
}
