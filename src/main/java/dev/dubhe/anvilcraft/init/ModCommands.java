package dev.dubhe.anvilcraft.init;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.dubhe.anvilcraft.command.AnvilCraftConfigCommand;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.jetbrains.annotations.NotNull;

public class ModCommands {
    @SuppressWarnings("unused")
    private static void register(@NotNull CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("anvilcraft");
        root.then(AnvilCraftConfigCommand.register());
        dispatcher.register(root);
    }

    public static void register(){
        CommandRegistrationCallback.EVENT.register(ModCommands::register);
    }
}
