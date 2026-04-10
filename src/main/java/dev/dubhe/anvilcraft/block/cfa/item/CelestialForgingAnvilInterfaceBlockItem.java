package dev.dubhe.anvilcraft.block.cfa.item;

import dev.dubhe.anvilcraft.block.cfa.CelestialForgingAnvilBlock;
import dev.dubhe.anvilcraft.block.cfa.interfaces.CelestialForgingAnvilInterfaceBlock;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.util.HorizontalDirection;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CelestialForgingAnvilInterfaceBlockItem extends BlockItem {
    public CelestialForgingAnvilInterfaceBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    protected @Nullable BlockState getPlacementState(BlockPlaceContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        BlockPos pos = context.getClickedPos();
        if (player == null) {
            return null;
        }
        List<Direction> cfaDir = new ObjectArrayList<>();
        for (Direction direction : HorizontalDirection.HORIZONTAL_DIRECTIONS) {
            BlockPos cfaPos = pos.relative(direction);
            BlockState cfaState = level.getBlockState(cfaPos);
            if (cfaState.is(ModBlocks.CELESTIAL_FORGING_ANVIL)) {
                Cube3x3PartHalf half = cfaState.getValue(CelestialForgingAnvilBlock.HALF);
                if (half == Cube3x3PartHalf.BOTTOM_E
                    || half == Cube3x3PartHalf.BOTTOM_W
                    || half == Cube3x3PartHalf.BOTTOM_N
                    || half == Cube3x3PartHalf.BOTTOM_S
                    || half == Cube3x3PartHalf.BOTTOM_EN
                    || half == Cube3x3PartHalf.BOTTOM_ES
                    || half == Cube3x3PartHalf.BOTTOM_WN
                    || half == Cube3x3PartHalf.BOTTOM_WS) {
                    cfaDir.add(direction);
                }
            }
        }
        if (!cfaDir.isEmpty()) {
            if (cfaDir.contains(player.getDirection())) {
                return getBlock().defaultBlockState()
                    .setValue(CelestialForgingAnvilInterfaceBlock.FACING, player.getDirection().getOpposite());
            }
            return getBlock().defaultBlockState().setValue(CelestialForgingAnvilInterfaceBlock.FACING, cfaDir.getFirst().getOpposite());
        }
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(
                Component.translatable("screen.anvilcraft.tooltip.cfa_interface").withStyle(ChatFormatting.RED),
                true
            );
        }
        return null;
    }
}
