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
        provider.add("screen.anvilcraft.smithing_template.royal_steel_upgrade_smithing_template"
            + ".additions_slot_description", "Put the Royal Steel Ingot");
        provider.add("screen.anvilcraft.smithing_template.royal_steel_upgrade_smithing_template"
            + ".applies_to", "Amethyst Pickaxe Golden Pickaxe Iron Pickaxe Diamond Pickaxe");
        provider.add("screen.anvilcraft.smithing_template.royal_steel_upgrade_smithing_template"
            + ".base_slot_description", "Put the pickaxe");
    }
}
