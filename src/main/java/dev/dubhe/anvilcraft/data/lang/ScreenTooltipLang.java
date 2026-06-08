package dev.dubhe.anvilcraft.data.lang;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumLangProvider;

/**
 * 物品栏上方的提示的翻译键
 */
public class ScreenTooltipLang {
    @SuppressWarnings("checkstyle:LineLength")
    public static void init(RegistrumLangProvider provider) {
        provider.add("screen.anvilcraft.tooltip.cfa_interface", "It must be placed tightly against the side of the Celestial Forging Anvil bottom");
        provider.add("screen.anvilcraft.tooltip.cfa_amplifier", "It must be placed diagonally on the Celestial Forging Anvil");

        provider.add("screen.anvilcraft.tooltip.trading_station.break_failed", "Please do not break someone else's Trading Station! Hold Shift to forcibly break it");
    }
}
