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

        provider.add("command.anvilcraft.storage.no_id", "No id provided");
        provider.add("command.anvilcraft.storage.not_found", "No storage using this id %s");
        provider.add("command.anvilcraft.storage.info.name", "Storage Name: %s");
        provider.add("command.anvilcraft.storage.info.id", "Storage ID: %s");
        provider.add("command.anvilcraft.storage.info.fullness", "Storage Fullness: %1$d / %2$d entries");
        provider.add("command.anvilcraft.storage.info.entry_level", "Entry Level: %1$d (Limit: %2$d entries)");
        provider.add("command.anvilcraft.storage.info.stack_level", "Stack Level: %1$d (Max Size: %2$d stacks)");
        provider.add("command.anvilcraft.storage.info.transfer_level", "Transfer Level: %d (%s)");
        provider.add("command.anvilcraft.storage.info.transfer.desc.min", "No Ability");
        provider.add("command.anvilcraft.storage.info.transfer.desc.one", "fTransfer");
        provider.add("command.anvilcraft.storage.info.transfer.desc.two", "fTransfer Pro");
        provider.add("command.anvilcraft.storage.info.transfer.desc.three", "eAccess");
        provider.add("command.anvilcraft.storage.info.transfer.desc.four", "eTransfer");
        provider.add(
            "command.anvilcraft.storage.remove.success",
            "Successfully removed storage %1$s.\nYou can recover it by \"%2$s\""
        );
        provider.add("command.anvilcraft.storage.remove.success.hovering", "Click to Run Command");
        provider.add("command.anvilcraft.storage.recover.success", "Successfully recovered storage %s");
        provider.add("command.anvilcraft.storage.recover.clear.success", "Successfully cleared recover station");
    }
}
