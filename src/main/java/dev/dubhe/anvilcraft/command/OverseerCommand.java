package dev.dubhe.anvilcraft.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import dev.dubhe.anvilcraft.block.entity.OverseerBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.util.OverseerUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.Set;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class OverseerCommand {
    private static final Logger logger = LogUtils.getLogger();

    public static void registerCommand(LiteralArgumentBuilder<CommandSourceStack> builder) {
        LiteralArgumentBuilder<CommandSourceStack> root = literal("overseer").requires(source -> source.hasPermission(2));
        root.then(
            literal("list")
                .then(
                    argument("dimension", DimensionArgument.dimension())
                        .executes(OverseerCommand::listOverseers)
                        .then(
                            argument("includeInactive", BoolArgumentType.bool())
                                .executes(OverseerCommand::listOverseersWithFlag)
                        )
                )
        );
        builder.then(root);
    }

    private static int listOverseers(CommandContext<CommandSourceStack> ctx) {
        return listOverseersInternal(ctx, false);
    }

    private static int listOverseersWithFlag(CommandContext<CommandSourceStack> ctx) {
        boolean includeInactive = BoolArgumentType.getBool(ctx, "includeInactive");
        return listOverseersInternal(ctx, includeInactive);
    }

    private static int listOverseersInternal(CommandContext<CommandSourceStack> ctx, boolean includeInactive) {
        CommandSourceStack source = ctx.getSource();
        ServerLevel level;
        try {
            level = DimensionArgument.getDimension(ctx, "dimension");
        } catch (CommandSyntaxException exception) {
            source.sendFailure(Component.translatable("command.anvilcraft.overseer.invalid_dimension").withStyle(ChatFormatting.RED));
            return 1;
        }
        ResourceKey<Level> dimension = level.dimension();
        Set<BlockPos> overseersPoses = OverseerUtil.getPlacedOverseers(dimension);
        MutableComponent msg = Component.translatable("command.anvilcraft.overseer.head", dimension.location().toString());
        for (BlockPos pos : overseersPoses) {
            Optional<OverseerBlockEntity> overseerBlockEntityOptional = level.getBlockEntity(pos, ModBlockEntities.OVERSEER.get());
            if (overseerBlockEntityOptional.isEmpty()) {
                logger.warn("Invalid overseer at {}", pos);
                continue;
            }
            OverseerBlockEntity overseerBlockEntity = overseerBlockEntityOptional.get();
            if (!includeInactive && overseerBlockEntity.getLoadLevel() <= 0) continue;
            MutableComponent component = Component.translatable(
                "command.anvilcraft.overseer.entry",
                Component.translatable("chat.coordinates", pos.getX(), pos.getY(), pos.getZ())
                    .withStyle((style) -> style.withClickEvent(new ClickEvent(
                        ClickEvent.Action.COPY_TO_CLIPBOARD,
                        "%s %s %s".formatted(pos.getX(), pos.getY(), pos.getZ())
                    )).withHoverEvent(new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        Component.translatable("chat.copy.click")
                    )).withColor(ChatFormatting.GREEN)),
                overseerBlockEntity.getLoadLevel(),
                overseerBlockEntity.getRandomTick()
            );
            msg.append(Component.literal("\n")).append(component);
        }
        source.sendSuccess(() -> msg, true);
        return 0;
    }
}
