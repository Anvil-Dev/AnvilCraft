package dev.dubhe.anvilcraft.data.lang;

import com.tterrag.registrate.providers.RegistrateLangProvider;
import org.jetbrains.annotations.NotNull;

public class OtherLang {
    /**
     * @param provider 提供器
     */
    public static void init(@NotNull RegistrateLangProvider provider) {
        provider.add("modmenu.nameTranslation.anvilcraft", "AnvilCraft");

        provider.add("item.anvilcraft.inherent_enchantment.tooltip", "Inherent enchantments:");
        provider.add(
            "item.anvilcraft.amethyst_pickaxe.tooltip", "Stone pickaxe quality, can mine iron ore, not diamonds!");
        provider.add("item.anvilcraft.fire_reforging.tooltip", "Reforging: mending in fire or lava");
        provider.add("item.anvilcraft.comrade_amulet.tooltip", "Signed players:");

        provider.add("item.anvilcraft.geode.find", "Suspected amethyst geode, located %s");
        provider.add("item.anvilcraft.disk.stored_from", "Stored from: %s");

        provider.add("entity.minecraft.villager.anvilcraft.jeweler", "Jeweler");

        provider.add("pack.anvilcraft.builtin_pack", "AnvilCraft Builtin ResourcePack");
        provider.add("pack.anvilcraft.transparent_cauldron.description", "Transparent Cauldron");

        provider.add("message.anvilcraft.copied_to_clipboard", "Copied to clipboard");
        provider.add("message.anvilcraft.code_gen_filed", "Code generation failed");
        provider.add(
            "message.anvilcraft.code_gen_check",
            "Please check if the selected area is a cube and if the output slot has items.");
        provider.add("message.anvilcraft.no_file_selected", "No file path selected");
        provider.add("message.anvilcraft.file_save_failed", "An issue occurred while saving file %s, %s");
        provider.add("message.anvilcraft.file_saved", "File saved to %s");
        provider.add("message.anvilcraft.disk.data_applied", "Applied setting stored in disk to block");
        provider.add("message.anvilcraft.disk.data_incompatible", "This block is incompatible with data stored in disk");
        provider.add("message.anvilcraft.disk.data_cleared", "Cleared data stored in disk");
        provider.add("message.anvilcraft.disk.data_stored", "Stored setting of block into disk");

        provider.add("enchantment.anvilcraft.beheading", "Beheading");
        provider.add("enchantment.anvilcraft.felling", "Felling");
        provider.add("enchantment.anvilcraft.harvest", "Harvest");
        provider.add("death.attack.anvilcraft.laser", "%1$s was pierced by laser");
        provider.add("death.attack.anvilcraft.lost_in_time", "%1$s was lost in the river of time");
    }
}
