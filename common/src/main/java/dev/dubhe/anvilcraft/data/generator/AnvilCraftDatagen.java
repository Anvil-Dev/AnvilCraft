package dev.dubhe.anvilcraft.data.generator;

import com.tterrag.registrate.providers.ProviderType;

import static dev.dubhe.anvilcraft.AnvilCraft.REGISTRATE;

public class AnvilCraftDatagen {
    public static void init() {
        REGISTRATE.addDataGenerator(ProviderType.ITEM_TAGS, MyItemTagGenerator::addTags);
    }
}
