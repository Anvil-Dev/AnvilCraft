package dev.dubhe.anvilcraft.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.command.SubCommand;
import dev.dubhe.anvilcraft.api.command.SubCommand2;
import dev.dubhe.anvilcraft.init.command.ModSuggestionProviders;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.MultiphaseRef;
import dev.dubhe.anvilcraft.saved.multiphase.Multiphase;
import dev.dubhe.anvilcraft.saved.multiphase.Multiphases;
import dev.dubhe.anvilcraft.util.CommandUtil;
import dev.dubhe.anvilcraft.util.component.IComponentInfo;
import dev.dubhe.anvilcraft.util.component.MultilineComponentHelper;
import dev.dubhe.anvilcraft.util.component.TranslatableInfo;
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

public class MultiphaseCommand {
    private static final SimpleCommandExceptionType ERROR_NOT_PLAYER = new SimpleCommandExceptionType(
        Component.translatable("command.anvilcraft.multiphase.apply.not_player")
    );

    public static void registerCommand(LiteralArgumentBuilder<CommandSourceStack> parent) {
        Supplier<RequiredArgumentBuilder<CommandSourceStack, UUID>> idPoint = () -> argument("id", UuidArgument.uuid())
            .suggests(ModSuggestionProviders.ALL_MULTIPHASES_ID);

        parent.then(
            CommandUtil.simplePoint(literal("multiphase"), argument("id", UuidArgument.uuid()), MultiphaseCommand::showInfo)
                .then(CommandUtil.simplePoint(literal("info"), idPoint.get(), MultiphaseCommand::showInfo))
                .then(CommandUtil.simplePoint(literal("remove"), idPoint.get(), MultiphaseCommand::remove))
                .then(
                    literal("recover")
                        .then(
                            argument("id", UuidArgument.uuid())
                                .suggests(ModSuggestionProviders.ALL_RECOVERABLE_MULTIPHASES_ID)
                                .executes(MultiphaseCommand::recover)
                        )
                        .then(literal("clear").executes(MultiphaseCommand::clearRecover))
                )
                .then(CommandUtil.simplePoint(literal("apply"), idPoint.get(), MultiphaseCommand::apply2Stack))
        );
    }

    private static int showInfo(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();

        return MultiphaseCommand.execWithContent(
            ctx,
            (id, multiphase) -> {
                var helper = MultilineComponentHelper.create()
                    .addln("command.anvilcraft.multiphase.info.multiphase_id", id)
                    .list(
                        Component.translatable("command.anvilcraft.multiphase.info.phases"),
                        multiphase.phases(),
                        phase -> new IComponentInfo[] {
                            new TranslatableInfo(
                                "command.anvilcraft.multiphase.info.custom_name",
                                phase.customName().orElse(
                                    Component.translatable("command.anvilcraft.multiphase.info.name.empty").withStyle(ChatFormatting.RED)
                                )
                            ),
                            new TranslatableInfo(
                                "command.anvilcraft.multiphase.info.item_name",
                                phase.itemName().orElse(
                                    Component.translatable("command.anvilcraft.multiphase.info.name.empty").withStyle(ChatFormatting.RED)
                                )
                            ),
                            new TranslatableInfo(
                                "command.anvilcraft.multiphase.info.repair_cost",
                                phase.repairCost()
                            ),
                            new TranslatableInfo(
                                "command.anvilcraft.multiphase.info.enchantments",
                                phase.enchantments()
                            ),
                            new TranslatableInfo(
                                "command.anvilcraft.multiphase.info.merciless_enchantments",
                                phase.storedEnchantments()
                            )
                        }
                    );
                return CommandUtil.sendSuccess(source, helper::build);
            }
        );
    }

    private static int remove(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();

        return MultiphaseCommand.execWithUUID(
            ctx,
            id -> {
                if (Multiphases.get().remove(id, source.registryAccess())) {
                    var command = "/anvilcraft multiphase recover " + id;
                    return CommandUtil.sendSuccess(
                        source,
                        "command.anvilcraft.multiphase.remove.success",
                        id,
                        Component.literal(command).withStyle(
                            Style.EMPTY
                                .withColor(ChatFormatting.GREEN)
                                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command))
                                .withHoverEvent(new HoverEvent(
                                    HoverEvent.Action.SHOW_TEXT,
                                    Component.translatable("command.anvilcraft.multiphase.remove.success.hovering")
                                ))
                        )
                    );
                }
                throw CommandUtil.notFound(AnvilCraft.of("multiphase"), id);
            }
        );
    }

    private static int recover(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();

        return MultiphaseCommand.execWithUUID(
            ctx,
            id -> {
                if (Multiphases.get().recover(id, source.registryAccess())) {
                    return CommandUtil.sendSuccess(source, "command.anvilcraft.multiphase.recover.success", id);
                }
                throw CommandUtil.notFound(AnvilCraft.of("multiphase"), id);
            }
        );
    }

    private static int clearRecover(CommandContext<CommandSourceStack> ctx) {
        Multiphases.get().clearRecoverFromCommand();
        return CommandUtil.sendSuccess(ctx.getSource(), "command.anvilcraft.multiphase.recover.clear.success");
    }

    private static int apply2Stack(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();

        return MultiphaseCommand.execWithUUID(
            ctx,
            id -> {
                if (!Multiphases.get().contains(id)) throw CommandUtil.notFound(AnvilCraft.of("multiphase"), id);
                if (!source.isPlayer()) throw MultiphaseCommand.ERROR_NOT_PLAYER.create();
                source.getPlayer().getMainHandItem().set(ModComponents.MULTIPHASE, new MultiphaseRef(id));
                return CommandUtil.sendSuccess(source, "command.anvilcraft.multiphase.recover.success", id);
            }
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

            var multiphase = stack.get(ModComponents.MULTIPHASE);
            if (multiphase == null) throw CommandUtil.ERROR_NO_ID.create();

            id = multiphase.id().get();
        }

        return sub.run(id);
    }

    private static int execWithContent(CommandContext<CommandSourceStack> ctx, SubCommand<Multiphase> sub) throws CommandSyntaxException {
        return MultiphaseCommand.execWithUUID(
            ctx,
            id -> {
                var contentOp = Multiphases.get().get(id);
                if (contentOp.isEmpty()) throw CommandUtil.notFound(AnvilCraft.of("multiphase"), id);
                return sub.run(contentOp.get());
            }
        );
    }

    private static int execWithContent(
        CommandContext<CommandSourceStack> ctx,
        SubCommand2<UUID, Multiphase> sub
    ) throws CommandSyntaxException {
        return MultiphaseCommand.execWithUUID(
            ctx,
            id -> {
                var contentOp = Multiphases.get().get(id);
                if (contentOp.isEmpty()) throw CommandUtil.notFound(AnvilCraft.of("multiphase"), id);
                return sub.run(id, contentOp.get());
            }
        );
    }
}
