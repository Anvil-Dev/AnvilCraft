package dev.dubhe.anvilcraft.data.lang;

import com.tterrag.registrate.providers.RegistrateLangProvider;

public class CommandLang {
    public static void init(RegistrateLangProvider provider) {
        provider.add("command.anvilcraft.powergrid.info.total_generate", "Total power generate: %s");
        provider.add("command.anvilcraft.powergrid.info.total_consume", "Total power consume: %s");
        provider.add("command.anvilcraft.powergrid.info.components", "Components of power grid:");
        provider.add("command.anvilcraft.powergrid.info.producer", "%1$s at %2$s, %3$s, %4$s (Power generate: %5$s, Range: %6$s)");
        provider.add("command.anvilcraft.powergrid.info.consumer", "%1$s at %2$s, %3$s, %4$s (Power consume: %5$s, Range: %6$s)");
        provider.add("command.anvilcraft.powergrid.info.dynamic_consumer", "%1$s at %2$s, %3$s, %4$s (Power consume: %5$s)");
        provider.add("command.anvilcraft.powergrid.info.transmitter", "%1$s at %2$s, %3$s, %4$s (Range: %5$s)");
        provider.add("command.anvilcraft.powergrid.info.not_found", "No power grid found at position %1$s, %2$s, %3$s");
    }
}
