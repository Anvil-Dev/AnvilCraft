package dev.dubhe.anvilcraft.recipe.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public interface IMultiblockRecipe extends Recipe<MultiblockInput> {
    void assemble(Level level, BlockPos landPos, BlockPos inputCorner, MultiblockInput input);

    @Override
    default ItemStack assemble(MultiblockInput input) {
        return this.getResultItem().copy();
    }

    boolean isValidCenterBlock(Level level, BlockPos pos, BlockState state);

    ItemStack getResultItem();

    @Override
    default boolean isSpecial() {
        return true;
    }

    @Override
    default boolean showNotification() {
        return false;
    }

    @Override
    default String group() {
        return "";
    }

    @Override
    default PlacementInfo placementInfo() {
        return PlacementInfo.createFromOptionals(List.of());
    }

    @Override
    default RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }
}
