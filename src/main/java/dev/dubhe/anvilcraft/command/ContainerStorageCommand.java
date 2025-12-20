package dev.dubhe.anvilcraft.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.command.SubCommand;
import dev.dubhe.anvilcraft.api.container.item.ItemEntries;
import dev.dubhe.anvilcraft.api.container.upgrade.Upgrades;
import dev.dubhe.anvilcraft.init.command.ModSuggestionProviders;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.saved.sc.ContainerStorage;
import dev.dubhe.anvilcraft.saved.sc.ContainerStorages;
import dev.dubhe.anvilcraft.util.CommandUtil;
import dev.dubhe.anvilcraft.util.component.MultilineComponentHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;

import java.util.UUID;
import java.util.function.Supplier;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class ContainerStorageCommand {
    public static final Component LF = Component.literal("\n");

    public static void registerCommand(LiteralArgumentBuilder<CommandSourceStack> parent) {
        Supplier<RequiredArgumentBuilder<CommandSourceStack, UUID>> idPoint = () -> argument("id", UuidArgument.uuid())
            .suggests(ModSuggestionProviders.ALL_SHULKER_CONTAINERS_ID);

        parent.then(
            CommandUtil.simplePoint(literal("storage"), argument("id", UuidArgument.uuid()), ContainerStorageCommand::showInfo)
                .then(CommandUtil.simplePoint(literal("info"), idPoint.get(), ContainerStorageCommand::showInfo))
                .then(CommandUtil.simplePoint(literal("remove"), idPoint.get(), ContainerStorageCommand::removeStorage))
                .then(
                    literal("recover")
                        .then(
                            argument("id", UuidArgument.uuid())
                                .suggests(ModSuggestionProviders.ALL_RECOVERABLE_SHULKER_CONTAINERS_ID)
                                .executes(ContainerStorageCommand::recoverStorage)
                        )
                        .then(literal("clear").executes(ContainerStorageCommand::clearRecover))
                )
        );
    }

    private static int showInfo(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();

        return ContainerStorageCommand.execWithStorage(
            ctx,
            storage -> {
                ItemEntries entries = storage.getEntries();
                Upgrades upgrades = storage.getUpgrades();
                var msg = MultilineComponentHelper.create()
                    .addln("command.anvilcraft.storage.info.name", storage.getName())
                    .addln("command.anvilcraft.storage.info.id", storage.getId())
                    .addln("command.anvilcraft.storage.info.fullness", entries.entrySize(), upgrades.getEntryLimit())
                    .addln("command.anvilcraft.storage.info.entry_level", upgrades.getEntryLevel(), upgrades.getEntryLimit())
                    .addln("command.anvilcraft.storage.info.stack_level", upgrades.getStackLevel(), upgrades.getStackPower())
                    .addln(
                        "command.anvilcraft.storage.info.transfer_level",
                        upgrades.getTransfer().ordinal(),
                        upgrades.getTransfer().getDesc()
                    );
                return CommandUtil.sendSuccess(source, msg::build, true);
            }
        );
    }

    private static int removeStorage(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();

        return ContainerStorageCommand.execWithUUID(
            ctx,
            id -> {
                if (ContainerStorages.get().removeStorage(id, source.registryAccess())) {
                    var command = "/anvilcraft storage recover " + id;
                    return CommandUtil.sendSuccess(
                        source,
                        () -> Component.translatable(
                            "command.anvilcraft.storage.remove.success",
                            id.toString(),
                            Component.literal(command).withStyle(
                                Style.EMPTY
                                    .withColor(ChatFormatting.GREEN)
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command))
                                    .withHoverEvent(new HoverEvent(
                                        HoverEvent.Action.SHOW_TEXT,
                                        CommandUtil.clickToRunCmdMsg()
                                    ))
                            )
                        ),
                        true
                    );
                }
                throw CommandUtil.notFound(AnvilCraft.of("storage"), id);
            }
        );
    }

    private static int recoverStorage(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();

        return ContainerStorageCommand.execWithUUID(
            ctx,
            id -> {
                if (ContainerStorages.get().recover(id, source.registryAccess())) {
                    return CommandUtil.sendSuccess(
                        source,
                        () -> Component.translatable("command.anvilcraft.storage.recover.success", id.toString()),
                        true
                    );
                }
                throw CommandUtil.notFound(AnvilCraft.of("storage"), id);
            }
        );
    }

    private static int clearRecover(CommandContext<CommandSourceStack> ctx) {
        ContainerStorages.get().clearRecoverFromCommand();
        return CommandUtil.sendSuccess(
            ctx.getSource(),
            () -> Component.translatable("command.anvilcraft.storage.recover.clear.success"),
            true
        );
    }

    private static int execWithUUID(CommandContext<CommandSourceStack> ctx, SubCommand<UUID> sub) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();

        UUID id;
        try {
            id = UuidArgument.getUuid(ctx, "id");
        } catch (IllegalArgumentException e) {
            if (!source.isPlayer()) throw CommandUtil.ERROR_NO_ID.create();
            var player = source.getPlayer();
            if (player == null) throw CommandUtil.ERROR_NO_ID.create();

            var stack = player.getMainHandItem();
            if (stack.isEmpty()) throw CommandUtil.ERROR_NO_ID.create();

            var storage = stack.get(ModComponents.CONTAINER_STORAGE);
            if (storage == null || storage.id().isEmpty()) throw CommandUtil.ERROR_NO_ID.create();

            id = storage.id().get();
        }

        return sub.run(id);
    }

    private static int execWithStorage(
        CommandContext<CommandSourceStack> ctx,
        SubCommand<ContainerStorage> sub
    ) throws CommandSyntaxException {
        return ContainerStorageCommand.execWithUUID(
            ctx, id -> {
                var contentOp = ContainerStorages.get().get(id);
                if (contentOp.isEmpty()) throw CommandUtil.notFound(AnvilCraft.of("storage"), id);
                return sub.run(contentOp.get());
            }
        );
    }
}
