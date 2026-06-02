package dev.dubhe.anvilcraft.block.item;

import dev.dubhe.anvilcraft.block.TradingStationBlock;
import dev.dubhe.anvilcraft.block.entity.TradingStationBlockEntity;
import dev.dubhe.anvilcraft.block.state.DirectionVertical2PartHalf;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class TradingStationBlockItem extends FlexibleMultiPartBlockItem<DirectionVertical2PartHalf, DirectionProperty, Direction> {
    public TradingStationBlockItem(TradingStationBlock block, Properties properties) {
        super(block, properties);
    }

    @Override
    protected boolean updateCustomBlockEntityTag(BlockPos pos, Level level, @Nullable Player player, ItemStack stack, BlockState state) {
        boolean resultSuper = super.updateCustomBlockEntityTag(pos, level, player, stack, state);
        if (player == null) return resultSuper;
        Optional<TradingStationBlockEntity> be = level.getBlockEntity(pos, ModBlockEntities.TRADING_STATION.get());
        if (be.isEmpty()) return resultSuper;
        be.get().setOwner(player.getGameProfile().getId());
        return true;
    }
}
