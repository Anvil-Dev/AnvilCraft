package dev.dubhe.anvilcraft.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.WorldCoordinates;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class MultiBlockCommand {
    public static void registerCommand(LiteralArgumentBuilder<CommandSourceStack> builder) {
        LiteralArgumentBuilder<CommandSourceStack> root = literal("multiBlock");
        root.then(
            literal("getMainPartPos")
                .then(
                    argument("pos", BlockPosArgument.blockPos())
                        .executes(MultiBlockCommand::getMainPartPos)
                )
        );
        builder.then(root);
    }

    private static int getMainPartPos(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ServerLevel level = source.getLevel();
        BlockPos pos = ctx.getArgument("pos", WorldCoordinates.class).getBlockPos(source);
        if (!level.isLoaded(pos)) {
            source.sendFailure(Component.translatable("argument.pos.unloaded").withStyle(ChatFormatting.RED));
            return 1;
        }
        BlockState blockState = level.getBlockState(pos);
        if (blockState.getBlock() instanceof AbstractMultiPartBlock<?> multiPartBlock) {
            BlockPos mainPartPos = multiPartBlock.getMainPartPos(pos, blockState);
            MutableComponent component = ComponentUtils.wrapInSquareBrackets(
                Component.translatable("chat.coordinates", mainPartPos.getX(), mainPartPos.getY(), mainPartPos.getZ())
            ).withStyle((style) -> style.withClickEvent(new ClickEvent(
                ClickEvent.Action.COPY_TO_CLIPBOARD,
                "%s %s %s".formatted(mainPartPos.getX(), mainPartPos.getY(), mainPartPos.getZ())
            )).withHoverEvent(new HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                Component.translatable("chat.copy.click")
            )).withColor(ChatFormatting.GREEN));
            source.sendSuccess(() -> Component.translatable("command.anvilcraft.multiBlock.multi_block_pos").append(component), true);
            return 0;
        }
        source.sendFailure(Component.translatable("command.anvilcraft.multiBlock.not_multi_block").withStyle(ChatFormatting.RED));
        return 1;
    }
}
