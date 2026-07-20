package dev.dubhe.anvilcraft.data.lang;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumLangProvider;

public class CategoryLang {
    public static void init(RegistrumLangProvider provider) {
        provider.add("category.anvilcraft.block", "Block Items");
        provider.add("category.anvilcraft.unstackable", "Unstackable Items");
        provider.add("category.anvilcraft.food_and_drink", "Foods and Drinks");
        provider.add("category.anvilcraft.namespace", "%s Items"); // 预设的模组命名空间翻译详见 OtherLang
        provider.add("category.anvilcraft.redstone", "Redstone Items");
        provider.add("category.anvilcraft.enchanted", "Enchanted Items");
        provider.add("category.anvilcraft.filter", "New Category");
    }
}
