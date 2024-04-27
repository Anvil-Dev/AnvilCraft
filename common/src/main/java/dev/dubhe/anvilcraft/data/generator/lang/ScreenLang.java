package dev.dubhe.anvilcraft.data.generator.lang;

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
        provider.add("screen.anvilcraft.royal_grindstone.remove_curse_number", "Remove %i curse number");
        provider.add("screen.anvilcraft.royal_grindstone.remove_repair_cost", "Remove %i repair cost");
        provider.add("screen.anvilcraft.royal_grindstone.title", "Remove curse and repair cost");
        provider.add("screen.anvilcraft.royal_steel_upgrade_smithing_template",
                "Royal Steel Upgrade Smithing Template");
        provider.add(
                "screen.anvilcraft.smithing_template.royal_steel_upgrade_smithing_template"
                        + ".additions_slot_description",
                "Put the Royal Steel Ingot or Royal Steel Block"
        );
        provider.add(
                "screen.anvilcraft.smithing_template.royal_steel_upgrade_smithing_template"
                        + ".applies_to",
                "Anvil Hammer, Amethyst Pickaxe, Golden Pickaxe, Iron Pickaxe, Diamond Pickaxe"
        );
        provider.add(
                "screen.anvilcraft.smithing_template.royal_steel_upgrade_smithing_template"
                        + ".base_slot_description",
                "Put "
        );
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
    }
}
