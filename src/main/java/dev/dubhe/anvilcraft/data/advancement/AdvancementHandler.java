package dev.dubhe.anvilcraft.data.advancement;

import com.tterrag.registrate.providers.RegistrateAdvancementProvider;

public class AdvancementHandler {
    public static void init(RegistrateAdvancementProvider provider) {
        AnvilCraftAdvancement.init(provider);
    }
}
