package dev.dubhe.anvilcraft.data.lang;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumLangProvider;

/**
 * 仓储分类设置界面的翻译键
 */
public class CategoryLang {
    public static void init(RegistrumLangProvider provider) {
        provider.add("screen.anvilcraft.storage.search.edit", "Search");
        provider.add("screen.anvilcraft.storage.search.tab", "Press Tab");
        provider.add("screen.anvilcraft.storage.search", "Search Mode: Currently %s");
        provider.add("screen.anvilcraft.storage.search.clear", "Clear");
        provider.add("screen.anvilcraft.storage.search.retention", "Retention");
        provider.add("screen.anvilcraft.storage.sort", "Sort Mode: Currently %s");
        provider.add("screen.anvilcraft.storage.sort.count", "by Count");
        provider.add("screen.anvilcraft.storage.sort.mod", "by Mod ID");
        provider.add("screen.anvilcraft.storage.sort.name", "by Display Name");
        provider.add("screen.anvilcraft.storage.order", "Sort Order: Currently %s");
        provider.add("screen.anvilcraft.storage.order.sequential", "Sequential");
        provider.add("screen.anvilcraft.storage.order.reverse", "Reverse");
        provider.add("screen.anvilcraft.storage.nbt", "Should Fold Items with Diff. NBT: Currently %s");
        provider.add("screen.anvilcraft.storage.nbt.unfold", "False");
        provider.add("screen.anvilcraft.storage.nbt.fold", "True");
        provider.add("screen.anvilcraft.storage.capacity.space", "Total Capacity: %1$s/%2$s");
        provider.add("screen.anvilcraft.storage.capacity.types", "Types: %1$s/%2$s");
        provider.add("screen.anvilcraft.storage.capacity.infinity", "Infinity Storage");
        provider.add("screen.anvilcraft.storage.count", "Count: %s");
        provider.add("screen.anvilcraft.storage.category.name", "Name: %s");
        provider.add("screen.anvilcraft.storage.category.mode", "Mode: %s");
        provider.add("screen.anvilcraft.storage.category.mode.unlimited", "Unlimited");
        provider.add("screen.anvilcraft.storage.category.mode.allowlist", "Allow Display");
        provider.add("screen.anvilcraft.storage.category.mode.blocklist", "Block Display");
        provider.add("screen.anvilcraft.storage.category.tooltip", "Left click to move to alternates, right click to pin to top");
        provider.add("screen.anvilcraft.storage.category.add", "Left click when selecting Filter to add custom category");
        provider.add("screen.anvilcraft.storage.category.alternate.removable", "Left click to list, right click to delete this category");
        provider.add("screen.anvilcraft.storage.category.alternate.unremovable", "Left click to list");
        provider.add("screen.anvilcraft.storage.category.setting.title", "Category Setting");

        provider.add("screen.anvilcraft.balance_mode.smart", "Smart");
        provider.add("screen.anvilcraft.balance_mode.restock", "Restock only");
        provider.add("screen.anvilcraft.balance_mode.deposit", "Deposit only");
        provider.add("screen.anvilcraft.balance_mode.off", "Off");

        provider.add("category.anvilcraft.block", "Block Items");
        provider.add("category.anvilcraft.unstackable", "Unstackable Items");
        provider.add("category.anvilcraft.food_and_drink", "Foods and Drinks");
        provider.add("category.anvilcraft.namespace", "%s Items");
        provider.add("category.anvilcraft.redstone", "Redstone Items");
        provider.add("category.anvilcraft.enchanted", "Enchanted Items");
        provider.add("category.anvilcraft.filter", "New Category");
    }
}
