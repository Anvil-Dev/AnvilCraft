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
        provider.add("config.jade.plugin_anvilcraft.smart_block_placer_provider", "Smart Block Placer");

        provider.add("tooltip.anvilcraft.jade.power_information", "Power Grid: %s");
        provider.add("tooltip.anvilcraft.jade.ruby_prism.power", "Laser level: %d");
        provider.add("tooltip.anvilcraft.jade.item_detector", "Detection Range: %d");

        provider.add("tooltip.anvilcraft.burning_heater.jade.state", "State: %s");
        provider.add("tooltip.anvilcraft.burning_heater.jade.state.off", "Off");
        provider.add("tooltip.anvilcraft.burning_heater.jade.state.smoldering", "Smoldering");
        provider.add("tooltip.anvilcraft.burning_heater.jade.state.lit", "Lit");

        provider.add("tooltip.anvilcraft.burning_heater.jade.can_smelt", "Can Smelt: %s");
        provider.add("tooltip.anvilcraft.burning_heater.jade.can_smelt.yes", "Yes");
        provider.add("tooltip.anvilcraft.burning_heater.jade.can_smelt.no", "No");

        provider.add("tooltip.anvilcraft.smart_block_placer.jade.operation_mode", "Operation Mode: %s");
        provider.add("tooltip.anvilcraft.smart_block_placer.jade.mode.normal", "Point");
        provider.add("tooltip.anvilcraft.smart_block_placer.jade.mode.blueprint", "Blueprint");
        provider.add("tooltip.anvilcraft.smart_block_placer.jade.placement_mode", "Placement Mode: %s");
        provider.add("tooltip.anvilcraft.smart_block_placer.jade.placement.pickup", "Pickup");
        provider.add("tooltip.anvilcraft.smart_block_placer.jade.placement.move", "Move");
        provider.add("tooltip.anvilcraft.smart_block_placer.jade.blueprint_name", "Blueprint: %s");
        provider.add("tooltip.anvilcraft.smart_block_placer.jade.missing_mode", "Missing Mode: %s");
        provider.add("tooltip.anvilcraft.smart_block_placer.jade.missing.skip", "Skip");
        provider.add("tooltip.anvilcraft.smart_block_placer.jade.missing.stop", "Stop");

        provider.add("tooltip.anvilcraft.wip_block.jade.recipe", "Procedural Recipe: %s");
        provider.add("tooltip.anvilcraft.wip_block.jade.step_count", "Steps Executed: %d");
    }
}
