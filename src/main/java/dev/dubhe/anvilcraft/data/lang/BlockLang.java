package dev.dubhe.anvilcraft.data.lang;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumLangProvider;
import net.minecraft.world.item.DyeColor;

public class BlockLang {
    public static void init(RegistrumLangProvider provider) {
        String button = "block.anvilcraft.big_red_button.";
        provider.add(button + "base", "Big Button Base");
        provider.add(button + "mounted", "%s Button");
        provider.add("screen.anvilcraft.big_red_button.flowing", "Flowing Rainbow");
        provider.add("screen.anvilcraft.big_red_button.gradient", "Color Cycle");
        for (DyeColor color : DyeColor.values()) {
            if (color != DyeColor.RED) {
                provider.add(button + color.getName(), "Big " + RegistrumLangProvider.toEnglishName(color.getName()) + " Button");
            }
        }
        provider.add(button + "rgb", "Rainbow RGB Button");
        provider.add(button + "small", "Small %s Button");
        provider.add(button + "large", "Large %s Button");
        provider.add("tag.block.anvilcraft.landing_no_recipe",
            "When an anvil lands on one of these blocks, it won't trigger recipe handling.");
        provider.add("block.anvilcraft.overflow_disposal_fluid_tank", "Overflow Disposal Fluid Tank");
        provider.add("block.anvilcraft.overflow_disposal_crate", "Overflow Disposal Crate");
        provider.add("screen.anvilcraft.ruins.title", "Ruins Block");
        provider.add("screen.anvilcraft.ruins.inert", "Interaction: No response");
        provider.add("screen.anvilcraft.ruins.fragile", "Interaction: Break instantly");
        provider.add("screen.anvilcraft.ruins.loot_table", "Loot table");
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
        provider.add(
            "block.anvilcraft.spacetime_supercomputer.tick_sprint_confirmation",
            "The Spacetime Supercomputer requested a Tick Sprint:"
        );
        provider.add("block.anvilcraft.spacetime_supercomputer.tick_sprint_allow", "✓ Allow");
        provider.add(
            "block.anvilcraft.spacetime_supercomputer.tick_sprint_allow_20_minutes",
            "✓ Allow for 20 minutes"
        );
        provider.add(
            "block.anvilcraft.spacetime_supercomputer.tick_sprint_allowed_20_minutes",
            "Tick Sprint requests will be allowed for 20 minutes"
        );
        provider.add(
            "block.anvilcraft.spacetime_supercomputer.tick_sprint_allowed",
            "This command execution was allowed"
        );
        provider.add(
            "block.anvilcraft.spacetime_supercomputer.tick_sprint_cancelled",
            "Tick Sprint command execution cancelled"
        );
        provider.add("block.anvilcraft.spacetime_supercomputer.tick_sprint_reject", "✗ Reject");
        provider.add(
            "block.anvilcraft.spacetime_supercomputer.tick_sprint_rejected",
            "This command execution was rejected"
        );
    }
}
