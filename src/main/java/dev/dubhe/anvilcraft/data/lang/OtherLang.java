package dev.dubhe.anvilcraft.data.lang;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumLangProvider;

public class OtherLang {
    public static void init(RegistrumLangProvider provider) {
        provider.add("item.anvilcraft.inherent_enchantment.tooltip", "Inherent enchantments:");
        provider.add(
            "item.anvilcraft.amethyst_pickaxe.tooltip", "Stone pickaxe quality, can mine iron ore, not diamonds!");
        provider.add("item.anvilcraft.comrade_amulet.tooltip", "Signed players:");

        provider.add("item.anvilcraft.geode.find", "Suspected amethyst geode, located %s");
        provider.add("item.anvilcraft.disk.stored_from", "Stored from: %s");
        provider.add("item.anvilcraft.spectral_slingshot.unload_return", "Returned when Unloaded");
        provider.add("item.anvilcraft.spectral_slingshot.unload_vanish", "Vanishes when Unloaded");

        provider.add("entity.minecraft.villager.anvilcraft.jeweler", "Jeweler");

        provider.add("pack.anvilcraft.builtin_pack", "AnvilCraft Builtin ResourcePack");
        provider.add("pack.anvilcraft.builtin_data_pack", "AnvilCraft Builtin DataPack");
        provider.add("pack.anvilcraft.transparent_cauldron.description", "Transparent Cauldron");
        provider.add("pack.anvilcraft.first_ancient_debris.description", "First Ancient Debris");

        provider.add("message.anvilcraft.copied_to_clipboard", "Copied to clipboard");
        provider.add("message.anvilcraft.code_gen_filed", "Code generation failed");
        provider.add(
            "message.anvilcraft.code_gen_check",
            "Please check if the selected area is a cube and if the output slot has items.");
        provider.add("message.anvilcraft.no_file_selected", "No file path selected");
        provider.add("message.anvilcraft.file_save_failed", "An issue occurred while saving file %1$s, %2$s");
        provider.add("message.anvilcraft.file_saved", "File saved to %s");
        provider.add("message.anvilcraft.disk.data_applied", "Applied setting stored in disk to block");
        provider.add("message.anvilcraft.disk.data_incompatible", "This block is incompatible with data stored in disk");
        provider.add("message.anvilcraft.disk.data_cleared", "Cleared data stored in disk");
        provider.add("message.anvilcraft.disk.data_stored", "Stored setting of block into disk");
        provider.add("message.anvilcraft.disk.extreme_body_requires_crystal",
            "Extreme celestial bodies can only be stored in Singularity Crystal"); // 极端天体的数据只能被存储在奇点晶体中
        provider.add("message.anvilcraft.chute.cannot_place", "Chute cannot face this direction");
        provider.add("message.anvilcraft.structure_scanner.no_disk", "Please insert a structure disk to save the structure!");
        provider.add("message.anvilcraft.structure_scanner.output_not_empty", "Output slot is not empty, please take the item first!");

        provider.add("enchantment.anvilcraft.beheading", "Beheading");
        provider.add("enchantment.anvilcraft.felling", "Felling");
        provider.add("enchantment.anvilcraft.harvest", "Harvest");
        provider.add("enchantment.anvilcraft.smelting", "Smelting");
        provider.add("enchantment.anvilcraft.disintegration", "Disintegration");
        provider.add("death.attack.anvilcraft.laser", "%1$s was pierced by laser");
        provider.add("death.attack.anvilcraft.gamma_laser", "%1$s want to be Hulk, but he's not Bruce Banner.");
        provider.add("death.attack.anvilcraft.lost_in_time", "%1$s was lost in the river of time");
        provider.add("death.attack.anvilcraft.heater_burn", "%1$s was well done by the heater");
        provider.add("death.attack.anvilcraft.plasma_jets", "%1$s tried to high-five the plasma jets. They did not high-five back.");

        provider.add("effect.anvilcraft.rage", "Rage");

        provider.add("item.anvilcraft.pill.tooltip",
            "Pills made together with potion to achieve corresponding effects, can be taken quickly");

        provider.add("subtitles.anvilcraft.plasma_jet", "Plasma Jet roaring");
        provider.add("subtitles.anvilcraft.plasma_jet_lava", "Plasma Jet burst");
        provider.add("subtitles.anvilcraft.burning_heater", "Burning Heater crackled");
        provider.add("subtitles.anvilcraft.giant_anvil_land", "Giant Anvil landed");
        provider.add("subtitles.anvilcraft.giant_anvil_shock", "Giant Anvil shockwave");
        provider.add("subtitles.anvilcraft.giant_anvil_resin_shock", "Giant Anvil resin shock");
        provider.add("subtitles.anvilcraft.tesla_tower_strike", "Tesla Tower strikes");

        provider.add("subtitles.anvilcraft.smart_block_placer_extend", "Smart Block Placer extended");
        provider.add("subtitles.anvilcraft.smart_block_placer_retract", "Smart Block Placer retracted");
        provider.add("subtitles.anvilcraft.smart_block_placer_shulker_open", "Smart Block Placer whirs");
    }
}
