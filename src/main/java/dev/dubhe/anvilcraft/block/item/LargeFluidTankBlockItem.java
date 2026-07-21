package dev.dubhe.anvilcraft.block.item;

import dev.dubhe.anvilcraft.api.tooltip.FluidTankItemTooltip;
import dev.dubhe.anvilcraft.block.LargeFluidTankBlock;
import dev.dubhe.anvilcraft.block.entity.LargeFluidTankBlockEntity;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class LargeFluidTankBlockItem extends SimpleMultiPartBlockItem<Cube3x3PartHalf> {
    public LargeFluidTankBlockItem(LargeFluidTankBlock block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(
        ItemStack stack,
        Item.TooltipContext context,
        List<Component> tooltipComponents,
        TooltipFlag tooltipFlag
    ) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        FluidTankItemTooltip.appendMultiTank(
            stack,
            context,
            tooltipComponents,
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
        // The initially placed part is not the tank's data-owning center part.
        return false;
    }
}
