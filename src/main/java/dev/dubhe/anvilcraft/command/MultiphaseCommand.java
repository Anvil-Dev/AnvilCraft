package dev.dubhe.anvilcraft.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.Multiphase;
import dev.dubhe.anvilcraft.util.CommandUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.item.ItemStack;

public class MultiphaseCommand {
    private static final SimpleCommandExceptionType ERROR_NO_MULTIPHASE = new SimpleCommandExceptionType(
        Component.translatable("command.anvilcraft.multiphase.no_item")
    );
    private static final SimpleCommandExceptionType ERROR_MAX_PHASES = new SimpleCommandExceptionType(
        Component.translatable("command.anvilcraft.multiphase.add.full")
    );

    public static void registerCommand(LiteralArgumentBuilder<CommandSourceStack> parent) {
        parent.then(
            Commands.literal("multiphase")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .executes(MultiphaseCommand::showInfo)
                .then(Commands.literal("info").executes(MultiphaseCommand::showInfo))
                .then(
                    Commands.literal("add")
                        .executes(context -> addPhases(context, 1))
                        .then(
                            Commands.argument("count", IntegerArgumentType.integer(1, Multiphase.MAX_PHASE_COUNT))
                                .executes(context -> addPhases(
                                    context,
                                    IntegerArgumentType.getInteger(context, "count")
                                ))
                        )
                )
        );
    }

    private static int showInfo(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ItemStack stack = getMultiphaseStack(context.getSource());
        Multiphase multiphase = stack.get(ModComponents.MULTIPHASE).capture(stack);
        stack.set(ModComponents.MULTIPHASE, multiphase);

        MutableComponent message = Component.translatable(
            "command.anvilcraft.multiphase.info.summary",
            multiphase.phases().size(),
            multiphase.activePhase() + 1
        ).withStyle(ChatFormatting.LIGHT_PURPLE);
        for (int i = 0; i < multiphase.phases().size(); i++) {
            Multiphase.Phase phase = multiphase.phases().get(i);
            message.append(Component.literal("\n")).append(Component.translatable(
                "command.anvilcraft.multiphase.info.phase",
                i + 1,
                multiphase.phaseDisplayName(i),
                phase.repairCost(),
                phase.enchantments()
            ));
        }
        return CommandUtil.sendSuccess(context.getSource(), () -> message);
    }

    private static int addPhases(CommandContext<CommandSourceStack> context, int requested) throws CommandSyntaxException {
        ItemStack stack = getMultiphaseStack(context.getSource());
        int added = 0;
        for (int i = 0; i < requested; i++) {
            Multiphase multiphase = stack.get(ModComponents.MULTIPHASE);
            if (multiphase == null || !multiphase.addPhase(stack)) break;
            added++;
        }
        if (added == 0) throw ERROR_MAX_PHASES.create();
        int phaseCount = stack.get(ModComponents.MULTIPHASE).phases().size();
        return CommandUtil.sendSuccess(
            context.getSource(),
            "command.anvilcraft.multiphase.add.success",
            added,
            phaseCount
        );
    }

    private static ItemStack getMultiphaseStack(CommandSourceStack source) throws CommandSyntaxException {
        ItemStack stack = source.getPlayerOrException().getMainHandItem();
        if (!stack.has(ModComponents.MULTIPHASE)) throw ERROR_NO_MULTIPHASE.create();
        return stack;
    }
}
