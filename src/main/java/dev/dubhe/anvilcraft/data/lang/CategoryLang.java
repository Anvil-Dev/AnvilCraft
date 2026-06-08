package dev.dubhe.anvilcraft.data.lang;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumLangProvider;

public class CategoryLang {
    public static void init(RegistrumLangProvider provider) {
        provider.add("category.anvilcraft.foods_and_drinks", "Foods and Drinks");
        provider.add("category.anvilcraft.namespace", "%s Items");
        provider.add("category.anvilcraft.namespace.anvilcraft", "AnvilCraft");
        provider.add("category.anvilcraft.unknown_namespace", "Unknown <%s>");
        provider.add("category.anvilcraft.redstone", "Redstone Items");
        provider.add("category.anvilcraft.enchanted", "Enchanted Items");
    }
}
