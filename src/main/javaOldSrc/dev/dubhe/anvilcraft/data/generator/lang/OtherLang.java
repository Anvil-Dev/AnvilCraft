package dev.dubhe.anvilcraft.data.lang;

import com.tterrag.registrate.providers.RegistrateLangProvider;
import org.jetbrains.annotations.NotNull;

public class OtherLang {
    /**
     * @param provider 提供器
     */
    public static void init(@NotNull RegistrateLangProvider provider) {
        provider.add(
            "modmenu.nameTranslation.anvilcraft",
            "AnvilCraft"
        );

        provider.add(
            "item.anvilcraft.default_enchantment.tooltip",
            "Has default enchantments:"
        );

        provider.add(
            "item.anvilcraft.amethyst_pickaxe.tooltip",
            "Stone pickaxe quality, can mine iron ore, not diamonds!"
        );

        provider.add(
            "item.anvilcraft.geode.find",
            "Suspected amethyst geode, located %s"
        );

        provider.add(
            "entity.minecraft.villager.anvilcraft.jeweler",
            "Jeweler"
        );

        provider.add(
            "entity.minecraft.villager.jeweler",
            "Jeweler"
        );

        provider.add(
            "item.anvilcraft.disk.stored_from",
            "Stored from: %s"
        );

        provider.add(
            "tooltip.anvilcraft.only_jei",
            "We have detected that you have only installed JEI and will not be able to obtain a complete recipe query. "
                + "We recommend installing %s for a complete gaming experience!"
        );

        provider.add(
            "pack.anvilcraft.transparent_cauldron.description",
            "Transparent Cauldron"
        );
    }
}
