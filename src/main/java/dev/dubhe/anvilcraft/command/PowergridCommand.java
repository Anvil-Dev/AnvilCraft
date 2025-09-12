package dev.dubhe.anvilcraft.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.dubhe.anvilcraft.api.power.DynamicPowerComponent;
import dev.dubhe.anvilcraft.api.power.IPowerComponent;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.WorldCoordinates;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static net.minecraft.commands.Commands.literal;

public class PowergridCommand {

    public static final int SHOW_INFO_LIMIT = 256;

    private static int showInfo(CommandContext<CommandSourceStack> ctx) {
        BlockPos pos = ctx.getArgument("pos", WorldCoordinates.class).getBlockPos(ctx.getSource());
        ServerLevel level = ctx.getSource().getLevel();
        AtomicInteger returnValue = new AtomicInteger(0);
        Optional.ofNullable(level.getBlockEntity(pos))
            .filter(be -> be instanceof IPowerComponent)
            .map(IPowerComponent.class::cast)
            .filter(p -> p.getGrid() != null)
            .ifPresentOrElse(p -> {
                returnValue.set(1);
                if (p.getGrid() != null) {
                    MutableComponent message = Component.translatable(
                            "command.anvilcraft.powergrid.info.total_generate", p.getGrid().getGenerate()).withStyle(ChatFormatting.GREEN)
                        .append(Component.literal("\n"))
                        .append(Component.translatable(
                            "command.anvilcraft.powergrid.info.total_consume", p.getGrid().getConsume()).withStyle(ChatFormatting.YELLOW))
                        .append(Component.literal("\n"))
                        .append(Component.translatable("command.anvilcraft.powergrid.info.components").withStyle(ChatFormatting.WHITE))
                        .append(Component.literal("\n"));
                    p.getGrid().getComponents().stream()
                        .limit(SHOW_INFO_LIMIT)
                        .map(IPowerComponent::getCommandDiscription)
                        .map(component -> component.append("\n"))
                        .forEach(message::append);
                    p.getGrid().getDynamicComponents().stream()
                        .limit(SHOW_INFO_LIMIT)
                        .map(DynamicPowerComponent::getCommandDiscription)
                        .map(component -> component.append("\n"))
                        .forEach(message::append);
                    ctx.getSource().sendSuccess(() -> message, true);
                }
            }, () -> ctx.getSource().sendFailure(Component.translatable(
                "command.anvilcraft.powergrid.info.not_found", pos.getX(), pos.getY(), pos.getZ()
            )));
        return returnValue.get();
    }

    public static void registerCommand(LiteralArgumentBuilder<CommandSourceStack> parent) {
        LiteralArgumentBuilder<CommandSourceStack> commandRoot = literal("powergrid");
        commandRoot.then(
            literal("info").then(
                Commands.argument("pos", BlockPosArgument.blockPos())
                    .executes(PowergridCommand::showInfo)
            ));
        parent.then(commandRoot);
    }
}
