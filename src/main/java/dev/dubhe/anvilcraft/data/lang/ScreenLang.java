package dev.dubhe.anvilcraft.data.lang;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumLangProvider;

public class ScreenLang {
    /**
     * 初始化 GUI 文本生成器
     *
     * @param provider 提供器
     */
    @SuppressWarnings("checkstyle:LineLength")
    public static void init(RegistrumLangProvider provider) {
        provider.add("screen.anvilcraft.button.direction", "Output Direction: %s");
        provider.add("screen.anvilcraft.button.direction.down", "Down");
        provider.add("screen.anvilcraft.button.direction.east", "East");
        provider.add("screen.anvilcraft.button.direction.north", "North");
        provider.add("screen.anvilcraft.button.direction.south", "South");
        provider.add("screen.anvilcraft.button.direction.up", "Up");
        provider.add("screen.anvilcraft.button.direction.west", "West");
        provider.add("screen.anvilcraft.button.off", "off");
        provider.add("screen.anvilcraft.button.on", "on");
        provider.add("screen.anvilcraft.button.filter_mode", "Filter Mode: %s");
        provider.add("screen.anvilcraft.button.filter_mode_any", "Any");
        provider.add("screen.anvilcraft.button.filter_mode_all", "All");
        provider.add("screen.anvilcraft.button.record", "Retention item filtering: %s");

        provider.add("screen.anvilcraft.button.compare_mode_hysteresis", "Mode: Hysteresis");
        provider.add("screen.anvilcraft.button.compare_mode_window", "Mode: Window");
        provider.add("screen.anvilcraft.button.redstone_control", "Redstone control on\nUse alternate signal to determine thresholds");
        provider.add("screen.anvilcraft.button.redstone_control_off", "Redstone control off");
        provider.add("screen.anvilcraft.button.reverse_off", "Output normal");
        provider.add("screen.anvilcraft.button.reverse", "Output reverse");

        provider.add("screen.anvilcraft.button.pulse_generator.start_mode.rising", "Mode: Rising Mode");
        provider.add("screen.anvilcraft.button.pulse_generator.start_mode.falling", "Mode: Falling Mode");
        provider.add("screen.anvilcraft.button.pulse_generator.start_mode.loop", "Mode: Loop Mode");
        provider.add("screen.anvilcraft.button.pulse_generator.reverse.off", "Reverse Mode: Off");
        provider.add("screen.anvilcraft.button.pulse_generator.reverse.on", "Reverse Mode: On");

        provider.add("screen.anvilcraft.filter.scroll_to_change", "Scroll mouse to change count");
        provider.add("screen.anvilcraft.filter.shift_to_scroll_faster", "Hold Shift to scroll faster");
        provider.add("screen.anvilcraft.slot.disable.tooltip", "Use item clicks to set filter");
        provider.add("screen.anvilcraft.royal_grindstone.will_remove", "Will remove:");
        provider.add("screen.anvilcraft.royal_grindstone.curse_count", "Curses: %1$s / %2$s");
        provider.add("screen.anvilcraft.royal_grindstone.repair_cost", "Repair cost: %1$s / %2$s");
        provider.add("screen.anvilcraft.royal_grindstone.gold_cost", "Gold cost: %1$s");
        provider.add("screen.anvilcraft.royal_grindstone.title", "Remove curse and repair cost");
        provider.add("screen.anvilcraft.ember_grindstone.title", "Extract enchantment");
        provider.add("screen.anvilcraft.ember_grindstone.cost", "Exp. Cost: %d");
        provider.add("screen.anvilcraft.frost_grindstone.title", "Disenchant");
        provider.add("screen.anvilcraft.ember_grindstone.earn", "Exp. Earn: %d");

        provider.add("screen.anvilcraft.royal_steel_upgrade_smithing_template", "Royal Steel Upgrade");
        provider.add("screen.anvilcraft.smithing_template.royal_steel_upgrade_smithing_template.additions_slot_description", "Put the Royal Steel Ingot or Royal Steel Block");
        provider.add("screen.anvilcraft.smithing_template.royal_steel_upgrade_smithing_template.applies_to", "Anvil, Smithing Table, Grindstone, Anvil Hammer, Amethyst Tools, Golden Tools, Iron Tools, Diamond Tools");
        provider.add("screen.anvilcraft.smithing_template.royal_steel_upgrade_smithing_template.base_slot_description", "Put upgradable item");
        provider.add("screen.anvilcraft.smithing_template.royal_steel_upgrade_smithing_template.upgrade_ingredients", "Royal Steel Ingot or Royal Steel Block");

        provider.add("screen.anvilcraft.transcendium_upgrade_smithing_template", "Transcendium Upgrade");
        provider.add("screen.anvilcraft.smithing_template.transcendium_upgrade_smithing_template.upgrade_ingredients", "Transcendium Ingot or Transcendium Block");
        provider.add("screen.anvilcraft.smithing_template.transcendium_upgrade_smithing_template.applies_to", "Ember Metal Anvil, Ember Anvil Hammer, Ember Dragon Rod");
        provider.add("screen.anvilcraft.smithing_template.transcendium_upgrade_smithing_template.base_slot_description", "Put upgradable item");
        provider.add("screen.anvilcraft.smithing_template.transcendium_upgrade_smithing_template.additions_slot_description", "Put the Transcendium Ingot or Transcendium Block");

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

        provider.add("tooltip.anvilcraft.redstone.title", "Redstone Stats:");
        provider.add("tooltip.anvilcraft.redstone.power", "  Power: %d");
        provider.add("tooltip.anvilcraft.redstone.output_power", "  Output Power: %d");
        provider.add("tooltip.anvilcraft.redstone.output_mode", "  Output Mode: %s");
        provider.add("tooltip.anvilcraft.redstone.output_mode.compare", "Compare");
        provider.add("tooltip.anvilcraft.redstone.output_mode.subtract", "Subtract");

        provider.add("tooltip.anvilcraft.heat.title", "Heat Stats:");
        provider.add("tooltip.anvilcraft.heat.tier", "Tier: %s");
        provider.add("tooltip.anvilcraft.heat.tier.normal", "Normal");
        provider.add("tooltip.anvilcraft.heat.tier.heated", "Heated");
        provider.add("tooltip.anvilcraft.heat.tier.redhot", "RedHot");
        provider.add("tooltip.anvilcraft.heat.tier.glowing", "Glowing");
        provider.add("tooltip.anvilcraft.heat.tier.incandescent", "Incandescent");
        provider.add("tooltip.anvilcraft.heat.tier.overheated", "Overheated");
        provider.add("tooltip.anvilcraft.heat.duration", "Duration: %s");

        provider.add("tooltip.anvilcraft.propel_piston.state", "Propel Piston State: ");
        provider.add("tooltip.anvilcraft.propel_piston.remaining_energy", "  Remaining Energy: %s");
        provider.add("tooltip.anvilcraft.propel_piston.remaining_push", "  Remaining Push: %s block-time");

        provider.add("block.anvilcraft.heat_collector.placement_too_close_to_another", "Too close to another heat collector");
        provider.add("tooltip.anvilcraft.heat_collector.not_work", "Heat Collector is not working");

        provider.add("block.anvilcraft.void_energy_collector.placement_too_close_to_another", "Too close to another void energy collector");

        provider.add("screen.anvilcraft.active_silencer.title", "Active Silencer");

        provider.add("block.anvilcraft.heliostats.invalid_placement", "Invalid placement");
        provider.add("block.anvilcraft.heliostats.placement_no_pos", "Irradiation position not set");
        provider.add("item.anvilcraft.heliostats.pos_set", "Will irradiate %s");
        provider.add("tooltip.anvilcraft.heliostats.not_work", "Heliostats are not working");
        provider.add("tooltip.anvilcraft.heliostats.no_rotation_angle", "  No possible rotation angle to irradiate the target");
        provider.add("tooltip.anvilcraft.heliostats.no_sun", "  No sunlight");
        provider.add("tooltip.anvilcraft.heliostats.obscured", "  The illumination path is obscured");
        provider.add("tooltip.anvilcraft.heliostats.too_far", "  The illumination pos is too far");
        provider.add("tooltip.anvilcraft.heliostats.unspecified_irradiation_block", "  Unspecified irradiation block");
        provider.add("tooltip.anvilcraft.heliostats.unknown", "  Unknown reason");

        provider.add("tooltip.anvilcraft.working_progress.title", "Working progress:");
        provider.add("tooltip.anvilcraft.working_progress.progress", "  %1$s %2$s%%");

        provider.add("tooltip.anvilcraft.space_overcompressor.stored_mass", "Stored Mass: %s");

        provider.add("screen.anvilcraft.ember_metal_upgrade_smithing_template", "Ember Metal Upgrade");
        provider.add("screen.anvilcraft.smithing_template.ember_metal_upgrade_smithing_template.additions_slot_description", "Put the Ember Metal Ingot or Ember Metal Block");
        provider.add("screen.anvilcraft.smithing_template.ember_metal_upgrade_smithing_template.applies_to", "Royal Steel Anvil Hammer, Netherite Tools, Royal Steel Tools, Royal Steel WorkStations");
        provider.add("screen.anvilcraft.smithing_template.ember_metal_upgrade_smithing_template.base_slot_description", "Put upgradable item");
        provider.add("screen.anvilcraft.smithing_template.ember_metal_upgrade_smithing_template.upgrade_ingredients", "Ember Metal Ingot or Ember Metal Block");

        provider.add("screen.anvilcraft.structure_tool.size", "Size:");
        provider.add("screen.anvilcraft.structure_tool.count", "Count: %d");
        provider.add("screen.anvilcraft.structure_tool.to_data_gen", "To Data Gen");
        provider.add("screen.anvilcraft.structure_tool.to_kubejs", "To KubeJS");
        provider.add("screen.anvilcraft.structure_tool.to_json", "To JSON");
        provider.add("screen.anvilcraft.structure_tool.regular_recipe", "Put any normal item to generate multiblock crafting recipe");
        provider.add("screen.anvilcraft.structure_tool.conversion_recipe", "Put another structure tool to generate multiblock conversion recipe");
        provider.add("screen.anvilcraft.structure_tool.conversion_output", "The area selected by it will be the output of recipe");

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

        provider.add("hud.anvilcraft.ionocraft_backpack_power", "Ionocraft Backpack Power: %d%%");

        provider.add("screen.anvilcraft.frost_metal_upgrade_smithing_template", "Frost Metal Upgrade");
        provider.add("screen.anvilcraft.smithing_template.frost_metal_upgrade_smithing_template.additions_slot_description", "Put the Frost Metal Ingot");
        provider.add("screen.anvilcraft.smithing_template.frost_metal_upgrade_smithing_template.applies_to", "Royal Steel Tools");
        provider.add("screen.anvilcraft.smithing_template.frost_metal_upgrade_smithing_template.base_slot_description", "Put upgradable item");
        provider.add("screen.anvilcraft.smithing_template.frost_metal_upgrade_smithing_template.upgrade_ingredients", "Frost Metal Ingot");

        provider.add("screen.anvilcraft.ember_smithing.tooltip.missing_template", "Add Multiple to One Template");
        provider.add("screen.anvilcraft.ember_smithing.two.missing", "Add Multiphase Transcendium");
        provider.add("screen.anvilcraft.ember_smithing.four.missing", "Add Resonator Core, Heavy Halberd Core, Multiphase Transcendium or Frost Metal Block");
        provider.add("screen.anvilcraft.ember_smithing.eight.missing", "Add Multiphase Matter");
        provider.add("screen.anvilcraft.ember_smithing.multiphase_matter.missing_tools", "Add a Shear, a Flint and Steel, a Brush, a Spyglass, a Magnet, a Fishing Rod, a Carrot on a Stick and a Warped Fungus on a Stick");
        provider.add("screen.anvilcraft.ember_smithing.heavy_halberd_core.missing_tools", "Add a sword and an axe of the same type, a Trident and a Mace");
        provider.add("screen.anvilcraft.ember_smithing.resonator_core.missing_tools", "Add same Ember or Frost metal tools");
        provider.add("screen.anvilcraft.ember_smithing.frost_metal_block.missing_tools", "Add Amulets in same category");
        provider.add("screen.anvilcraft.ember_smithing.multiphase_transcendium.missing_tools", "Add a Ember Metal Resonator or Heavy Halberd and a Frost Metal Resonator or Heavy Halberd");
        provider.add("screen.anvilcraft.ember_smithing.multiphase_transcendium.resonator_missing_tools", "Add a Ember Metal Resonator and a Frost Metal Resonator");
        provider.add("screen.anvilcraft.ember_smithing.multiphase_transcendium.heavy_halberd_missing_tools", "Add a Ember Metal Heavy Halberd and a Frost Metal Heavy Halberd");

        provider.add("screen.anvilcraft.resonator.title", "Switch resonate mode");
        provider.add("screen.anvilcraft.resonator.auto", "Auto");
        provider.add("screen.anvilcraft.resonator.axe", "Axe");
        provider.add("screen.anvilcraft.resonator.shovel", "Shovel");
        provider.add("screen.anvilcraft.resonator.hoe", "Hoe");
        provider.add("screen.anvilcraft.resonator.pickaxe", "Pickaxe");

        provider.add("screen.anvilcraft.multiphase.title", "Switch phase");
        provider.add("screen.anvilcraft.multiphase.merciless", "-Merciless");

        provider.add("screen.anvilcraft.multitool.title", "Switch tool");
        provider.add("screen.anvilcraft.multitool.all", "All");

        provider.add("screen.anvilcraft.deflection_ring.state", "Deflection Ring State");
        provider.add("screen.anvilcraft.deflection_ring.speed", "  Last Entity Speed: %d m/tick");

        provider.add("screen.anvilcraft.filter.black_list", "Black List");
        provider.add("screen.anvilcraft.filter.white_list", "White List");
        provider.add("screen.anvilcraft.filter.match_component", "Match Component");
        provider.add("screen.anvilcraft.filter.mismatch_component", "Mismatch Component");

        provider.add("screen.anvilcraft.filter.scroll_wheel_to_change_stack_limit", "Scroll mouse wheel to change stack limit");

        provider.add("screen.anvilcraft.frost_smithing.tooltip.missing_template", "Add Permutation Template or Deformation Template");
        provider.add("screen.anvilcraft.frost_smithing.permutation.missing", "Add Royal Steel Ingot, Ember Metal Ingot, Multiphase Matter or Multiphase Matter Block");
        provider.add("screen.anvilcraft.frost_smithing.deformation.missing_tools", "Add any type of Swords, tools, armors or bow-likes");
        provider.add("screen.anvilcraft.frost_smithing.royal_steel_ingot.missing_tools", "Add Diamond weapons or tools or Royal Steel weapons or tools");
        provider.add("screen.anvilcraft.frost_smithing.ember_metal_ingot.missing_tools", "Add Netherite weapons or tools or Ember Metal weapons or tools");
        provider.add("screen.anvilcraft.frost_smithing.multiphase_matter.missing_tools", "Add Frost Metal weapons or tools or Ember Metal weapons or tools");
        provider.add("screen.anvilcraft.frost_smithing.multiphase_matter_block.missing_tools", "Add Frost workstations or Ember workstations");

        provider.add("screen.anvilcraft.exp_collector.tooltip", "Exp: %s/%smB");

        provider.add("screen.anvilcraft.smart_block_placer.layer.1", "Layer 1");
        provider.add("screen.anvilcraft.smart_block_placer.layer.2", "Layer 2");
        provider.add("screen.anvilcraft.smart_block_placer.layer.3", "Layer 3");
        provider.add("screen.anvilcraft.smart_block_placer.layer.4", "Layer 4");
        provider.add("screen.anvilcraft.smart_block_placer.layer.5", "Layer 5");
        provider.add("screen.anvilcraft.smart_block_placer.layer_mode.all", "Show All Layers");
        provider.add("screen.anvilcraft.smart_block_placer.layer_mode.single", "Show Layer %s / 5");
        provider.add("screen.anvilcraft.smart_block_placer.operation_mode.pickup", "Pickup Mode: Retrieve blocks from containers behind");
        provider.add("screen.anvilcraft.smart_block_placer.operation_mode.move", "Move Mode: Move the blocks behind to the placement position.");
        provider.add("screen.anvilcraft.smart_block_placer.missing_mode.skip", "Skip Mode: Skip missing blocks during placement");
        provider.add("screen.anvilcraft.smart_block_placer.missing_mode.stop", "Stop Mode: Stop placement when blocks are missing");
        provider.add("screen.anvilcraft.smart_block_placer.position.selected", "Position (%s, %s) - Selected");
        provider.add("screen.anvilcraft.smart_block_placer.position.unselected", "Position (%s, %s) - Not selected");
        provider.add("screen.anvilcraft.smart_block_placer.preview.empty", "No positions configured");
        provider.add("screen.anvilcraft.smart_block_placer.disk_slot", "Disk Slot: Place Disk and enable blueprint mode");
        provider.add("screen.anvilcraft.smart_block_placer.book_slot", "Book Slot: Place book in blueprint mode");
        provider.add("screen.anvilcraft.smart_block_placer.structure.loaded", "Loaded: ");
        provider.add("screen.anvilcraft.smart_block_placer.missing.block", "Missing:");
        provider.add("screen.anvilcraft.smart_block_placer.no_structure_record", "Record a structure with the Structure Scanner first");
        
        // Structure Material Book
        provider.add("book.anvilcraft.material_list.missing_header", "Missing:");
        
        // Structure Scanner
        provider.add("screen.anvilcraft.structure_scanner.info_title", "Structure Info");
        provider.add("screen.anvilcraft.structure_scanner.ready", "Structure scan ready");
        provider.add("screen.anvilcraft.structure_scanner.tooltip.large_structure", "This structure is large and cannot be placed by the Smart Block Placer, but can still be saved");
        provider.add("screen.anvilcraft.structure_scanner.tooltip.unknown_blocks", "Structure contains unknown blocks");
        provider.add("screen.anvilcraft.structure_scanner.tooltip.too_large", "Structure is too large to save");
        provider.add("screen.anvilcraft.structure_scanner.tooltip.multiblock_blocks", "Structure contains multiblock blocks, cannot be recognized by Smart Block Placer");
    }
}
