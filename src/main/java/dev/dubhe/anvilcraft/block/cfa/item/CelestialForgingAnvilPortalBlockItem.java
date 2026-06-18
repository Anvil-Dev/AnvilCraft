package dev.dubhe.anvilcraft.block.cfa.item;

import dev.dubhe.anvilcraft.block.cfa.CelestialForgingAnvilBlock;
import dev.dubhe.anvilcraft.block.cfa.CelestialForgingAnvilPortalBlock;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import dev.dubhe.anvilcraft.block.state.Cube323PartHalf;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
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

public class CelestialForgingAnvilPortalBlockItem extends BlockItem {
    public CelestialForgingAnvilPortalBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    protected @Nullable BlockState getPlacementState(BlockPlaceContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        BlockPos pos = context.getClickedPos();
        if (player == null) return null;

        // Scan adjacent positions for CFA side centers
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos cfaPos = pos.relative(dir);
            BlockState cfaState = level.getBlockState(cfaPos);
            if (cfaState.is(ModBlocks.CELESTIAL_FORGING_ANVIL)) {
                Cube323PartHalf half = cfaState.getValue(CelestialForgingAnvilBlock.HALF);
                // Only allow placement on side centers (BOTTOM_N/S/E/W)
                if (half == Cube323PartHalf.BOTTOM_N || half == Cube323PartHalf.BOTTOM_S
                    || half == Cube323PartHalf.BOTTOM_E || half == Cube323PartHalf.BOTTOM_W) {
                    // Check with parent CFA that this side doesn't already have a portal
                    BlockPos controllerPos = cfaPos.offset(half.getOffset().multiply(-1));
                    if (level.getBlockEntity(controllerPos) instanceof CelestialForgingAnvilBlockEntity cfaBe) {
                        if (cfaBe.getPortals().containsKey(half)) {
                            if (player instanceof ServerPlayer sp) {
                                sp.sendSystemMessage(
                                    Component.translatable("message.anvilcraft.portal.already_exists")
                                        .withStyle(ChatFormatting.RED), true);
                            }
                            return null;
                        }
                        return getBlock().defaultBlockState()
                            .setValue(CelestialForgingAnvilPortalBlock.FACING, dir.getOpposite())
                            .setValue(CelestialForgingAnvilPortalBlock.OPEN, false);
                    }
                }
            }
        }

        if (player instanceof ServerPlayer sp) {
            sp.sendSystemMessage(
                Component.translatable("message.anvilcraft.portal.invalid_placement")
                    .withStyle(ChatFormatting.RED), true);
        }
        return null;
    }
}
