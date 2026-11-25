package dev.dubhe.anvilcraft.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.dubhe.anvilcraft.util.NoPowerChallengeNotice;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

/// 聊天栏里「点击查看」「点击忽略」对应的内部命令。
public class NoPowerChallengeCommand {
    public static void registerCommand(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("nopower")
            .then(Commands.literal("notice").executes(context -> {
                ServerPlayer player = context.getSource().getPlayerOrException();
                NoPowerChallengeNotice.sendDetails(player);
                return 1;
            }))
            .then(Commands.literal("ignore").executes(context -> {
                ServerPlayer player = context.getSource().getPlayerOrException();
                NoPowerChallengeNotice.ignore(player);
                return 1;
            })));
    }
}