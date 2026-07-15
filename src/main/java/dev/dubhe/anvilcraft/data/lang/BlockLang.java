package dev.dubhe.anvilcraft.data.lang;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumLangProvider;

public class BlockLang {
    public static void init(RegistrumLangProvider provider) {
        provider.add("block.anvilcraft.spacetime_supercomputer.insufficient_energy", "Insufficient energy to execute the command");
        provider.add("block.anvilcraft.spacetime_supercomputer.no_supported_command", "This command is not supported for execution");
        provider.add(
            "block.anvilcraft.spacetime_supercomputer.tick_sprint_countdown_in_progress",
            "A Tick Sprint countdown is already in progress"
        );
        provider.add(
            "block.anvilcraft.spacetime_supercomputer.tick_sprint_confirmation",
            "The Spacetime Supercomputer requested a Tick Sprint:"
        );
        provider.add("block.anvilcraft.spacetime_supercomputer.tick_sprint_allow", "Allow");
        provider.add(
            "block.anvilcraft.spacetime_supercomputer.tick_sprint_allowed",
            "This command execution was allowed"
        );
        provider.add(
            "block.anvilcraft.spacetime_supercomputer.tick_sprint_cancelled",
            "Tick Sprint command execution cancelled"
        );
        provider.add("block.anvilcraft.spacetime_supercomputer.tick_sprint_reject", "Reject");
        provider.add(
            "block.anvilcraft.spacetime_supercomputer.tick_sprint_rejected",
            "This command execution was rejected"
        );
    }
}
