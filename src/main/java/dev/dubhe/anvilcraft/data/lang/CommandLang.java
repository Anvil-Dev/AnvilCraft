package dev.dubhe.anvilcraft.data.lang;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumLangProvider;

public class CommandLang {
    public static void init(RegistrumLangProvider provider) {
        provider.add("command.anvilcraft.powergrid.info.total_generate", "Total power generate: %s");
        provider.add("command.anvilcraft.powergrid.info.total_consume", "Total power consume: %s");
        provider.add("command.anvilcraft.powergrid.info.components", "Components of power grid:");
        provider.add("command.anvilcraft.powergrid.info.producer", "%1$s at %2$s, %3$s, %4$s (Power generate: %5$s, Range: %6$s)");
        provider.add("command.anvilcraft.powergrid.info.consumer", "%1$s at %2$s, %3$s, %4$s (Power consume: %5$s, Range: %6$s)");
        provider.add("command.anvilcraft.powergrid.info.dynamic_consumer", "%1$s at %2$s, %3$s, %4$s (Power consume: %5$s)");
        provider.add("command.anvilcraft.powergrid.info.transmitter", "%1$s at %2$s, %3$s, %4$s (Range: %5$s)");
        provider.add("command.anvilcraft.powergrid.info.not_found", "No power grid found at position %1$s, %2$s, %3$s");

        provider.add("command.anvilcraft.multiphase.no_item", "The main-hand item does not have Multiphase");
        provider.add(
            "command.anvilcraft.multiphase.info.summary",
            "Multiphase spaces: %1$s, active: %2$s, Merciless: %3$s"
        );
        provider.add(
            "command.anvilcraft.multiphase.info.phase",
            "[%1$s] %2$s, repair cost: %3$s, enchantments: %4$s"
        );
        provider.add(
            "command.anvilcraft.multiphase.add.success",
            "Added %1$s enchantment space(s); total: %2$s"
        );
        provider.add("command.anvilcraft.multiphase.add.full", "This item already has four enchantment spaces");

        provider.add("command.anvilcraft.universe.no_id", "No ID provided and cannot find ID in items on hand");

        provider.add("command.anvilcraft.multiBlock.not_multi_block", "This block is not a multi-block");
        provider.add("command.anvilcraft.multiBlock.multi_block_pos", "Main part pos is ");

        provider.add("command.anvilcraft.overseer.head", "All overseers in %s");
        provider.add("command.anvilcraft.overseer.entry", "[%1$s]@Lv.%2$s, random tick: %3$s");
        provider.add("command.anvilcraft.overseer.invalid_dimension", "Dimension is invalid");
    }
}
