package dev.dubhe.anvilcraft.init;

import net.minecraft.commands.CommandSourceStack;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import org.jetbrains.annotations.NotNull;

import static net.minecraft.commands.Commands.literal;

public class ModCommands {
    public static void register(@NotNull CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = literal("anvilcraft");
        ModInspections.INSTANCE.registerCommand(root);
        dispatcher.register(root);
    }
}
