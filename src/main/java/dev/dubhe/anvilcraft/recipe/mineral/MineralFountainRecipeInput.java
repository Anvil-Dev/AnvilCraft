package dev.dubhe.anvilcraft.recipe.mineral;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * 矿物涌泉配方的匹配输入。
 *
 * @param from   上方（被转化）的方块
 * @param around 四个水平相邻方块
 * @param pos    矿物涌泉所在位置，用于判定高度与维度
 */
public record MineralFountainRecipeInput(BlockState from, List<BlockState> around, BlockPos pos)
    implements RecipeInput {
    public MineralFountainRecipeInput {
        around = List.copyOf(around);
    }

    @Override
    public ItemStack getItem(int index) {
        return new ItemStack(this.from.getBlock());
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }
}
