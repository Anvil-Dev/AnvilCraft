package dev.dubhe.anvilcraft.integration.jei.category.anvil.liquid;

import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.block.ModFluids;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.drawable.DrawableBlockStateIcon;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.TimeWarpRecipe;
import mezz.jei.api.gui.builder.IIngredientAcceptor;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.types.IRecipeHolderType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.Arrays;
import java.util.List;

public class TimeWarpCategory extends AbstractLiquidCategory<TimeWarpRecipe> {
    public TimeWarpCategory(IGuiHelper helper) {
        super(
            helper,
            new DrawableBlockStateIcon(
                Blocks.CAULDRON.defaultBlockState(),
                ModBlocks.CORRUPTED_BEACON
                    .get()
                    .defaultBlockState()
                    .trySetValue(BlockStateProperties.WATERLOGGED, false)
            ),
            Component.translatable("gui.anvilcraft.category.time_warp")
        );
    }

    @Override
    public IRecipeHolderType<TimeWarpRecipe> getRecipeType() {
        return AnvilCraftJeiPlugin.TIME_WARP;
    }

    @Override
    protected BlockState getProcessBlock() {
        return ModBlocks.CORRUPTED_BEACON
            .get()
            .defaultBlockState()
            .trySetValue(BlockStateProperties.WATERLOGGED, false);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<TimeWarpRecipe> recipeHolder, IFocusGroup focuses) {
        super.setRecipe(builder, recipeHolder, focuses);
        TimeWarpRecipe recipe = recipeHolder.value();
        if (!recipe.getHasCauldron().fluid().fluids()
            .map(fluids -> fluids.stream().anyMatch(holder -> holder.value() == ModFluids.MELT_GEM.get())).orElse(false)) {
            return;
        }
        List<ItemIngredientPredicate> inputs = recipe.getInputItems();
        if (inputs.isEmpty()) return;
        IIngredientAcceptor<?> lookup = builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT);
        for (ItemIngredientPredicate input : inputs) {
            lookup.addItemStacks(Arrays.stream(input.getItems()).map(ItemStackTemplate::create).toList());
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        super.registerRecipeCatalysts(registration);
        registration.addCraftingStation(this.getRecipeType(), ModBlocks.CORRUPTED_BEACON);
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(
            AnvilCraftJeiPlugin.TIME_WARP,
            JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.TIME_WARP.get())
        );
    }
}
