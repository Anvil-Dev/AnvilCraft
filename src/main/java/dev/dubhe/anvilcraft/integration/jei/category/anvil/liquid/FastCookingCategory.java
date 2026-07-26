package dev.dubhe.anvilcraft.integration.jei.category.anvil.liquid;

import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.drawable.DrawableBlockStateIcon;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.FastCookingRecipe;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.types.IRecipeHolderType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;

public class FastCookingCategory extends AbstractLiquidCategory<FastCookingRecipe> {
    public FastCookingCategory(IGuiHelper helper) {
        super(
            helper,
            new DrawableBlockStateIcon(
                Blocks.CAULDRON.defaultBlockState(),
                Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true)
            ),
            Component.translatable("gui.anvilcraft.category.fast_cooking")
        );
    }

    @Override
    public IRecipeHolderType<FastCookingRecipe> getRecipeType() {
        return AnvilCraftJeiPlugin.FAST_COOKING;
    }

    @Override
    protected BlockState getProcessBlock() {
        return Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        super.registerRecipeCatalysts(registration);
        registration.addCraftingStation(getRecipeType(), Items.CAMPFIRE);
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(
            AnvilCraftJeiPlugin.FAST_COOKING,
            JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.FAST_COOKING.get())
        );
    }
}
