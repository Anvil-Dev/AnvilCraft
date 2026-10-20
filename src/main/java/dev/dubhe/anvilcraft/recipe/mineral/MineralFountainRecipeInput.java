package dev.dubhe.anvilcraft.recipe.mineral;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

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
