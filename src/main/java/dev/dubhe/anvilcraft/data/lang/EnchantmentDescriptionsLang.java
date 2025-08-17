package dev.dubhe.anvilcraft.data.lang;

import com.tterrag.registrate.providers.RegistrateLangProvider;
import org.jetbrains.annotations.NotNull;

public class EnchantmentDescriptionsLang {
    /**
     * @param provider 提供器
     */
    public static void init(@NotNull RegistrateLangProvider provider) {
        provider.add("enchantment.anvilcraft.felling.desc",
            "Vein mining of logs and their variants, higher level increases number limit.");
        provider.add("enchantment.anvilcraft.harvest.desc",
            "Harvest and replant mature crops, higher level increases range.");
        provider.add("enchantment.anvilcraft.beheading.desc",
            "Increase drop chance of Wither Skeleton Skull and make other mobs drop their head.");
        provider.add("enchantment.anvilcraft.smelting.desc",
            "Smelts block drop (after Silk Touch and Fortune), higher levels provide chances of doubling ores outcome");
    }
}
