package dev.dubhe.anvilcraft.item.block;

import dev.dubhe.anvilcraft.api.tooltip.FluidTankItemTooltip;
import dev.dubhe.anvilcraft.api.tooltip.providers.IItemTooltipProvider;
import dev.dubhe.anvilcraft.block.container.LargeFluidTankBlock;
import dev.dubhe.anvilcraft.block.entity.LargeFluidTankBlockEntity;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/// 大型流体储罐的物品形态，额外显示罐内流体
public class LargeFluidTankBlockItem extends SimpleMultiPartBlockItem<Cube3x3PartHalf> implements IItemTooltipProvider {
    public LargeFluidTankBlockItem(LargeFluidTankBlock block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendItemTooltip(
        ItemStack stack,
        Item.TooltipContext context,
        TooltipDisplay display,
        Consumer<Component> builder,
        TooltipFlag tooltipFlag
    ) {
        FluidTankItemTooltip.appendMultiTank(
            stack,
            context,
            builder,
            LargeFluidTankBlockEntity.BASE_CAPACITY
        );
    }

    @Override
    protected boolean updateCustomBlockEntityTag(
        BlockPos pos,
        Level level,
        @Nullable Player player,
        ItemStack stack,
        BlockState state
    ) {
        // 3x3x3 储罐的方块实体在主控位置，放置逻辑由方块自身在 setPlacedBy 里写入
        return false;
    }
}
