package dev.dubhe.anvilcraft.integration.jei.category.anvil.liquid;

import dev.dubhe.anvilcraft.block.power.consumer.HeaterBlock;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.drawable.DrawableBlockStateIcon;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.SuperHeatingRecipe;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.types.IRecipeHolderType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class SuperHeatingCategory extends AbstractLiquidCategory<SuperHeatingRecipe> {
    public SuperHeatingCategory(IGuiHelper helper) {
        super(
            helper,
            DrawableBlockStateIcon.alternatingBoth(
                Blocks.CAULDRON.defaultBlockState(),
                ModBlocks.HEATER.getDefaultState().setValue(HeaterBlock.OVERLOAD, false),
                ModBlocks.FISH_TANK.getDefaultState(),
                ModBlocks.BURNING_HEATER.getDefaultState()
            ),
            Component.translatable("gui.anvilcraft.category.super_heating")
        );
    }

    @Override
    public IRecipeHolderType<SuperHeatingRecipe> getRecipeType() {
        return AnvilCraftJeiPlugin.SUPER_HEATING;
    }

    /** 电加热器与燃烧加热器都能完成超加热，因此轮换展示。 */
    @Override
    protected BlockState getProcessBlock() {
        return JeiRenderHelper.alternatingState(
            ModBlocks.HEATER.getDefaultState().setValue(HeaterBlock.OVERLOAD, false),
            ModBlocks.BURNING_HEATER.getDefaultState()
        );
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        super.registerRecipeCatalysts(registration);
        registration.addCraftingStation(this.getRecipeType(), ModBlocks.HEATER);
        registration.addCraftingStation(this.getRecipeType(), ModBlocks.BURNING_HEATER);
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(
            AnvilCraftJeiPlugin.SUPER_HEATING,
            JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.SUPER_HEATING.get())
        );
    }
}
