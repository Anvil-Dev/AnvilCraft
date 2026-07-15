package dev.dubhe.anvilcraft.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.dubhe.anvilcraft.block.entity.SpacetimeSupercomputerBlockEntity;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.UUID;

public class TickSprintVoteCommand {
    public static void registerCommand(LiteralArgumentBuilder<CommandSourceStack> parent) {
        parent.then(Commands.literal("tick_sprint_vote")
            .requires(CommandSourceStack::isPlayer)
            .then(Commands.argument("dimension", DimensionArgument.dimension())
                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                    .then(Commands.argument("voteId", UuidArgument.uuid())
                        .then(Commands.literal("accept").executes(context -> submitVote(context, true)))
                        .then(Commands.literal("reject").executes(context -> submitVote(context, false)))))));
    }

    private static int submitVote(CommandContext<CommandSourceStack> context, boolean accepted)
        throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ServerLevel level = DimensionArgument.getDimension(context, "dimension");
        BlockPos pos = BlockPosArgument.getBlockPos(context, "pos");
        UUID voteId = UuidArgument.getUuid(context, "voteId");
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof SpacetimeSupercomputerBlockEntity supercomputer
            && supercomputer.submitTickSprintVote(player, voteId, accepted)) {
            return 1;
        }
        return 0;
    }
}
