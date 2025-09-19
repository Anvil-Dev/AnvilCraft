package dev.dubhe.anvilcraft.data.lang;

import com.tterrag.registrate.providers.RegistrateLangProvider;

public class CategoryLang {
    public static void init(RegistrateLangProvider provider) {
        provider.add("category.anvilcraft.mod_name.minecraft", "Minecraft");
        provider.add("category.anvilcraft.mod_name.anvilcraft", "AnvilCraft");

        // Integrations
        provider.add("category.anvilcraft.mod_name.ae2", "Applied Energistics 2");
        provider.add("category.anvilcraft.mod_name.create", "Create");
        provider.add("category.anvilcraft.mod_name.twilightforest", "Twilight Forest");

        provider.add("category.anvilcraft.mod_name_suffix", " Items");

        provider.add("category.anvilcraft.block", "Blocks");
        provider.add("category.anvilcraft.unstackable", "Unstackable Items");
        provider.add("category.anvilcraft.food", "Food and Drinks");
        provider.add("category.anvilcraft.redstone", "Redstone Items");
        provider.add("category.anvilcraft.enchanted", "Enchanted Items");
        provider.add("category.anvilcraft.new", "New Category");
    }
}
