package dev.dubhe.anvilcraft.integration.jei.category.anvil.liquid;

import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.drawable.DrawableBlockStateIcon;
import dev.dubhe.anvilcraft.integration.jei.recipe.ComplexFluidJeiRecipe;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.SolidLiquidRecipe;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

public class SolidLiquidCategory extends AbstractLiquidCategory<SolidLiquidRecipe> {
    public SolidLiquidCategory(IGuiHelper helper) {
        super(
            helper,
            new DrawableBlockStateIcon(
                Blocks.ANVIL.defaultBlockState(),
                Blocks.WATER_CAULDRON.defaultBlockState()
            ),
            Component.translatable("gui.anvilcraft.category.solid_liquid")
        );
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        AnvilCraftJeiPlugin.addAnvilCauldronCatalysts(registration, AnvilCraftJeiPlugin.SOLID_LIQUID);
    }

    @Override
    public RecipeType<RecipeHolder<SolidLiquidRecipe>> getRecipeType() {
        return AnvilCraftJeiPlugin.SOLID_LIQUID;
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        List<RecipeHolder<SolidLiquidRecipe>> recipes = JeiRecipeUtil
            .getRecipeHoldersFromType(ModRecipeTypes.SOLID_LIQUID_TYPE.get())
            .stream()
            .filter(holder -> !ComplexFluidJeiRecipe.isComplex(holder.value()))
            .toList();
        registration.addRecipes(AnvilCraftJeiPlugin.SOLID_LIQUID, recipes);
    }
}
