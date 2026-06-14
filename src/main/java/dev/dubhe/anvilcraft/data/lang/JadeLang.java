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
        provider.add("config.jade.plugin_anvilcraft.charger_provider", "Charger");
        provider.add("config.jade.plugin_anvilcraft.discharger_provider", "Discharger");

        provider.add("tooltip.anvilcraft.jade.power_information", "Power Grid: %s");
        provider.add("tooltip.anvilcraft.jade.ruby_prism.power", "Laser level: %d");
        provider.add("tooltip.anvilcraft.jade.item_detector", "Detection Range: %d");

        provider.add("tooltip.anvilcraft.charger.jade.working_progress", "Working Progress: %s");
        provider.add("tooltip.anvilcraft.charger.jade.time", "%1$s / %2$s");
        provider.add("tooltip.anvilcraft.charger.jade.energy", "%1$s / %2$s");

        provider.add("tooltip.anvilcraft.discharger.jade.working_progress", "Discharging Progress: %s");
        provider.add("tooltip.anvilcraft.discharger.jade.time", "%1$s / %2$s");
        provider.add("tooltip.anvilcraft.discharger.jade.energy", "%1$s / %2$s");

        provider.add("tooltip.anvilcraft.burning_heater.jade.state", "State: %s");
        provider.add("tooltip.anvilcraft.burning_heater.jade.state.off", "Off");
        provider.add("tooltip.anvilcraft.burning_heater.jade.state.smoldering", "Smoldering");
        provider.add("tooltip.anvilcraft.burning_heater.jade.state.lit", "Lit");
        provider.add("tooltip.anvilcraft.burning_heater.jade.burn_time", "Burn Time: %s");
        provider.add("tooltip.anvilcraft.burning_heater.jade.can_smelt", "Can Smelt: %s");
        provider.add("tooltip.anvilcraft.burning_heater.jade.can_smelt.yes", "Yes");
        provider.add("tooltip.anvilcraft.burning_heater.jade.can_smelt.no", "No");
    }
}
