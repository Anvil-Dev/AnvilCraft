package dev.dubhe.anvilcraft.data.generator.lang;

import com.tterrag.registrate.providers.RegistrateLangProvider;
import org.jetbrains.annotations.NotNull;

public class OtherLang {
    /**
     * @param provider 提供器
     */
    public static void init(@NotNull RegistrateLangProvider provider) {
        provider.add("item.anvilcraft.default_enchantment.tooltip", "Has default enchantments:");

        provider.add(
                "item.anvilcraft.amethyst_pickaxe.tooltip",
                "Stone pickaxe quality, can mine iron ore, not diamonds!");

        provider.add("item.anvilcraft.geode.find", "Suspected amethyst geode, located %s");

        provider.add("entity.minecraft.villager.anvilcraft.jeweler", "Jeweler");

        provider.add("entity.minecraft.villager.jeweler", "Jeweler");

        provider.add("item.anvilcraft.disk.stored_from", "Stored from: %s");
    }
}
