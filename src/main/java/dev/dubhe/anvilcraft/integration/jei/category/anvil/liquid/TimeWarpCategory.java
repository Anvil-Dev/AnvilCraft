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
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

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
    public RecipeType<RecipeHolder<TimeWarpRecipe>> getRecipeType() {
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
    public void setRecipe(
        IRecipeLayoutBuilder builder,
        RecipeHolder<TimeWarpRecipe> recipeHolder,
        IFocusGroup focuses
    ) {
        super.setRecipe(builder, recipeHolder, focuses);
        TimeWarpCategory.addGemLookup(builder, recipeHolder.value());
    }

    /**
     * 宝石在宝石转化配方里是原料，按 R 查询宝石时看不到对应的宝石块配方。
     * 故把宝石额外登记成仅用于查询的产物，使「宝石 → 宝石块」也能被 R 检索到。
     */
    private static void addGemLookup(IRecipeLayoutBuilder builder, TimeWarpRecipe recipe) {
        if (!TimeWarpCategory.usesMeltGem(recipe)) return;
        List<ItemIngredientPredicate> inputs = recipe.getInputItems();
        if (inputs.isEmpty()) return;
        IIngredientAcceptor<?> lookup = builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT);
        for (ItemIngredientPredicate input : inputs) {
            lookup.addItemStacks(List.of(input.getItems()));
        }
    }

    private static boolean usesMeltGem(TimeWarpRecipe recipe) {
        return recipe.getHasCauldron()
            .fluid()
            .fluids()
            .map(fluids -> fluids.stream().anyMatch(holder -> holder.value() == ModFluids.MELT_GEM.get()))
            .orElse(false);
    }

    @Override
    public void getTooltip(
        ITooltipBuilder tooltip,
        RecipeHolder<TimeWarpRecipe> recipeHolder,
        IRecipeSlotsView recipeSlotsView,
        double mouseX,
        double mouseY
    ) {
        super.getTooltip(tooltip, recipeHolder, recipeSlotsView, mouseX, mouseY);
        if (this.isProcessBlockHovered(recipeHolder.value(), mouseX, mouseY)) {
            tooltip.add(ModBlocks.CORRUPTED_BEACON.get().getName());
            tooltip.add(Component.translatable("gui.anvilcraft.category.time_warp.need_activated")
                .withStyle(ChatFormatting.RED));
        }
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        AnvilCraftJeiPlugin.addAnvilCauldronCatalysts(registration, AnvilCraftJeiPlugin.TIME_WARP);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.CORRUPTED_BEACON), AnvilCraftJeiPlugin.TIME_WARP);
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(
            AnvilCraftJeiPlugin.TIME_WARP,
            JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.TIME_WARP_TYPE.get())
        );
    }
}
