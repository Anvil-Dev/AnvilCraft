package dev.dubhe.anvilcraft.data.lang;

import com.tterrag.registrate.providers.RegistrateLangProvider;
import org.jetbrains.annotations.NotNull;

public class OtherLang {
    /**
     * @param provider 提供器
     */
    public static void init(@NotNull RegistrateLangProvider provider) {
        provider.add("modmenu.nameTranslation.anvilcraft", "AnvilCraft");

        provider.add("item.anvilcraft.default_enchantment.tooltip", "Has default enchantments:");
        provider.add(
                "item.anvilcraft.amethyst_pickaxe.tooltip", "Stone pickaxe quality, can mine iron ore, not diamonds!");

        provider.add("item.anvilcraft.geode.find", "Suspected amethyst geode, located %s");
        provider.add("item.anvilcraft.disk.stored_from", "Stored from: %s");

        provider.add("entity.minecraft.villager.anvilcraft.jeweler", "Jeweler");

        provider.add("pack.anvilcraft.builtin_pack", "AnvilCraft Builtin ResourcePack");
        provider.add("pack.anvilcraft.transparent_cauldron.description", "Transparent Cauldron");
    }
}
