package dev.dubhe.anvilcraft.recipe.multiblock;

import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public interface IMultiblockRecipe extends Recipe<MultiblockInput> {
    void assemble(Level level, BlockPos landPos, BlockPos inputCorner, MultiblockInput ctx);

    @Override
    default ItemStack assemble(MultiblockInput input, HolderLookup.Provider registries) {
        return this.getResultItem(registries);
    }

    boolean isValidCenterBlock(Level level, BlockPos pos, BlockState state);

    @Override
    default boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    default boolean isSpecial() {
        return true;
    }

    @Override
    default ItemStack getToastSymbol() {
        return ModBlocks.GIANT_ANVIL.asStack();
    }
}
