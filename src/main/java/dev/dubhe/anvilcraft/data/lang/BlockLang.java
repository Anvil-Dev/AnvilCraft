package dev.dubhe.anvilcraft.data.lang;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumLangProvider;

public class BlockLang {
    @SuppressWarnings("checkstyle:LineLength")
    public static void init(RegistrumLangProvider provider) {
        provider.add("block.anvilcraft.check_valve", "Check Valve");
        provider.add("block.anvilcraft.spacetime_supercomputer.insufficient_energy", "Insufficient energy to execute the command");
        provider.add("block.anvilcraft.spacetime_supercomputer.no_supported_command", "This command is not supported for execution");
        provider.add(
            "block.anvilcraft.spacetime_supercomputer.tick_sprint_countdown",
            "Tick Sprint will start in %1$s seconds"
        );
        provider.add(
            "block.anvilcraft.spacetime_supercomputer.tick_sprint_countdown_in_progress",
            "A Tick Sprint countdown is already in progress"
        );
    }
}
