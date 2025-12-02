package dev.dubhe.anvilcraft.util;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.ArgumentBuilder;
import dev.dubhe.anvilcraft.command.ContainerStorageCommand;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

import static net.minecraft.commands.Commands.literal;

public class CommandUtil {
    public static final int FAILURE = 0;

    public static int sendFailure(CommandSourceStack source, String transKey) {
        return CommandUtil.sendFailure(source, Component.translatable(transKey));
    }

    public static int sendFailure(CommandSourceStack source, String transKey, Object... args) {
        return CommandUtil.sendFailure(source, Component.translatable(transKey, args));
    }

    public static int sendFailure(CommandSourceStack source, Component msg) {
        source.sendFailure(msg);
        return CommandUtil.FAILURE;
    }

    public static int sendSuccess(CommandSourceStack source, Supplier<Component> msg, boolean allowLogging) {
        source.sendSuccess(msg, allowLogging);
        return Command.SINGLE_SUCCESS;
    }

    public static <S, T extends ArgumentBuilder<S, T>, T2 extends ArgumentBuilder<S, T2>> T simplePoint(
        T builder,
        T2 subPoint,
        Command<S> command
    ) {
        return builder.executes(command).then(subPoint.executes(command));
    }
}
