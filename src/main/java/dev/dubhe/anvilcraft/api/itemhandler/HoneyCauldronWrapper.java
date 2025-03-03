package dev.dubhe.anvilcraft.api.itemhandler;

import dev.dubhe.anvilcraft.block.HoneyCauldronBlock;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class HoneyCauldronWrapper implements IItemHandler {
    private final Level level;
    private final BlockPos pos;

    public HoneyCauldronWrapper(Level level, BlockPos pos) {
        this.level = level;
        this.pos = pos;
    }

    @Override
    public int getSlots() {
        return 1;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        if (slot == 0) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof HoneyCauldronBlock && level.getBlockState(pos).getValue(HoneyCauldronBlock.LEVEL) == 4) {
                return new ItemStack(Items.HONEY_BLOCK, 1);
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return stack;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount <= 0) return ItemStack.EMPTY;
        if (slot == 0) {
            ItemStack stackInSlot = getStackInSlot(slot);
            if (!stackInSlot.isEmpty()) {
                if (simulate) {
                    return stackInSlot.copyWithCount(1);
                } else {
                    ItemStack copy = stackInSlot.copy();
                    stackInSlot.shrink(1);
                    updateWorld();
                    return copy;
                }
            } else {
                return ItemStack.EMPTY;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
        return 1;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return false;
    }

    private void updateWorld() {
        level.setBlockAndUpdate(pos, Blocks.CAULDRON.defaultBlockState());
    }
}
