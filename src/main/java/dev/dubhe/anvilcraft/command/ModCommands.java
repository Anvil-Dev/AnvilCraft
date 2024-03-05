package dev.dubhe.anvilcraft.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.jetbrains.annotations.NotNull;

public class ModCommands {
    @SuppressWarnings("unused")
    public static void register(@NotNull CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("anvilcraft");
        root.then(AnvilCraftConfigCommand.register());
        dispatcher.register(root);
    }
}
