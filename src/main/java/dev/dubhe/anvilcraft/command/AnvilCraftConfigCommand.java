package dev.dubhe.anvilcraft.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class AnvilCraftConfigCommand {
    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("config").requires(stack -> stack.hasPermission(Commands.LEVEL_ADMINS))
                .then(Commands.literal("reload").executes(AnvilCraftConfigCommand::configReload))
                .then(Commands.literal("check").executes(AnvilCraftConfigCommand::configCheck));
    }

    public static int configReload(@NotNull CommandContext<CommandSourceStack> context) {
        AnvilCraft.loadOrCreateConfig();
        Supplier<Component> success = () -> Component.translatable("commands.anvilcraft.config.reload.success")
                .withStyle(ChatFormatting.GREEN)
                .withStyle(ChatFormatting.BOLD);
        context.getSource().sendSuccess(success, false);
        return 0;
    }

    public static int configCheck(@NotNull CommandContext<CommandSourceStack> context) {
        String configStr = AnvilCraft.GSON.toJson(AnvilCraft.config);
        Supplier<Component> success = () -> Component.translatable("commands.anvilcraft.config.check.success")
                .withStyle(ChatFormatting.GREEN)
                .withStyle(ChatFormatting.BOLD);
        Supplier<Component> config = () -> Component.literal(configStr).withStyle(ChatFormatting.GRAY);
        context.getSource().sendSuccess(success, false);
        context.getSource().sendSuccess(config, false);
        return 0;
    }
}
