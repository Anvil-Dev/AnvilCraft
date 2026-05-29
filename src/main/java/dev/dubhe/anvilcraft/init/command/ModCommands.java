package dev.dubhe.anvilcraft.init.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.dubhe.anvilcraft.command.MultiBlockCommand;
import dev.dubhe.anvilcraft.command.MultiphaseCommand;
import dev.dubhe.anvilcraft.command.OverseerCommand;
import dev.dubhe.anvilcraft.command.PowergridCommand;
import dev.dubhe.anvilcraft.init.ModInspections;
import net.minecraft.commands.CommandSourceStack;

import static net.minecraft.commands.Commands.literal;

public class ModCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = literal("anvilcraft");
        ModInspections.INSTANCE.registerCommand(root);
        PowergridCommand.registerCommand(root);
        MultiphaseCommand.registerCommand(root);
        MultiBlockCommand.registerCommand(root);
        OverseerCommand.registerCommand(root);
        dispatcher.register(root);
    }
}
