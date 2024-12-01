package dev.dubhe.anvilcraft.data.lang;

import com.tterrag.registrate.providers.RegistrateLangProvider;

public class ScreenLang {
    /**
     * 初始化 GUI 文本生成器
     *
     * @param provider 提供器
     */
    public static void init(RegistrateLangProvider provider) {
        provider.add("screen.anvilcraft.button.direction", "Output Direction: %s");
        provider.add("screen.anvilcraft.button.direction.down", "Down");
        provider.add("screen.anvilcraft.button.direction.east", "East");
        provider.add("screen.anvilcraft.button.direction.north", "North");
        provider.add("screen.anvilcraft.button.direction.south", "South");
        provider.add("screen.anvilcraft.button.direction.up", "Up");
        provider.add("screen.anvilcraft.button.direction.west", "West");
        provider.add("screen.anvilcraft.button.off", "off");
        provider.add("screen.anvilcraft.button.on", "on");
        provider.add("screen.anvilcraft.button.record", "Retention item filtering: %s");
        provider.add("screen.anvilcraft.slot.disable.tooltip", "Use item clicks to set filter");
        provider.add("screen.anvilcraft.royal_grindstone.remove_curse_count", "Removed %i curse");
        provider.add("screen.anvilcraft.royal_grindstone.remove_repair_cost", "Removed %i repair cost");
        provider.add("screen.anvilcraft.royal_grindstone.title", "Remove curse and repair cost");
        provider.add(
                "screen.anvilcraft.royal_steel_upgrade_smithing_template", "Royal Steel Upgrade Smithing Template");
        provider.add(
                "screen.anvilcraft.smithing_template.royal_steel_upgrade_smithing_template"
                        + ".additions_slot_description",
                "Put the Royal Steel Ingot or Royal Steel Block");
        provider.add(
                "screen.anvilcraft.smithing_template.royal_steel_upgrade_smithing_template" + ".applies_to",
                "Anvil Hammer, Amethyst Pickaxe, Golden Pickaxe, Iron Pickaxe, Diamond Pickaxe");
        provider.add(
                "screen.anvilcraft.smithing_template.royal_steel_upgrade_smithing_template" + ".base_slot_description",
                "Put ");

        provider.add("screen.anvilcraft.item_collector.title", "Item Collector");
        provider.add("screen.anvilcraft.item_collector.range", "Range");
        provider.add("screen.anvilcraft.item_collector.cooldown", "Cooldown");
        provider.add("screen.anvilcraft.item_collector.input_power", "Input Power");

        provider.add("tooltip.anvilcraft.grid_information.title", "Power Grid Stats:");
        provider.add("tooltip.anvilcraft.grid_information.producer_stats", "Power Producer Stats:");
        provider.add("tooltip.anvilcraft.grid_information.consumer_stats", "Power Consumer Stats:");
        provider.add("tooltip.anvilcraft.grid_information.output_power", "  Power Generation: %d");
        provider.add("tooltip.anvilcraft.grid_information.input_power", "  Power Consumption: %d");
        provider.add("tooltip.anvilcraft.grid_information.total_consumed", "  Total Consumption: %d");
        provider.add("tooltip.anvilcraft.grid_information.total_generated", "  Total Generation: %d");
        provider.add("tooltip.anvilcraft.grid_information.utilization", "  Power Utilization: %s");
        provider.add("tooltip.anvilcraft.grid_information.overloaded1", "It appears that this grid is overloaded.");
        provider.add("tooltip.anvilcraft.grid_information.overloaded2", "Add more sources or remove the components");
        provider.add("tooltip.anvilcraft.grid_information.overloaded3", "with a high stress impact.");

        provider.add("screen.anvilcraft.active_silencer.title", "Active Silencer");

        provider.add("block.anvilcraft.heliostats.invalid_placement", "Invalid placement");
        provider.add("block.anvilcraft.heliostats.placement_no_pos", "Irradiation position not set");
        provider.add("item.anvilcraft.heliostats.pos_set", "Will irradiate %s");
        provider.add("item.anvilcraft.heliostats.no_rotation_angle", "Will irradiate %s");
        provider.add("tooltip.anvilcraft.heliostats.not_work", "Heliostats are not working");
        provider.add("tooltip.anvilcraft.heliostats.no_sun", "  No sunlight");
        provider.add("tooltip.anvilcraft.heliostats.obscured", "  The illumination path is obscured");
        provider.add("tooltip.anvilcraft.heliostats.too_far", "  The illumination pos is too far");
        provider.add("tooltip.anvilcraft.heliostats.unspecified_irradiation_block", "  Unspecified irradiation block");
        provider.add("tooltip.anvilcraft.heliostats.unknown", "  Unknown reason");

        provider.add(
                "screen.anvilcraft.ember_metal_upgrade_smithing_template", "Ember Metal Upgrade Smithing Template");
        provider.add(
                "screen.anvilcraft.smithing_template.ember_metal_upgrade_smithing_template"
                        + ".additions_slot_description",
                "Put the Ember Metal Ingot or Ember Metal Block");
        provider.add(
                "screen.anvilcraft.smithing_template.ember_metal_upgrade_smithing_template" + ".applies_to",
                "Royal Steel Anvil Hammer, Netherite Tools, Royal Steel Tools, Royal Steel WorkStations");
        provider.add(
                "screen.anvilcraft.smithing_template.ember_metal_upgrade_smithing_template" + ".base_slot_description",
                "Put ");

        provider.add("screen.anvilcraft.structure_tool.size", "Size:");
        provider.add("screen.anvilcraft.structure_tool.count", "Count: %d");
        provider.add("screen.anvilcraft.structure_tool.to_data_gen", "To Data Gen");
        provider.add("screen.anvilcraft.structure_tool.to_kubejs", "To KubeJS");
        provider.add("screen.anvilcraft.structure_tool.to_json", "To JSON");

        provider.add("screen.anvilcraft.anvil_hammer.title", "Modifying Block");

        provider.add("screen.anvilcraft.active_silencer.search", "enter keyword to search");

        provider.add("screen.anvilcraft.tesla_tower.filter.unknown", "Unknown Filter");
        provider.add("screen.anvilcraft.tesla_tower.filter.is_player_id", "Player Id Filter");
        provider.add("screen.anvilcraft.tesla_tower.filter.is_player", "Player Filter");
        provider.add("screen.anvilcraft.tesla_tower.filter.is_pet", "Pet Filter");
        provider.add("screen.anvilcraft.tesla_tower.filter.is_on_vehicle", "On Vehicle Filter");
        provider.add("screen.anvilcraft.tesla_tower.filter.is_friendly", "Friendly Entity Filter");
        provider.add("screen.anvilcraft.tesla_tower.filter.is_entity_id", "Entity Filter");
        provider.add("screen.anvilcraft.tesla_tower.filter.is_baby_friendly", "Baby Friendly Entity Filter");
        provider.add("screen.anvilcraft.tesla_tower.filter.has_custom_name", "Custom Named Entity Filter");
    }
}
