package dev.dubhe.anvilcraft.util;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;
import java.util.function.Supplier;

import static net.minecraft.commands.Commands.literal;

public class CommandUtil {
    public static final SimpleCommandExceptionType ERROR_SENDER_NOT_PLAYER = new SimpleCommandExceptionType(
        Component.translatable("command.anvilcraft.universe.sender_not_player")
    );
    public static final SimpleCommandExceptionType ERROR_NO_ID = new SimpleCommandExceptionType(
        Component.translatable("command.anvilcraft.universe.no_id")
    );
    private static final Dynamic2CommandExceptionType ERROR_NOT_FOUND = new Dynamic2CommandExceptionType(
        (contentId, id) -> Component.translatable(
            "command." + contentId.toString().replace(':', '.') + ".not_found", ComponentUtil.argValidate(id)
        )
    );

    /**
     * 用于未找到内容时，抛出 {@link CommandSyntaxException}
     *
     * @param contentId 内容id
     * @return 一个 {@link CommandSyntaxException}
     * @apiNote 例如错误信息为 {@code command.anvilcraft.multiphase.not_found}，<br>
     *          则需传入的内容id为 {@code anvilcraft:multiphase}
     */
    public static CommandSyntaxException notFound(ResourceLocation contentId, UUID id) {
        return ERROR_NOT_FOUND.create(contentId, id);
    }

    public static Component clickToRunCmdMsg() {
        return Component.translatable("command.anvilcraft.universe.click_to_run_cmd");
    }

    public static int sendSuccess(CommandSourceStack source, String key, Object... args) {
        return CommandUtil.sendSuccess(source, key, true, args);
    }

    public static int sendSuccess(CommandSourceStack source, String key, boolean allowLogging, Object... args) {
        return CommandUtil.sendSuccess(source, () -> Component.translatable(key, ComponentUtil.argValidate(args)), allowLogging);
    }

    public static int sendSuccess(CommandSourceStack source, Supplier<Component> msg) {
        return CommandUtil.sendSuccess(source, msg, true);
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
