package dev.dubhe.anvilcraft.integration.jei.category.anvil.liquid;

import dev.dubhe.anvilcraft.block.HeaterBlock;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.drawable.DrawableBlockStateIcon;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.SuperHeatingRecipe;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class SuperHeatingCategory extends AbstractLiquidCategory<SuperHeatingRecipe> {
    public SuperHeatingCategory(IGuiHelper helper) {
        super(
            helper,
            new DrawableBlockStateIcon(
                Blocks.CAULDRON.defaultBlockState(),
                ModBlocks.HEATER.getDefaultState().setValue(HeaterBlock.OVERLOAD, false)
            ),
            Component.translatable("gui.anvilcraft.category.super_heating")
        );
    }

    @Override
    public RecipeType<RecipeHolder<SuperHeatingRecipe>> getRecipeType() {
        return AnvilCraftJeiPlugin.SUPER_HEATING;
    }

    @Override
    protected BlockState getProcessBlock() {
        return ModBlocks.HEATER.getDefaultState().setValue(HeaterBlock.OVERLOAD, false);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        super.registerRecipeCatalysts(registration);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.HEATER), getRecipeType());
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.BURNING_HEATER), getRecipeType());
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(
            AnvilCraftJeiPlugin.SUPER_HEATING,
            JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.SUPER_HEATING_TYPE.get())
        );
    }
}
