package dev.dubhe.anvilcraft.data.lang;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumLangProvider;
import dev.dubhe.anvilcraft.api.tooltip.ItemTooltipManager;

public class ItemTooltipLang {
    @SuppressWarnings("checkstyle:LineLength")
    public static void init(RegistrumLangProvider provider) {
        ItemTooltipManager.NEED_TOOLTIP_ITEMS.forEach(
            (item, s) -> provider.add(ItemTooltipManager.getTranslationKey(item), s));
        provider.add("tooltip.anvilcraft.item.reinforced_concrete", "Creeper proof");
        provider.add("tooltip.anvilcraft.item.recipe.processing.chance", "%1$s%% Chance");

        provider.add("tooltip.anvilcraft.item.structure_tool.line_1", "Developer tool");
        provider.add("tooltip.anvilcraft.item.structure_tool.line_2", "Right click to select an area for multiblock recipe");
        provider.add("tooltip.anvilcraft.item.structure_tool.line_3", "Blocks in the area will be the input of recipe");
        provider.add("tooltip.anvilcraft.item.structure_tool.min_pos", "Min: x: %1$d, y: %2$d, z: %3$d");
        provider.add("tooltip.anvilcraft.item.structure_tool.max_pos", "Max: x: %1$d, y: %2$d, z: %3$d");
        provider.add("tooltip.anvilcraft.item.structure_tool.size", "Size: x: %1$d, y: %2$d, z: %3$d");
        provider.add(
            "tooltip.anvilcraft.item.structure_tool.shift_to_clear",
            "Shift + right click to clear selected area"
        );
        provider.add("tooltip.anvilcraft.item.structure_tool.data_removed", "Cleared selected area");
        provider.add("tooltip.anvilcraft.item.structure_tool.must_cube", "The selected area must be a cube");
        provider.add(
            "tooltip.anvilcraft.item.structure_tool.must_odd",
            "The side length of the selected area must be odd and cannot exceed 15"
        );
        provider.add(
            "tooltip.anvilcraft.item.structure_tool.inconsistent_size",
            "The size of input pattern must be same as output pattern"
        );
        provider.add("tooltip.anvilcraft.item.structure_tool.click_to_copy", "Click to copy");
        provider.add("tooltip.anvilcraft.item.disk.store", "Right click block to copy its setting");
        provider.add("tooltip.anvilcraft.item.disk.clear", "Shift + Right click to clear data stored");
        provider.add("tooltip.anvilcraft.heliostats.adjacent_heliostats", "Adjacent heliostats detected.");

        provider.add("item.anvilcraft.ionocraft_backpack.flight_time", "Flight Time: %ds");

        provider.add(
            "tooltip.anvilcraft.item.amulet_box.line_1",
            "Right click to store the Totems of Undying on your inventory, and shift-right-click to retrieve the totems;"
        );
        provider.add(
            "tooltip.anvilcraft.item.amulet_box.line_2",
            "When holding, consume the totems in the box when needed, and after consuming the totem, you may receive a secret gift."
        );
        provider.add("tooltip.anvilcraft.item.amulet_box.fullness", "Fullness: %1$d / %2$d");

        provider.add("tooltip.anvilcraft.press_key", "Hold [%s] for information");

        provider.add(
            "tooltip.anvilcraft.pill_box",
            "Store pills, right-click to take one pill each, and press [%s] to use them in the inventory"
        );

        provider.add("item.anvilcraft.ionocraft_backpack.flight_time_energy", "Remaining Energy: %sMJ, Flight Time: %ss");

        provider.add("tooltip.anvilcraft.resonator.desc", "Press [%s] to switch modes. Auto mode supports all tools");
        provider.add("tooltip.anvilcraft.resonator.mining_desc", "Press [%s] to change modes. Auto mode supports all tools and can “resonance‑mine” most blocks when holding right‑click");

        provider.add("tooltip.anvilcraft.thought", "Press [%s] for more info");
        provider.add("tooltip.anvilcraft.item.amulet_box.desc", "Stores multiple active amulets or totems");
        
        // Structure Disk
        provider.add("item.anvilcraft.structure_disk.structure", "Structure: %s");
        provider.add("item.anvilcraft.structure_disk.size", "Size: %s");
    }
}
