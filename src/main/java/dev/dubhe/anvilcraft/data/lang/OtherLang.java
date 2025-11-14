package dev.dubhe.anvilcraft.data.lang;

import com.tterrag.registrate.providers.RegistrateLangProvider;

public class OtherLang {
    public static void init(RegistrateLangProvider provider) {
        provider.add("item.anvilcraft.inherent_enchantment.tooltip", "Inherent enchantments:");
        provider.add(
            "item.anvilcraft.amethyst_pickaxe.tooltip", "Stone pickaxe quality, can mine iron ore, not diamonds!");
        provider.add("item.anvilcraft.comrade_amulet.tooltip", "Signed players:");

        provider.add("item.anvilcraft.geode.find", "Suspected amethyst geode, located %s");
        provider.add("item.anvilcraft.disk.stored_from", "Stored from: %s");

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
        provider.add("message.anvilcraft.chute.cannot_place", "Chute cannot face this direction");

        provider.add("enchantment.anvilcraft.beheading", "Beheading");
        provider.add("enchantment.anvilcraft.felling", "Felling");
        provider.add("enchantment.anvilcraft.harvest", "Harvest");
        provider.add("enchantment.anvilcraft.smelting", "Smelting");
        provider.add("death.attack.anvilcraft.laser", "%1$s was pierced by laser");
        provider.add("death.attack.anvilcraft.lost_in_time", "%1$s was lost in the river of time");

        provider.add("key.categories.anvilcraft", "AnvilCraft");
        provider.add("key.anvilcraft.switch_phase", "Switch Phase");
        provider.add("key.anvilcraft.toggle_goggle", "Toggle Goggle Mode");
        provider.add("key.anvilcraft.switch_resonate_mode", "Switch Resonate Mode");

        provider.add("effect.anvilcraft.rage", "Rage");
        provider.add("effect.anvilcraft.invulnerable", "Invulnerable");

        provider.add(
            "item.anvilcraft.pill.tooltip",
            "Pills made together with potion to achieve corresponding effects, can be taken quickly"
        );
        provider.add("item.anvilcraft.pill.effect.empty", "Ineffective Pill");
        provider.add("item.anvilcraft.pill.effect.fire_resistance", "Pill of Fire Resistance");
        provider.add("item.anvilcraft.pill.effect.harming", "Pill of Harming");
        provider.add("item.anvilcraft.pill.effect.healing", "Pill of Healing");
        provider.add("item.anvilcraft.pill.effect.infested", "Pill of Infested");
        provider.add("item.anvilcraft.pill.effect.invisibility", "Pill of Invisibility");
        provider.add("item.anvilcraft.pill.effect.leaping", "Pill of Leaping");
        provider.add("item.anvilcraft.pill.effect.levitation", "Pill of Levitation");
        provider.add("item.anvilcraft.pill.effect.luck", "Pill of Luck");
        provider.add("item.anvilcraft.pill.effect.mundane", "Pill of Mundane");
        provider.add("item.anvilcraft.pill.effect.night_vision", "Pill of Night Vision");
        provider.add("item.anvilcraft.pill.effect.oozing", "Pill of Oozing");
        provider.add("item.anvilcraft.pill.effect.poison", "Pill of Poison");
        provider.add("item.anvilcraft.pill.effect.regeneration", "Pill of Regeneration");
        provider.add("item.anvilcraft.pill.effect.slow_falling", "Pill of Slow Falling");
        provider.add("item.anvilcraft.pill.effect.slowness", "Pill of Slowness");
        provider.add("item.anvilcraft.pill.effect.strength", "Pill of Strength");
        provider.add("item.anvilcraft.pill.effect.swiftness", "Pill of Swiftness");
        provider.add("item.anvilcraft.pill.effect.thick", "Pill of Thick");
        provider.add("item.anvilcraft.pill.effect.turtle_master", "Pill of Turtle Master");
        provider.add("item.anvilcraft.pill.effect.water_breathing", "Pill of Water Breathing");
        provider.add("item.anvilcraft.pill.effect.weakness", "Pill of Weakness");
        provider.add("item.anvilcraft.pill.effect.weaving", "Pill of Weaving");
        provider.add("item.anvilcraft.pill.effect.wind_charged", "Pill of Wind Charged");
    }
}
