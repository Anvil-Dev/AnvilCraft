package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.api.itemhandler.IItemHandlerHolder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class ChuteBlockItem extends BlockItem {
    public ChuteBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        return level.getBlockEntity(pos) instanceof IItemHandlerHolder || level.getCapability(
            Capabilities.ItemHandler.BLOCK, context.getClickedPos(), context.getClickedFace()
        ) != null ? this.useOn(context) : super.onItemUseFirst(stack, context);
    }
}
