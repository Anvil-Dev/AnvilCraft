package dev.dubhe.anvilcraft.block.cfa.item;

import dev.dubhe.anvilcraft.block.cfa.CelestialForgingAnvilBlock;
import dev.dubhe.anvilcraft.block.item.FlexibleMultiPartBlockItem;
import dev.dubhe.anvilcraft.block.multipart.FlexibleMultiPartBlock;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import dev.dubhe.anvilcraft.block.state.DirectionCube232PartHalf;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

public class CelestialForgingAnvilAmplifierBlockItem
    extends FlexibleMultiPartBlockItem<DirectionCube232PartHalf, DirectionProperty, Direction> {
    public CelestialForgingAnvilAmplifierBlockItem(
        FlexibleMultiPartBlock<DirectionCube232PartHalf, DirectionProperty, Direction> block,
        Properties properties
    ) {
        super(block, properties);
    }

    @Override
    public boolean canPlace(BlockPlaceContext context, BlockState state) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (player == null) {
            return false;
        }
        return switch (player.getDirection()) {
            case EAST -> {
                BlockPos offset = pos.offset(1, 0, -2);
                BlockState blockState = level.getBlockState(offset);
                if (
                    blockState.is(ModBlocks.CELESTIAL_FORGING_ANVIL)
                        && blockState.getValue(CelestialForgingAnvilBlock.HALF) == Cube3x3PartHalf.BOTTOM_WS
                ) {
                    yield true;
                }
                if (player instanceof ServerPlayer serverPlayer) {
                    serverPlayer.sendSystemMessage(
                        Component.translatable("screen.anvilcraft.tooltip.cfa_amplifier").withStyle(ChatFormatting.RED),
                        true
                    );
                }
                yield false;
            }
            case SOUTH -> {
                BlockPos offset = pos.offset(1, 0, 1);
                BlockState blockState = level.getBlockState(offset);
                if (
                    blockState.is(ModBlocks.CELESTIAL_FORGING_ANVIL)
                        && blockState.getValue(CelestialForgingAnvilBlock.HALF) == Cube3x3PartHalf.BOTTOM_WN
                ) {
                    yield true;
                }
                if (player instanceof ServerPlayer serverPlayer) {
                    serverPlayer.sendSystemMessage(
                        Component.translatable("screen.anvilcraft.tooltip.cfa_amplifier").withStyle(ChatFormatting.RED),
                        true
                    );
                }
                yield false;
            }
            case WEST -> {
                BlockPos offset = pos.offset(-2, 0, 1);
                BlockState blockState = level.getBlockState(offset);
                if (
                    blockState.is(ModBlocks.CELESTIAL_FORGING_ANVIL)
                        && blockState.getValue(CelestialForgingAnvilBlock.HALF) == Cube3x3PartHalf.BOTTOM_EN
                ) {
                    yield true;
                }
                if (player instanceof ServerPlayer serverPlayer) {
                    serverPlayer.sendSystemMessage(
                        Component.translatable("screen.anvilcraft.tooltip.cfa_amplifier").withStyle(ChatFormatting.RED),
                        true
                    );
                }
                yield false;
            }
            case NORTH -> {
                BlockPos offset = pos.offset(-2, 0, -2);
                BlockState blockState = level.getBlockState(offset);
                if (
                    blockState.is(ModBlocks.CELESTIAL_FORGING_ANVIL)
                        && blockState.getValue(CelestialForgingAnvilBlock.HALF) == Cube3x3PartHalf.BOTTOM_ES
                ) {
                    yield true;
                }
                if (player instanceof ServerPlayer serverPlayer) {
                    serverPlayer.sendSystemMessage(
                        Component.translatable("screen.anvilcraft.tooltip.cfa_amplifier").withStyle(ChatFormatting.RED),
                        true
                    );
                }
                yield false;
            }
            default -> {
                if (player instanceof ServerPlayer serverPlayer) {
                    serverPlayer.sendSystemMessage(
                        Component.translatable("screen.anvilcraft.tooltip.cfa_amplifier").withStyle(ChatFormatting.RED),
                        true
                    );
                }
                yield false;
            }
        };
    }
}
