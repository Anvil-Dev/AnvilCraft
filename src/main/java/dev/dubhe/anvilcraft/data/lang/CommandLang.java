package dev.dubhe.anvilcraft.data.lang;

import com.tterrag.registrate.providers.RegistrateLangProvider;

public class CommandLang {
    public static void init(RegistrateLangProvider provider) {
        provider.add("command.anvilcraft.powergrid.info.total_generate", "Total power generate: %s");
        provider.add("command.anvilcraft.powergrid.info.total_consume", "Total power consume: %s");
        provider.add("command.anvilcraft.powergrid.info.components", "Components of power grid:");
        provider.add("command.anvilcraft.powergrid.info.producer", "%s at %s, %s, %s (Power generate: %s, Range: %s)");
        provider.add("command.anvilcraft.powergrid.info.consumer", "%s at %s, %s, %s (Power consume: %s, Range: %s)");
        provider.add("command.anvilcraft.powergrid.info.dynamic_consumer", "%s at %s, %s, %s (Power consume: %s)");
        provider.add("command.anvilcraft.powergrid.info.transmitter", "%s at %s, %s, %s (Range: %s)");
        provider.add("command.anvilcraft.powergrid.info.not_found", "No power grid found at position %s, %s, %s");
    }
}
