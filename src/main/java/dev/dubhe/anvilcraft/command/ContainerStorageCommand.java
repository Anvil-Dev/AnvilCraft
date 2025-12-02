package dev.dubhe.anvilcraft.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.dubhe.anvilcraft.api.container.ContainerStorage;
import dev.dubhe.anvilcraft.api.container.ContainerStorages;
import dev.dubhe.anvilcraft.api.container.item.ItemEntries;
import dev.dubhe.anvilcraft.api.container.upgrade.Upgrades;
import dev.dubhe.anvilcraft.init.command.ModSuggestionProviders;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.util.CommandUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;

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

    private static int showInfo(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();

        return ContainerStorageCommand.execWithStorage(
            ctx,
            storage -> {
                ItemEntries entries = storage.getEntries();
                Upgrades upgrades = storage.getUpgrades();
                var msg = Stream.of(
                    Component.translatable("command.anvilcraft.storage.info.name", storage.getName()),
                    Component.translatable("command.anvilcraft.storage.info.id", storage.getId().toString()),
                    Component.translatable("command.anvilcraft.storage.info.fullness", entries.entrySize(), upgrades.getEntryLimit()),
                    Component.translatable(
                        "command.anvilcraft.storage.info.entry_level",
                        upgrades.getEntryLevel(),
                        upgrades.getEntryLimit()
                    ),
                    Component.translatable(
                        "command.anvilcraft.storage.info.stack_level",
                        upgrades.getStackLevel(),
                        upgrades.getStackPower()
                    ),
                    Component.translatable(
                        "command.anvilcraft.storage.info.transfer_level",
                        upgrades.getTransfer().ordinal(),
                        upgrades.getTransfer().getDesc()
                    )
                ).reduce((msg1, msg2) -> msg1.append(ContainerStorageCommand.LF).append(msg2));
                return CommandUtil.sendSuccess(source, msg::get, true);
            }
        );
    }

    private static int removeStorage(CommandContext<CommandSourceStack> ctx) {
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
                                        Component.translatable("command.anvilcraft.storage.remove.success.hovering")
                                    ))
                            )
                        ),
                        true
                    );
                }
                return CommandUtil.sendFailure(source, Component.translatable("command.anvilcraft.storage.not_found", id.toString()));
            }
        );
    }

    private static int recoverStorage(CommandContext<CommandSourceStack> ctx) {
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
                return CommandUtil.sendFailure(source, Component.translatable("command.anvilcraft.storage.not_found", id.toString()));
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

    private static int execWithUUID(CommandContext<CommandSourceStack> ctx, ToIntFunction<UUID> runnable) {
        CommandSourceStack source = ctx.getSource();

        UUID id;
        try {
            id = UuidArgument.getUuid(ctx, "id");
        } catch (IllegalArgumentException e) {
            if (!source.isPlayer()) return CommandUtil.sendFailure(source, "command.anvilcraft.storage.no_id");
            var player = source.getPlayer();
            if (player == null) return CommandUtil.sendFailure(source, "command.anvilcraft.storage.no_id");

            var stack = player.getMainHandItem();
            if (stack.isEmpty()) return CommandUtil.sendFailure(source, "command.anvilcraft.storage.no_id");

            var storage = stack.get(ModComponents.CONTAINER_STORAGE);
            if (storage == null || storage.id().isEmpty()) return CommandUtil.sendFailure(source, "command.anvilcraft.storage.no_id");

            id = storage.id().get();
        }

        return runnable.applyAsInt(id);
    }

    private static int execWithStorage(CommandContext<CommandSourceStack> ctx, ToIntFunction<ContainerStorage> runnable) {
        return ContainerStorageCommand.execWithUUID(
            ctx, id -> {
                CommandSourceStack source = ctx.getSource();

                AtomicInteger result = new AtomicInteger();
                ContainerStorages.get().getStorage(id).ifPresentOrElse(
                    storage -> result.set(runnable.applyAsInt(storage)),
                    () -> result.set(CommandUtil.sendFailure(source, "command.anvilcraft.storage.not_found", id.toString()))
                );

                return result.get();
            }
        );
    }
}
