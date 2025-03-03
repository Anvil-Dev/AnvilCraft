package dev.dubhe.anvilcraft.data.lang;

import com.tterrag.registrate.providers.RegistrateLangProvider;
import org.jetbrains.annotations.NotNull;

public class AdvancementLang {
    /**
     * @param provider 提供器
     */
    public static void init(@NotNull RegistrateLangProvider provider) {
        provider.add("advancements.anvilcraft.root.title", "Welcome to AnvilCraft");
        provider.add(
            "advancements.anvilcraft.root.description",
            "Pick up the anvil, start from the vanilla, and enter the world of technology and magic");
        provider.add("advancements.anvilcraft.crab_claw.title", "Win half of the resurrection race");
        provider.add("advancements.anvilcraft.crab_claw.description", "Obtain crab claws from the crab trap");
    }
}
