package dev.dubhe.anvilcraft.data.generator.advancement;

import com.tterrag.registrate.providers.RegistrateAdvancementProvider;

public class AdvancementHandler {
    public static void init(RegistrateAdvancementProvider provider) {
        AnvilCraftAdvancement.init(provider);
    }
}
