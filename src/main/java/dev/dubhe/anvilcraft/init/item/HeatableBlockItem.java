package dev.dubhe.anvilcraft.init.item;

import dev.dubhe.anvilcraft.api.heat.HeatRecorder;
import dev.dubhe.anvilcraft.block.heatable.HeatableBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class HeatableBlockItem extends BlockItem {
    public HeatableBlockItem(HeatableBlock block, Properties properties) {
        super(block, properties);
    }

    @Override
    protected @Nullable BlockState getPlacementState(BlockPlaceContext context) {
        return Optional.ofNullable(this.getBlock().getStateForPlacement(context))
            .flatMap(state -> HeatRecorder.getPrevTierHeatableBlock(context.getLevel(), context.getClickedPos(), state))
            .map(block -> block.getStateForPlacement(context))
            .filter(state -> this.canPlace(context, state))
            .orElse(super.getPlacementState(context));
    }
}
