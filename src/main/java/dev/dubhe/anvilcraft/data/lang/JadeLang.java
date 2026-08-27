package dev.dubhe.anvilcraft.data.lang;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumLangProvider;

public class JadeLang {
    public static void init(RegistrumLangProvider provider) {
        provider.add("config.jade.plugin_anvilcraft.power_provider", "Anvil Craft Power");
        provider.add("config.jade.plugin_anvilcraft.redstone_wire", "Redstone Wire");
        provider.add("config.jade.plugin_anvilcraft.ruby_prism", "Ruby Prism");
        provider.add("config.jade.plugin_anvilcraft.item_detector", "Item Detector");
        provider.add("config.jade.plugin_anvilcraft.space_overcompressor", "Space Overcompressor");
        provider.add("config.jade.plugin_anvilcraft.heatable_block_provider", "Heatable Block");
        provider.add("config.jade.plugin_anvilcraft.burning_heater_provider", "Burning Heater");
        provider.add("config.jade.plugin_anvilcraft.smart_block_placer_provider", "Smart Block Placer");
        provider.add("config.jade.plugin_anvilcraft.charger_provider", "Charging Progress");
        provider.add("config.jade.plugin_anvilcraft.discharger_provider", "Discharging Progress");
        provider.add("config.jade.plugin_anvilcraft.auto_enchanting_table_provider", "Auto Enchanting Progress");
        provider.add("config.jade.plugin_anvilcraft.wip_block", "Processing Block");
        provider.add("config.jade.plugin_anvilcraft.creative_crate", "Creative Crate");
        provider.add("config.jade.plugin_anvilcraft.creative_fluid_tank", "Creative Fluid Tank");
        provider.add("config.jade.plugin_anvilcraft.large_laser", "Large Laser");
        provider.add("config.jade.plugin_anvilcraft.load_monitor", "Load Monitor");
        provider.add("config.jade.plugin_anvilcraft.menger_sponge", "Menger Sponge");
        provider.add("config.jade.plugin_anvilcraft.crab_trap", "Crab Trap");
        provider.add("config.jade.plugin_anvilcraft.collector", "Collector");
        provider.add("config.jade.plugin_anvilcraft.control_valve", "Control Valve");
        provider.add("config.jade.plugin_anvilcraft.cursed_gold_enchant_power", "Enchant Power");
        provider.add("config.jade.plugin_anvilcraft.fluid_tank", "Fluid Tank");
        provider.add("config.jade.plugin_anvilcraft.pulse_generator", "Pulse Generator");

        provider.add("tooltip.anvilcraft.jade.power_information", "Power Grid: %s");
        provider.add("tooltip.anvilcraft.jade.ruby_prism.power", "Laser level: %d");
        provider.add("tooltip.anvilcraft.jade.item_detector", "Detection Range: %d");

        provider.add("tooltip.anvilcraft.burning_heater.jade.state", "State: %s");
        provider.add("tooltip.anvilcraft.burning_heater.jade.state.off", "Extinguished");
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

        provider.add("tooltip.anvilcraft.charger.jade.working_progress", "Charging Progress: %s");
        provider.add("tooltip.anvilcraft.charger.jade.time", "%1$s / %2$s");
        provider.add("tooltip.anvilcraft.charger.jade.energy", "%1$s / %2$s");

        provider.add("tooltip.anvilcraft.discharger.jade.working_progress", "Discharging Progress: %s");
        provider.add("tooltip.anvilcraft.auto_enchanting_table.jade.working_progress", "Enchanting Progress: %s");
        provider.add("tooltip.anvilcraft.discharger.jade.time", "%1$s / %2$s");
        provider.add("tooltip.anvilcraft.discharger.jade.energy", "%1$s / %2$s");
        provider.add("tooltip.anvilcraft.pulse_generator.jade.mode", "Mode: %s");
        provider.add("tooltip.anvilcraft.pulse_generator.jade.mode.rising", "Rising Edge");
        provider.add("tooltip.anvilcraft.pulse_generator.jade.mode.falling", "Falling Edge");
        provider.add("tooltip.anvilcraft.pulse_generator.jade.mode.loop", "Loop");
        provider.add("tooltip.anvilcraft.pulse_generator.jade.reverse", "Reverse: %s");
        provider.add("tooltip.anvilcraft.pulse_generator.jade.reverse.on", "On");
        provider.add("tooltip.anvilcraft.pulse_generator.jade.reverse.off", "Off");
        provider.add("tooltip.anvilcraft.pulse_generator.jade.working_progress", "Pulse Progress: %s");
        provider.add("tooltip.anvilcraft.wip_block.jade.recipe", "Procedural Recipe: %s");
        provider.add("tooltip.anvilcraft.wip_block.jade.step_count", "Steps Executed: %d");

        provider.add("tooltip.anvilcraft.infinity", "Infinity");

        provider.add("tooltip.anvilcraft.crab_trap.jade.fishing", "Fishing Attempts: %s");
        provider.add("tooltip.anvilcraft.control_valve.jade.filter", "Filter: %s");
        provider.add("tooltip.anvilcraft.control_valve.jade.rate", "Max Rate: %s");
        provider.add("tooltip.anvilcraft.collector.jade.range", "Collect Range: %s");
        provider.add("tooltip.anvilcraft.collector.jade.cooldown", "Collect Cooldown: %s");
        provider.add("tooltip.anvilcraft.load_monitor.jade.load", "Load: %s");
    }
}
