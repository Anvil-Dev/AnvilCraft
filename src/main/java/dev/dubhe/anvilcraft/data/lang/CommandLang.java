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

        provider.add("command.anvilcraft.storage.no_hand_item", "No item in main hand");
        provider.add("command.anvilcraft.storage.no_storage", "The held item does not reference a storage");
        provider.add("command.anvilcraft.storage.invalid_type", "Invalid storage type");
        provider.add("command.anvilcraft.storage.invalid_id", "Invalid storage ID");
        provider.add("command.anvilcraft.storage.info.item", "Storage info of %s");
        provider.add("command.anvilcraft.storage.info.none", "none");
        provider.add("command.anvilcraft.storage.info.terminal", "Bound storage: %s");
        provider.add("command.anvilcraft.storage.info.ref", "Type: %1$s, ID: %2$s");
        provider.add("command.anvilcraft.storage.list.head", "Storages (%1$s):");
        provider.add("command.anvilcraft.storage.list.entry", "Type: %1$s, ID: %2$s");
        provider.add("command.anvilcraft.storage.bind.success", "Bound %1$s storage to %2$s");
        provider.add("command.anvilcraft.storage.unbind.success", "Unbound storage");

        provider.add("command.anvilcraft.universe.no_id", "No ID provided and cannot find ID in items on hand");

        provider.add("command.anvilcraft.multiBlock.not_multi_block", "This block is not a multi-block");
        provider.add("command.anvilcraft.multiBlock.multi_block_pos", "Main part pos is ");

        provider.add("command.anvilcraft.overseer.head", "All overseers in %s");
        provider.add("command.anvilcraft.overseer.entry", "[%1$s]@Lv.%2$s, random tick: %3$s");
        provider.add("command.anvilcraft.overseer.invalid_dimension", "Dimension is invalid");
    }
}
