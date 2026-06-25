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
        provider.add("tooltip.anvilcraft.grid_information.output_power", "  Power Generation: %s");
        provider.add("tooltip.anvilcraft.grid_information.input_power", "  Power Consumption: %s");
        provider.add("tooltip.anvilcraft.grid_information.total_consumed", "  Total Consumption: %s");
        provider.add("tooltip.anvilcraft.grid_information.total_generated", "  Total Generation: %s");
        provider.add("tooltip.anvilcraft.grid_information.utilization", "  Power Utilization: %s");
        provider.add("tooltip.anvilcraft.grid_information.overloaded1", "It appears that this grid is overloaded.");
        provider.add("tooltip.anvilcraft.grid_information.overloaded2", "Add more sources or remove the components");
        provider.add("tooltip.anvilcraft.grid_information.overloaded3", "with a high stress impact.");

        provider.add("tooltip.anvilcraft.fluid_tank.capacity", "Capacity:");
        provider.add("tooltip.anvilcraft.fluid_tank.capacity.value", "  %s / %s");
        provider.add("tooltip.anvilcraft.fluid_tank.capacity.value.infinity", "  %s / ∞");
        provider.add("tooltip.anvilcraft.fluid_tank.fluid", "Fluid:");

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

        provider.add("tooltip.anvilcraft.burning_heater.state_name", "State:");
        provider.add("tooltip.anvilcraft.burning_heater.state_name.off", "  Off");
        provider.add("tooltip.anvilcraft.burning_heater.state_name.smoldering", "  Smoldering");
        provider.add("tooltip.anvilcraft.burning_heater.state_name.lit", "  Lit");
        provider.add("tooltip.anvilcraft.burning_heater.burn_time_label", "Remaining Burn Time:");
        provider.add("tooltip.anvilcraft.burning_heater.can_smelt", "Can Smelt:");
        provider.add("tooltip.anvilcraft.burning_heater.can_smelt.yes", "  Yes");
        provider.add("tooltip.anvilcraft.burning_heater.can_smelt.no", "  No");

        // Jade provider also uses these — no .jade. infix needed as they share the tooltip.anvilcraft.burning_heater namespace
        provider.add("tooltip.anvilcraft.burning_heater.state.off", "State: Off");
        provider.add("tooltip.anvilcraft.burning_heater.state.smoldering", "State: Smoldering");
        provider.add("tooltip.anvilcraft.burning_heater.state.lit", "State: Lit");
        provider.add("tooltip.anvilcraft.burning_heater.burn_time", "Remaining Burn Time: %s");

        provider.add("block.anvilcraft.celestial_forging_anvil.placement_too_close_to_another", "Too close to another Celestial Forging Anvil"); // 距离另一个锻星砧太近
        provider.add("block.anvilcraft.celestial_forging_anvil_amplifier.need_anvil_corner", "Needs to be placed on the corner of the Celestial Forging Anvil");
        provider.add("block.anvilcraft.heat_collector.placement_too_close_to_another", "Too close to another heat collector");
        provider.add("tooltip.anvilcraft.heat_collector.not_work", "Heat Collector is not working");

        provider.add("block.anvilcraft.infinite_collector.placement_too_close_to_another", "Too close to another infinite collector");
        provider.add("block.anvilcraft.void_energy_collector.placement_too_close_to_another", "Too close to another void energy collector");

        provider.add("screen.anvilcraft.active_silencer.title", "Active Silencer");

        provider.add("block.anvilcraft.heliostats.invalid_placement", "Invalid placement");
        provider.add("block.anvilcraft.heliostats.placement_no_pos", "Irradiation position not set");
        provider.add("item.anvilcraft.heliostats.pos_set", "Will irradiate %s");
        provider.add("tooltip.anvilcraft.heliostats.not_work", "Heliostats are not working");
        provider.add("tooltip.anvilcraft.heliostats.no_rotation_angle", "  No possible direction angle to irradiate the target");
        provider.add("tooltip.anvilcraft.heliostats.no_sun", "  No sunlight");
        provider.add("tooltip.anvilcraft.heliostats.obscured", "  The illumination path is obscured");
        provider.add("tooltip.anvilcraft.heliostats.too_far", "  The illumination pos is too far");
        provider.add("tooltip.anvilcraft.heliostats.unspecified_irradiation_block", "  Unspecified irradiation block");
        provider.add("tooltip.anvilcraft.heliostats.unknown", "  Unknown reason");

        provider.add("tooltip.anvilcraft.working_progress.title", "Working progress:");
        provider.add("tooltip.anvilcraft.working_progress.progress", "  %1$s %2$s%%");
        provider.add("tooltip.anvilcraft.working_progress.time", "  %1$s / %2$s");

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
        provider.add("screen.anvilcraft.tesla_tower.filter.is_hostile", "Hostile Entity Filter");
        provider.add("screen.anvilcraft.tesla_tower.filter.is_neutral", "Neutral Entity Filter");
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

        provider.add("screen.anvilcraft.trading_station.not_owner", "You are not the owner of this Trading Station!");
        provider.add("screen.anvilcraft.trading_station.provide", "Mark as providing items");
        provider.add("screen.anvilcraft.trading_station.request", "Mark as requesting items");
        provider.add("screen.anvilcraft.trading_station.player_allow", "Allow trading with player");
        provider.add("screen.anvilcraft.trading_station.player_not_allow", "Disallow trading with player");
        provider.add("screen.anvilcraft.trading_station.villager_allow", "Allow trading with villager");
        provider.add("screen.anvilcraft.trading_station.villager_not_allow", "Disallow trading with villager");
        provider.add("screen.anvilcraft.trading_station.input_allow", "Allow automatically inputting");
        provider.add("screen.anvilcraft.trading_station.input_not_allow", "Disallow automatically inputting");
        provider.add("screen.anvilcraft.trading_station.output_allow", "Allow automatically outputting");
        provider.add("screen.anvilcraft.trading_station.output_not_allow", "Disallow automatically outputting");

        // Celestial Forging Anvil (所有冒号后面都要带空格，中文也是！)
        provider.add("screen.anvilcraft.celestial_forging_anvil", "Celestial Forging Anvil"); // 锻星砧
        provider.add("screen.anvilcraft.cfa.celestial_params", "Parameter"); // 天体参数
        provider.add("screen.anvilcraft.cfa.lock", "Click to lock"); // 点击锁定
        provider.add("screen.anvilcraft.cfa.locked_tooltip", "Operation requires unlocking first"); // 需要先解锁才可操作，如果你不是锻星砧拥有者，请询问拥有者后操作
        provider.add("screen.anvilcraft.cfa.missing_amplifier.line1", "Amplifier missing,"); // 滚木
        provider.add("screen.anvilcraft.cfa.missing_amplifier.line2", "celestial body status"); // 缺少增幅器，天体状态暂无法同步
        provider.add("screen.anvilcraft.cfa.missing_amplifier.line3", "cannot be synced"); // 滚木
        provider.add("screen.anvilcraft.cfa.power_fail", "Insufficient power"); // 电力不足
        provider.add("screen.anvilcraft.cfa.re_search_tooltip", "Search Again"); // 再次搜索
        provider.add("screen.anvilcraft.cfa.search_fail", "Unreasonable parameters"); // 参数不合理，无法搜索
        provider.add("screen.anvilcraft.cfa.search_loading", "Searching"); // 参数合理，搜索中
        provider.add("screen.anvilcraft.cfa.search_tooltip", "Search for celestial bodies with this parameter"); // 搜索该参数的天体
        provider.add("screen.anvilcraft.cfa.unlock", "Shift-Click to unlock"); // 按住Shift点击以解锁
        provider.add("screen.anvilcraft.cfa.unlock_warning", "Unlock will delete the megastructure, Shift-Click to confirm"); // 解锁会删除已建造的巨构，按住Shift点击确认解锁
        provider.add("screen.anvilcraft.cfa.radius", "Radius: %s"); // 半径: %s
        provider.add("screen.anvilcraft.cfa.age", "Age: %s"); // 年龄: %s
        provider.add("screen.anvilcraft.cfa.mass", "Mass: %s"); // 质量: %s
        provider.add("screen.anvilcraft.cfa.tilt", "Axial Tilt: %s"); // 自转轴倾角: %s

        provider.add("screen.anvilcraft.cfa.atmos", "Atmosphere: %s"); // 大气: %s
        provider.add("screen.anvilcraft.cfa.atmos.yes", "Yes"); // 有
        provider.add("screen.anvilcraft.cfa.none", "None"); // 无

        provider.add("screen.anvilcraft.cfa.mag", "Magnetic Field: %s"); // 磁场: %s
        provider.add("screen.anvilcraft.cfa.mag.very_weak", "Very Weak"); // 几乎没有
        provider.add("screen.anvilcraft.cfa.mag.weak", "Weak"); // 弱
        provider.add("screen.anvilcraft.cfa.mag.medium", "Medium"); // 中等
        provider.add("screen.anvilcraft.cfa.mag.strong", "Strong"); // 强
        provider.add("screen.anvilcraft.cfa.mag.very_strong", "Very Strong"); // 非常强
        provider.add("screen.anvilcraft.cfa.mag.extreme", "Extreme"); // 极端强

        provider.add("screen.anvilcraft.cfa.spin", "Spin: %s"); // 自转速度: %s
        provider.add("screen.anvilcraft.cfa.spin.very_slow", "Very Slow"); // 非常慢
        provider.add("screen.anvilcraft.cfa.spin.slow", "Slow"); // 慢
        provider.add("screen.anvilcraft.cfa.spin.medium", "Medium"); // 中等
        provider.add("screen.anvilcraft.cfa.spin.fast", "Fast"); // 快
        provider.add("screen.anvilcraft.cfa.spin.very_fast", "Very Fast"); // 非常快
        provider.add("screen.anvilcraft.cfa.spin.super_fast", "Super Fast"); // 极端快

        provider.add("screen.anvilcraft.cfa.temp", "Temperature: %s"); // 表面温度: %s
        provider.add("screen.anvilcraft.cfa.temp.freezing", "Freezing"); // 极寒
        provider.add("screen.anvilcraft.cfa.temp.cold", "Cold"); // 寒冷
        provider.add("screen.anvilcraft.cfa.temp.mild", "Mild"); // 温和
        provider.add("screen.anvilcraft.cfa.temp.hot", "Hot"); // 炎热
        provider.add("screen.anvilcraft.cfa.temp.scorched", "Scorched"); // 焦土

        provider.add("screen.anvilcraft.cfa.liquid", "Liquid Coverage: %s"); // 液体覆盖率: %s
        provider.add("screen.anvilcraft.cfa.liquid.none", "None"); // 无
        provider.add("screen.anvilcraft.cfa.liquid.low", "Low"); // 低
        provider.add("screen.anvilcraft.cfa.liquid.medium", "Medium"); // 中
        provider.add("screen.anvilcraft.cfa.liquid.high", "High"); // 高

        provider.add("screen.anvilcraft.cfa.pressure", "Pressure Type: %s"); // 压力类型: %s
        provider.add("screen.anvilcraft.cfa.pressure.gas", "Gas"); // 气体
        provider.add("screen.anvilcraft.cfa.pressure.ice", "Ice"); // 冰

        provider.add("screen.anvilcraft.cfa.wind", "Wind Speed: %s"); // 风速: %s
        provider.add("screen.anvilcraft.cfa.wind.high", "High"); // 高
        provider.add("screen.anvilcraft.cfa.wind.very_high", "Very High"); // 非常高

        provider.add("screen.anvilcraft.cfa.type", "Type: %s"); // 天体类型: %s
        provider.add("screen.anvilcraft.cfa.class.no_match", "Null"); // ？？？
        provider.add("screen.anvilcraft.cfa.class.large_moon", "Large Moon"); // 大型卫星
        provider.add("screen.anvilcraft.cfa.class.rocky_planet", "Rocky Planet"); // 岩石行星
        // Rocky planet types — keyed by temperature × liquid × atmosphere
        provider.add("screen.anvilcraft.cfa.class.freezing_no_liquid_no_atmos", "Deathly Frozen"); // 死寂冻土
        provider.add("screen.anvilcraft.cfa.class.freezing_no_liquid_atmos", "Desolate Frozen"); // 荒原冻土
        provider.add("screen.anvilcraft.cfa.class.freezing_liquid", "Frozen Planet"); // 冰封星球
        provider.add("screen.anvilcraft.cfa.class.scorched_no_liquid_no_atmos", "Deathly Scorched"); // 死寂焦土
        provider.add("screen.anvilcraft.cfa.class.scorched_no_liquid_atmos", "Desolate Scorched"); // 荒原焦土
        provider.add("screen.anvilcraft.cfa.class.scorched_liquid", "Lava Planet"); // 熔岩星球
        provider.add("screen.anvilcraft.cfa.class.deathly_planet", "Deathly Planet"); // 死寂星球
        provider.add("screen.anvilcraft.cfa.class.desert_planet", "Desert Planet"); // 荒漠星球
        provider.add("screen.anvilcraft.cfa.class.cold_riverbank", "Frozen Riverbank"); // 冰原河滩
        provider.add("screen.anvilcraft.cfa.class.mild_riverbank", "Warm Riverbank"); // 温和河滩
        provider.add("screen.anvilcraft.cfa.class.hot_riverbank", "Sweltering Riverbank"); // 炎热河滩
        provider.add("screen.anvilcraft.cfa.class.cold_land_ocean", "Frozen Land-Ocean"); // 冰原海陆
        provider.add("screen.anvilcraft.cfa.class.mild_land_ocean", "Warm Land-Ocean"); // 温和海陆
        provider.add("screen.anvilcraft.cfa.class.hot_land_ocean", "Sweltering Land-Ocean"); // 炎热海陆
        provider.add("screen.anvilcraft.cfa.class.cold_ocean", "Frozen Ocean"); // 冰封海洋
        provider.add("screen.anvilcraft.cfa.class.mild_ocean", "Warm Ocean"); // 温和海洋
        provider.add("screen.anvilcraft.cfa.class.hot_ocean", "Sweltering Ocean"); // 过热海洋

        provider.add("screen.anvilcraft.cfa.class.ice_giant", "Ice Giant"); // 冰巨行星
        provider.add("screen.anvilcraft.cfa.class.gas_giant", "Gas Giant"); // 气体巨星
        provider.add("screen.anvilcraft.cfa.class.brown_dwarf", "Brown Dwarf");

        provider.add("screen.anvilcraft.cfa.class.m_main", "M-Star"); // Red Dwarf 红矮星
        provider.add("screen.anvilcraft.cfa.class.k_main", "K-Star"); // Orange Dwarf 橙矮星
        provider.add("screen.anvilcraft.cfa.class.g_main", "G-Star"); // Yellow Dwarf 黄矮星
        provider.add("screen.anvilcraft.cfa.class.f_main", "F-Star"); // Yellow-White Main 黄白主序星
        provider.add("screen.anvilcraft.cfa.class.a_main", "A-Star"); // White Main 白主序星
        provider.add("screen.anvilcraft.cfa.class.b_main", "B-Star"); // Blue-White Main 蓝主序星
        provider.add("screen.anvilcraft.cfa.class.o_main", "O-Star"); // Blue Main 蓝主序星
        provider.add("screen.anvilcraft.cfa.class.m_giant", "M-Giant"); // Red Giant 红巨星
        provider.add("screen.anvilcraft.cfa.class.k_giant", "K-Giant"); // Orange Giant 橙巨星
        provider.add("screen.anvilcraft.cfa.class.g_giant", "G-Giant"); // Yellow Giant 黄巨星
        provider.add("screen.anvilcraft.cfa.class.f_giant", "F-Giant"); // Yellow-White Giant 黄白巨星
        provider.add("screen.anvilcraft.cfa.class.a_giant", "A-Giant"); // White Giant 白巨星
        provider.add("screen.anvilcraft.cfa.class.b_giant", "B-Giant"); // Blue-White Giant 蓝白巨星
        provider.add("screen.anvilcraft.cfa.class.o_giant", "O-Giant"); // Blue Giant 蓝巨星
        provider.add("screen.anvilcraft.cfa.class.m_supergiant", "M-Supergiant"); // Red Supergiant 红超巨星
        provider.add("screen.anvilcraft.cfa.class.k_supergiant", "K-Supergiant"); // Orange Supergiant 橙超巨星
        provider.add("screen.anvilcraft.cfa.class.g_supergiant", "G-Supergiant"); // Yellow Supergiant 黄超巨星
        provider.add("screen.anvilcraft.cfa.class.f_supergiant", "F-Supergiant"); // Yellow-White Supergiant 黄白超巨星
        provider.add("screen.anvilcraft.cfa.class.a_supergiant", "A-Supergiant"); // White Supergiant 白超巨星
        provider.add("screen.anvilcraft.cfa.class.b_supergiant", "B-Supergiant"); // Blue-White Supergiant 蓝白超巨星
        provider.add("screen.anvilcraft.cfa.class.o_supergiant", "O-Supergiant"); // Blue Supergiant 蓝超巨星
        provider.add("screen.anvilcraft.cfa.class.white_dwarf", "White Dwarf"); // 白矮星

        // Celestial Restriction Ring Refactor
        provider.add("screen.anvilcraft.cfa.refactor_title", "Refactor"); // 束星环重构
        provider.add("screen.anvilcraft.cfa.need_lock", "Need to lock"); // 需要先锁定搜索结果
        provider.add("screen.anvilcraft.cfa.no_refactor_option", "No refactoring option selected"); // 未选择再构选项
        provider.add("screen.anvilcraft.cfa.insufficient_materials", "Insufficient building materials"); // 建材不足
        provider.add("screen.anvilcraft.cfa.material_required", "Requires: %s × %s"); // 需求: %s × %s
        provider.add("screen.anvilcraft.cfa.refactor_materials", "Refactor materials"); // 重构建材
        provider.add("screen.anvilcraft.cfa.refactor_start_tooltip", "Refactor Celestial Restriction into a selected megastructure"); // 将束星环重构为选择的巨构
        // CFA Interface tooltips
        provider.add("screen.anvilcraft.cfa.logistics_interface.title", "Logistics Interface"); // 物流接口
        provider.add("screen.anvilcraft.cfa.fluid_interface.title", "Fluid Interface"); // 流体接口
        provider.add("screen.anvilcraft.cfa.laser_interface.title", "Laser Interface"); // 激光接口
        provider.add("screen.anvilcraft.cfa.interface.empty", "(Empty)"); // (空)
        provider.add("screen.anvilcraft.cfa.laser_interface.received", "Receiving: Lv.%s"); // 接收激光: 等级.%s+
        provider.add("screen.anvilcraft.cfa.laser_interface.received_gamma", "Receiving: Gamma Lv.%s"); // 接收伽马激光: 等级.%s+
        provider.add("screen.anvilcraft.cfa.laser_interface.emitting", "Emitting: Lv.%s"); // 发射激光: 等级.%s+
        provider.add("screen.anvilcraft.cfa.laser_interface.emitting_gamma", "Emitting: Gamma Lv.%s"); // 发射伽马激光: 等级.%s+
        provider.add("screen.anvilcraft.cfa.laser_interface.no_laser", "(No laser)"); // (无激光)
        provider.add("screen.anvilcraft.cfa.laser_interface.required", "Required: Lv.%s+"); // 需求激光: 等级.%s+
        provider.add("screen.anvilcraft.cfa.laser_interface.required_gamma", "Required: Gamma Lv.%s+"); // 需求伽马激光: 等级.%s+
        provider.add("screen.anvilcraft.cfa.laser_interface.valid", "✓ Valid"); // ✓ 达成
        provider.add("screen.anvilcraft.cfa.laser_interface.invalid", "✗ Invalid"); // ✗ 未达成

        // CFA interface HUD tooltip (displayed on logistics interface)
        provider.add("screen.anvilcraft.cfa.temple_demand", "◇ Temple Demand ◇"); // ◇ 神庙需求 ◇
        provider.add("screen.anvilcraft.cfa.collider_targets", "◇ Collider Targets ◇"); // ◇ 可撞击的物品 ◇
        provider.add("screen.anvilcraft.cfa.collider_processing", "◇ Processing"); // ◇ 正在加工
        provider.add("screen.anvilcraft.cfa.collider_star_missing", "! Star Missing !"); // ! 天体丢失 !

        // Megastructure names (English) — 11 unique megastructures
        provider.add("screen.anvilcraft.cfa.megastructure.planet_excavator", "Planet Excavator"); // 行星开采器
        provider.add("screen.anvilcraft.cfa.megastructure.planet_exctractor", "Planet Exctractor"); // 行星抽取器
        provider.add("screen.anvilcraft.cfa.megastructure.eco_station", "Ecological Station"); // 生态站
        provider.add("screen.anvilcraft.cfa.megastructure.temple", "Temple"); // 神庙
        provider.add("screen.anvilcraft.cfa.megastructure.giant_planet_exctractor", "Giant Planet Exctractor"); // 巨行星抽取器
        provider.add("screen.anvilcraft.cfa.megastructure.stellar_ring_collider", "Stellar Ring Collider"); // 星环对撞机
        provider.add("screen.anvilcraft.cfa.megastructure.dyson_sphere_small", "Dyson Sphere"); // 小戴森球
        provider.add("screen.anvilcraft.cfa.megastructure.dyson_sphere_large", "Dyson Sphere"); // 大戴森球
        provider.add("screen.anvilcraft.cfa.megastructure.magnetar_coil", "Magnetar Coil"); // 磁星线圈
        provider.add("screen.anvilcraft.cfa.megastructure.penrose_sphere", "Penrose Sphere"); // 彭罗斯球
        provider.add("screen.anvilcraft.cfa.megastructure.matter_decompressor", "Matter Decompressor"); // 物质解压器
        provider.add("screen.anvilcraft.cfa.megastructure.stellar_evolution_accelerator", "Stellar Evolution Accelerator"); // 恒星演化加速器
        provider.add("screen.anvilcraft.cfa.megastructure.wormhole_stabilizer", "Wormhole Stabilizer"); // 虫洞稳定器

        // Megastructure descriptions (Shift-expanded tooltips)
        provider.add("screen.anvilcraft.cfa.megastructure.planet_excavator.description", "Excavating mineral resources on the planet, and this may damage the planet's ecology");
        provider.add("screen.anvilcraft.cfa.megastructure.planet_exctractor.description", "Extracting fluid resources from the planet, and this may damage the planet's ecology");
        provider.add("screen.anvilcraft.cfa.megastructure.eco_station.description", "Cultivate creatures on the planet and harvest biological resources");
        provider.add("screen.anvilcraft.cfa.megastructure.temple.description", "Bestow blessings or mete out punishments to inferior civilization, and reap the resources they offer in homage");
        provider.add("screen.anvilcraft.cfa.megastructure.giant_planet_exctractor.description", "Extracting resources from the atmosphere of giant planet. In addition to gases and liquids, solid products may also be produced");
        provider.add("screen.anvilcraft.cfa.megastructure.stellar_ring_collider.description", "Utilizing the gravitational and magnetic fields of star to accelerate the anvil to higher speeds or to frequently impact and manufacture items in batches");
        provider.add("screen.anvilcraft.cfa.megastructure.dyson_sphere_small.description", "Collecting light energy from star, generate a large amount of electricity");
        provider.add("screen.anvilcraft.cfa.megastructure.dyson_sphere_large.description", "Collecting light energy from star, generate a large amount of electricity");
        provider.add("screen.anvilcraft.cfa.megastructure.magnetar_coil.description", "Generate electricity by utilizing the rapidly changing magnetic field of a magnetar");
        provider.add("screen.anvilcraft.cfa.megastructure.penrose_sphere.description", "Utilize the ergosphere generated by the high-speed rotation of a black hole to enhance laser energy");
        provider.add("screen.anvilcraft.cfa.megastructure.matter_decompressor.description", "Extract matter from the interior of neutron stars or within the event horizon of black holes by activating the exotic gravitational field with gamma laser");
        provider.add("screen.anvilcraft.cfa.megastructure.stellar_evolution_accelerator.description", "Accelerate the stellar evolution until the end of its lifespan, which may trigger a supernova explosion and give birth to extreme celestial bodys");
        provider.add("screen.anvilcraft.cfa.megastructure.wormhole_stabilizer.description", "By stabilizing the energy of black holes with negative matter, wormholes can be established between multiple identical black holes to enable the transmission of matter and energy");

        // CFA amplified + planet warning
        provider.add("screen.anvilcraft.cfa.amplified_planet_warning", "Cannot build small megastructures on an amplified CFA"); // 不能在增幅的锻星砧上建造小型巨构

        // Megastructure usage texts (shown below built megastructure button in CFA screen)
        // §c = red (laser), §e = gold (logistics), §b = aqua (fluid), §a = green (power),
        // §f = white (descriptions), §7 = gray (none/empty), §r = reset
        provider.add("screen.anvilcraft.cfa.megastructure.planet_excavator.usage",
            """
                Requires:
                §c[Lsr Int]§r
                §cLaser Lv.16+ §r§f(each beam adds 1× work efficiency)§r
                ———————
                Outputs:
                §e[Log Int]§r
                §fPlanetary mineral resources§r
                ———————
                Side Effects:
                §fReduces liquid coverage, causes biological extinction until the planet becomes a Deathly Planet§r"""
        );
        provider.add("screen.anvilcraft.cfa.megastructure.planet_exctractor.usage",
            """
                Requires:
                §7None§r
                ———————
                Outputs:
                §b[Flu Int]§r
                §fPlanetary fluid resources§r
                ———————
                Side Effects:
                §fReduces liquid coverage, causes biological extinction until the planet becomes a Deathly Planet§r"""
        );
        provider.add("screen.anvilcraft.cfa.megastructure.eco_station.usage",
            """
                Requires:
                §a[Power Grid]§r
                §f1000kW§r
                ———————
                Outputs:
                §e[Log Int]§r
                §fPlanetary biological items§r
                §b[Flu Int]§r
                §fPlanetary biological fluids§r
                ———————
                Side Effects:
                §7None§r"""
        );
        provider.add("screen.anvilcraft.cfa.megastructure.temple.usage",
            """
                Requires:
                §e[Log Int]§r
                §fOfferings and punishments§r
                ———————
                Outputs:
                §e[Log Int]§r
                §fPramitive civilization offering§r
                ———————
                Side Effects:
                §7None§r"""
        );
        provider.add("screen.anvilcraft.cfa.megastructure.giant_planet_exctractor.usage",
            """
                Requires:
                §7None§r
                ———————
                Outputs:
                §e[Log Int]§r
                §fGiant planet solid resources§r
                §b[Flu Int]§r
                §fGiant planet fluid resources§r
                ———————
                Side Effects:
                §7None§r"""
        );
        provider.add("screen.anvilcraft.cfa.megastructure.stellar_ring_collider.usage",
            """
                Requires:
                §e[Log Int]§r
                §fCollision materials and anvils§r
                §a[Power Grid]§r
                §f4000kW§r
                ———————
                Outputs:
                §e[Log Int]§r
                §fHigh-speed impact processed products§r
                ———————
                Side Effects:
                §7None§r"""
        );
        provider.add("screen.anvilcraft.cfa.megastructure.dyson_sphere_small.usage",
            """
                Requires:
                §7None§r
                ———————
                Outputs:
                §a[Power Grid]§r
                §fGenerates power based on stellar temperature and radius§r
                ———————
                Side Effects:
                §7None§r"""
        );
        provider.add("screen.anvilcraft.cfa.megastructure.dyson_sphere_large.usage",
            """
                Requires:
                §7None§r
                ———————
                Outputs:
                §a[Power Grid]§r
                §fGenerates power based on stellar temperature and radius§r
                ———————
                Side Effects:
                §7None§r"""
        );
        provider.add("screen.anvilcraft.cfa.megastructure.magnetar_coil.usage",
            """
                Requires:
                §7None§r
                ———————
                Outputs:
                §a[Power Grid]§r
                §fGenerates power based on neutron star magnetic field strength and rotation speed§r
                ———————
                Side Effects:
                §7None§r"""
        );
        provider.add("screen.anvilcraft.cfa.megastructure.penrose_sphere.usage",
            """
                Requires:
                §c[Lsr Int]§r
                §cLaser Lv.1+ §r§f(cannot be on the side center of the CFA)§r
                ———————
                Outputs:
                §c[Lsr Int]§r
                §dGamma Laser Lv.= §r§f(output interface is opposite the input on the same side)§r
                ———————
                Side Effects:
                §7None§r"""
        );
        provider.add("screen.anvilcraft.cfa.megastructure.matter_decompressor.usage",
            """
                Requires:
                §c[Lsr Int]§r
                §dGamma Laser Lv.1+ §r§f(each level provides higher work efficiency)§r
                ———————
                Outputs:
                §e[Log Int]§r
                §fOutputs corresponding matter based on celestial body type§r
                ———————
                Side Effects:
                §7None§r"""
        );
        provider.add("screen.anvilcraft.cfa.megastructure.stellar_evolution_accelerator.usage",
            """
                Requires:
                §7None§r
                ———————
                Outputs:
                §a[Power Grid]§r
                §fDyson Sphere generates infinite power during acceleration§r
                ———————
                Side Effects:
                §fThe star becomes a white dwarf, or undergoes a supernova producing a neutron star or black hole§r"""
        );
        provider.add("screen.anvilcraft.cfa.megastructure.wormhole_stabilizer.usage",
            """
                Requires:
                §fIdentical Black Holes§r
                ———————
                Outputs:
                §fEstablish Wormhole Connections§r
                ———————
                Side Effects:
                §7None§r"""
        );

        // Planet resource bar
        provider.add("screen.anvilcraft.cfa.resource_title", "— Celestial Resources —"); // — 天体资源 —

        // Seed slot tooltip
        provider.add("screen.anvilcraft.cfa.seed_slot.title", "Seed Item"); // 种子物品
        provider.add("screen.anvilcraft.cfa.seed_slot.description", "Consume all items placed in. If specific item has been consumed, a hidden celestial body will be discovered");

        // Special celestial body type names
        provider.add("screen.anvilcraft.cfa.class.special.overworld_like", "Overworld Like"); // 类主世界
        provider.add("screen.anvilcraft.cfa.class.special.flesh_planet", "Flesh Planet"); // 血肉星球
        provider.add("screen.anvilcraft.cfa.class.special.intelligent_planet", "Intelligent Planet"); // 智慧星球
        provider.add("screen.anvilcraft.cfa.class.special.shattered_planet", "Shattered Planet"); // 破碎星球
        provider.add("screen.anvilcraft.cfa.class.special.hollow_planet", "Hollow Planet"); // 中空星球
        provider.add("screen.anvilcraft.cfa.class.special.error_planet", "Error Planet"); // 错误星球

        // Stellar remnant type names
        provider.add("screen.anvilcraft.cfa.class.neutron_star", "Neutron Star"); // 中子星
        provider.add("screen.anvilcraft.cfa.class.black_hole", "Black Hole"); // 黑洞

        // Stellar evolution accelerator stages
        provider.add("screen.anvilcraft.cfa.evolution.stage1", "Main Sequence"); // 主序星阶段
        provider.add("screen.anvilcraft.cfa.evolution.stage2", "Giant Phase"); // 巨星阶段
        provider.add("screen.anvilcraft.cfa.evolution.stage3", "Supernova"); // 超新星爆发
        provider.add("screen.anvilcraft.cfa.evolution.stage4", "M-Dwarf"); // 红矮星阶段
        provider.add("screen.anvilcraft.cfa.evolution.stage_unknown", "Unknown Stage"); // ？？？
        provider.add("screen.anvilcraft.cfa.evolution.time_remaining", "Time: %s"); // 剩余时间:
        provider.add("screen.anvilcraft.cfa.evolution.infinite_power", "Infinite Power"); // 无限发电中
        provider.add("screen.anvilcraft.cfa.evolution_cannot_unlock", "The star is currently evolving and cannot be unlocked"); // 天体正在演化，无法解锁

        // Portal placement messages
        provider.add("message.anvilcraft.portal.invalid_placement", "Portals can only be placed on CFA side centers"); // 传送门只能放置在锻星砧侧面的中心位置
    }
}
