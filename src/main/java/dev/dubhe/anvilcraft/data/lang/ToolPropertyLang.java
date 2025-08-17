package dev.dubhe.anvilcraft.data.lang;

import com.tterrag.registrate.providers.RegistrateLangProvider;
import org.jetbrains.annotations.NotNull;

public class ToolPropertyLang {
    /**
     * @param provider 提供器
     */
    public static void init(@NotNull RegistrateLangProvider provider) {
        provider.add("tooltip.anvilcraft.property.fire_reforging", "Reforging: mending in fire or lava");
        provider.add("tooltip.anvilcraft.property.multiphase", "Multiphase: press [%s] to switch phases or merciless");
        provider.add("tooltip.anvilcraft.property.multiphase.name.0", "α");
        provider.add("tooltip.anvilcraft.property.multiphase.name.1", "β");
        provider.add("tooltip.anvilcraft.property.multiphase.name.2", "γ");
        provider.add("tooltip.anvilcraft.property.multiphase.name.3", "δ");
        provider.add("tooltip.anvilcraft.property.multiphase.suffix.0", "-α");
        provider.add("tooltip.anvilcraft.property.multiphase.suffix.1", "-β");
        provider.add("tooltip.anvilcraft.property.multiphase.suffix.2", "-γ");
        provider.add("tooltip.anvilcraft.property.multiphase.suffix.3", "-δ");
        provider.add("tooltip.anvilcraft.property.merciless", "Merciless: disable all enchantments except specific [Hold %s] "
            + "and convert them into attack damage and mining efficiency");
        provider.add("tooltip.anvilcraft.property.merciless.shifting", "Merciless: disable all enchantments except specific (%s) "
            + "and convert them into attack damage and mining efficiency");
        provider.add("tooltip.anvilcraft.property.merciless.curse", "All curses");
        provider.add(
            "tooltip.anvilcraft.property.eternal",
            "Eternal: unbreakable, immune fire, explode, cactus, even the time and the void");
        provider.add("tooltip.anvilcraft.property.providence", "Providence: has chance to trigger [Hold %s] enchantments multiple times");
        provider.add(
            "tooltip.anvilcraft.property.providence.shifting",
            "Providence: has chance to trigger (%s) enchantments multiple times");
    }
}
