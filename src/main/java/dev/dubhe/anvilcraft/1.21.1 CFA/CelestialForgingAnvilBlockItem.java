package dev.dubhe.anvilcraft.block.cfa.item;

import dev.dubhe.anvilcraft.block.item.SimpleMultiPartBlockItem;
import dev.dubhe.anvilcraft.block.multipart.SimpleMultiPartBlock;
import dev.dubhe.anvilcraft.block.state.Cube323PartHalf;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class CelestialForgingAnvilBlockItem extends SimpleMultiPartBlockItem<Cube323PartHalf> {
    private static final int PLACEMENT_RADIUS = 7;

    public CelestialForgingAnvilBlockItem(
        SimpleMultiPartBlock<Cube323PartHalf> block, Properties properties
    ) {
        super(block, properties);
    }

    @Override
    public boolean canPlace(BlockPlaceContext context, BlockState state) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        BlockPos clickedPos = context.getClickedPos();
        for (BlockPos pos : BlockPos.betweenClosed(
            clickedPos.offset(PLACEMENT_RADIUS, PLACEMENT_RADIUS, PLACEMENT_RADIUS),
            clickedPos.offset(-PLACEMENT_RADIUS, -PLACEMENT_RADIUS, -PLACEMENT_RADIUS)
        )) {
            if (level.getBlockState(pos).is(this.getBlock())) {
                if (level.isClientSide() && player != null) {
                    player.displayClientMessage(
                        Component.translatable(
                            "block.anvilcraft.celestial_forging_anvil.placement_too_close_to_another")
                            .withStyle(ChatFormatting.RED),
                        true
                    );
                }
                return false;
            }
        }
        return super.canPlace(context, state);
    }
}
