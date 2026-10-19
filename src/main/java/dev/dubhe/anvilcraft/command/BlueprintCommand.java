package dev.dubhe.anvilcraft.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.dubhe.anvilcraft.saved.BlueprintAdministrators;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;

public final class BlueprintCommand {
    private BlueprintCommand() {
    }

    public static void registerCommand(LiteralArgumentBuilder<CommandSourceStack> root) {
        var administrators = Commands.literal("blueprint")
            .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_ADMIN));
        administrators.then(Commands.literal("add").then(Commands.argument("players", GameProfileArgument.gameProfile())
            .executes(context -> setAllowed(context, true))));
        administrators.then(Commands.literal("remove").then(Commands.argument("players", GameProfileArgument.gameProfile())
            .executes(context -> setAllowed(context, false))));
        root.then(administrators);
    }

    private static int setAllowed(CommandContext<CommandSourceStack> context, boolean allowed) throws CommandSyntaxException {
        var administrators = BlueprintAdministrators.get(context.getSource().getServer());
        int changed = 0;
        for (var profile : GameProfileArgument.getGameProfiles(context, "players")) {
            if (administrators.setAllowed(profile.id(), allowed)) changed++;
        }
        int count = changed;
        context.getSource().sendSuccess(() -> Component.translatable(
            "command.anvilcraft.blueprint." + (allowed ? "added" : "removed"), count), true);
        return changed;
    }
}
