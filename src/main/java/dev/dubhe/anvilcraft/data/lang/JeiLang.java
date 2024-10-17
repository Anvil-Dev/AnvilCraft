package dev.dubhe.anvilcraft.data.lang;

import com.tterrag.registrate.providers.RegistrateLangProvider;

public class JeiLang {
    public static void init(RegistrateLangProvider provider) {
        provider.add("gui.anvilcraft.category.chance", "Chance: %s%%");
        provider.add("gui.anvilcraft.category.average_output", "Average: %s");
        provider.add("gui.anvilcraft.category.min_output", "Min: %s");
        provider.add("gui.anvilcraft.category.max_output", "Max: %s");

        provider.add("gui.anvilcraft.category.mesh", "Mesh");

        provider.add("gui.anvilcraft.category.block_compress", "Block Compress");
        provider.add("gui.anvilcraft.category.block_crush", "Block Crush");

        provider.add("gui.anvilcraft.category.item_compress", "Item Compress");
        provider.add("gui.anvilcraft.category.item_crush", "Item Crush");

        provider.add("gui.anvilcraft.category.cooking", "Cooking");
        provider.add("gui.anvilcraft.category.boiling", "Boiling");

        provider.add("gui.anvilcraft.category.stamping", "Stamping");

        provider.add("gui.anvilcraft.category.super_heating", "Super Heating");
        provider.add("gui.anvilcraft.category.super_heating.convert_to", "Convert to %s");

        provider.add("gui.anvilcraft.category.squeezing", "Squeezing");

        provider.add("gui.anvilcraft.category.item_inject", "Item Inject");

        provider.add("gui.anvilcraft.category.cement_staining", "Cement Staining");

        provider.add("gui.anvilcraft.category.concrete", "Concrete");

        provider.add("gui.anvilcraft.category.bulging", "Bulging");
        provider.add("gui.anvilcraft.category.bulging.consume_fluid", "Consume: %s");
        provider.add("gui.anvilcraft.category.bulging.produce_fluid", "Produce: %s");

        provider.add("gui.anvilcraft.category.time_warp", "Time Warp");
        provider.add("gui.anvilcraft.category.time_warp.consume_fluid", "Consume: %s");
        provider.add("gui.anvilcraft.category.time_warp.produce_fluid", "Produce: %s");
        provider.add("gui.anvilcraft.category.time_warp.need_activated", "Need Activated");

        provider.add("gui.anvilcraft.category.multiblock", "Multiblock Crafting");
        provider.add("gui.anvilcraft.category.multiblock.all_layers", "All Layers Visible");
        provider.add("gui.anvilcraft.category.multiblock.single_layer", "Visible Layer: %d of %d");

        provider.add("gui.anvilcraft.category.jewel_crafting", "Jewel Crafting");

        provider.add("jei.anvilcraft.info.geode_1", "Finds Amethyst Geodes nearby when using.");
        provider.add("jei.anvilcraft.info.geode_2", "Dropped by Budding Amethyst blocks.");
        provider.add("jei.anvilcraft.info.geode_3", "You can also find it in the Bonus Chest");
        provider.add("jei.anvilcraft.info.geode_4", "Or trade it from a Jeweler Villager");

        provider.add("jei.anvilcraft.info.royal_steel_upgrade_smithing_template_1", "You can find it in the chest of the village weapons smith.");
        provider.add("jei.anvilcraft.info.royal_steel_upgrade_smithing_template_2", "Or you can trade with the jeweler.");

        provider.add("jei.anvilcraft.info.craw_claw", "You can obtain this from crab traps placed in the water.");

        provider.add("jei.anvilcraft.info.capacitor", "You can charge the empty capacitor in the charger to obtain it.");
    }
}
