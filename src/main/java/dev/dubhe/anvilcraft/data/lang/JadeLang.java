package dev.dubhe.anvilcraft.data.lang;

import com.tterrag.registrate.providers.RegistrateLangProvider;

public class JadeLang {
    /**
     * @param provider 提供器
     */
    public static void init(RegistrateLangProvider provider) {
        provider.add("config.jade.plugin_anvilcraft.power_provider", "Anvil Craft Power");
        provider.add("config.jade.plugin_anvilcraft.ruby_prism", "Ruby Prism");
        provider.add("config.jade.plugin_anvilcraft.item_detector", "Item Detector");
        provider.add("config.jade.plugin_anvilcraft.space_overcompressor", "Space Overcompressor");

        provider.add("tooltip.anvilcraft.jade.power_information", "Power Grid: %s");
        provider.add("tooltip.anvilcraft.jade.ruby_prism.power", "Laser level: %d");
        provider.add("tooltip.anvilcraft.jade.item_detector", "Detection Range: %d");
    }
}
