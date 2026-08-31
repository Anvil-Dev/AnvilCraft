package dev.dubhe.anvilcraft.integration.jei.category.anvil;

import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.anvilcraft.lib.v2.util.predicate.ChanceBlockState;
import dev.anvilcraft.lib.v2.util.predicate.ChanceItemStack;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.util.JeiBlockIngredientUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiFluidUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.recipe.anvil.predicate.block.HasCauldron;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.SqueezingRecipe;
import dev.dubhe.anvilcraft.recipe.component.HasCauldronSimple;
import dev.dubhe.anvilcraft.util.TooltipUtil;
import mezz.jei.api.gui.ITickTimer;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeHolderType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * 挤压配方类别 - 使用 3D 方块渲染展示输入/输出方块，流体使用 JEI 流体格子展示。
 */
public class SqueezingCategory implements IRecipeCategory<RecipeHolder<SqueezingRecipe>> {
    private static final String INPUT_BLOCK = "input_block";
    private static final String OUTPUT_BLOCK = "output_block";
    private static final String OUTPUT_FLUID = "output_fluid";

    public static final int WIDTH = 162;
    public static final int HEIGHT = 64;
    public static final int FLUID_X = 125;
    public static final int FLUID_Y = 46;

    private final IDrawable slotDefault;
    private final IDrawable arrowDefault;
    private final IDrawable icon;
    private final ITickTimer timer;
    private final Component title;

    public SqueezingCategory(IGuiHelper helper) {
        this.arrowDefault = JeiRenderHelper.getArrowDefault(helper);
        this.slotDefault = JeiRenderHelper.getSlotDefault(helper);
        this.icon = helper.createDrawableItemStack(new ItemStack(Items.ANVIL));
        this.title = Component.translatable("gui.anvilcraft.category.squeezing");
        this.timer = helper.createTickTimer(30, 60, true);
    }

    @Override
    public IRecipeHolderType<SqueezingRecipe> getRecipeType() {
        return AnvilCraftJeiPlugin.SQUEEZING;
    }

    @Override
    public Component getTitle() {
        return this.title;
    }

    @Override
    public int getWidth() {
        return SqueezingCategory.WIDTH;
    }

    @Override
    public int getHeight() {
        return SqueezingCategory.HEIGHT;
    }

    @Override
    public IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public void setRecipe(
        IRecipeLayoutBuilder builder,
        RecipeHolder<SqueezingRecipe> recipeHolder,
        IFocusGroup focuses
    ) {
        SqueezingRecipe recipe = recipeHolder.value();

        if (!recipe.getInputBlocks().isEmpty()) {
            List<ItemStack> inputs = recipe.getInputBlocks().stream()
                .flatMap(input -> input.getBlocks().stream())
                .map(block -> new ItemStack(block.value()))
                .toList();
            JeiBlockIngredientUtil.addSlot(builder, RecipeIngredientRole.INPUT, SqueezingCategory.INPUT_BLOCK, 40, 22, 18, 18, inputs);
        }

        if (!recipe.getResultBlocks().isEmpty()) {
            List<ItemStack> outputs = recipe.getResultBlocks().stream()
                .map(result -> new ItemStack(result.state().getBlock()))
                .toList();
            JeiBlockIngredientUtil.addSlot(builder, RecipeIngredientRole.OUTPUT, SqueezingCategory.OUTPUT_BLOCK, 100, 22, 20, 18, outputs);
        }
        for (ChanceItemStack output : recipe.getResultItems()) {
            builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT)
                .add(output.stack().withCount(output.getMaxCount()));
        }
        JeiFluidUtil.addOutputSlot(
            builder, SqueezingCategory.OUTPUT_FLUID, SqueezingCategory.FLUID_X, SqueezingCategory.FLUID_Y, 16, 16, recipe.getHasCauldron());
    }

    @Override
    public void createRecipeExtras(
        IRecipeExtrasBuilder builder,
        RecipeHolder<SqueezingRecipe> recipeHolder,
        IFocusGroup focuses
    ) {
        JeiBlockIngredientUtil.suppressHoverOverlays(builder);
    }

    @Override
    public void getTooltip(
        ITooltipBuilder tooltip,
        RecipeHolder<SqueezingRecipe> recipeHolder,
        IRecipeSlotsView recipeSlotsView,
        double mouseX,
        double mouseY
    ) {
        SqueezingRecipe recipe = recipeHolder.value();
        if (mouseX >= 40 && mouseX <= 58 && mouseY >= 20 && mouseY <= 38) {
            tooltip.addAll(
                TooltipUtil.tooltip(recipe.getInputBlocks().getFirst().constructStatesForRender().getFirst().getBlock())
            );
        }
        if (mouseX >= 100 && mouseX <= 120 && mouseY >= 20 && mouseY <= 38) {
            List<ChanceBlockState> result = recipe.getResultBlocks();
            if (result.isEmpty()) return;
            tooltip.addAll(
                TooltipUtil.tooltip(
                    result.get((int) ((System.currentTimeMillis() / 1000) % result.size())).state().getBlock()
                )
            );
        }
    }

    @Override
    public void draw(
        RecipeHolder<SqueezingRecipe> recipeHolder,
        IRecipeSlotsView recipeSlotsView,
        GuiGraphicsExtractor graphics,
        double mouseX,
        double mouseY
    ) {
        SqueezingRecipe recipe = recipeHolder.value();
        
        RenderSupport.renderBlock(graphics, Blocks.CAULDRON.defaultBlockState(), 40, 35, 20);

        List<BlockState> input = new ArrayList<>();
        for (BlockStatePredicate predicate : recipe.getInputBlocks()) {
            input.addAll(predicate.constructStatesForRender());
        }
        if (input.isEmpty()) return;
        BlockState renderedState = JeiBlockIngredientUtil.getDisplayedState(recipeSlotsView, SqueezingCategory.INPUT_BLOCK, input)
            .orElse(input.getFirst());
        RenderSupport.renderBlock(graphics, renderedState, 40, 25, 20);
        
        int anvilYOffset = JeiRenderHelper.getAnvilAnimationOffset(this.timer);
        RenderSupport.renderBlock(graphics, Blocks.ANVIL.defaultBlockState(), 40, 7 + anvilYOffset, 20);

        this.arrowDefault.draw(graphics, 73, 28);

        HasCauldronSimple cauldronFluid = recipe.getHasCauldron();
        if (HasCauldron.isNotEmpty(cauldronFluid.transform())) {
            this.slotDefault.draw(graphics, SqueezingCategory.FLUID_X - 1, SqueezingCategory.FLUID_Y - 1);
        }

        List<ChanceBlockState> result = recipe.getResultBlocks();
        if (result.isEmpty()) return;
        List<BlockState> resultStates = result.stream().map(ChanceBlockState::state).toList();
        renderedState = JeiBlockIngredientUtil.getDisplayedState(recipeSlotsView, SqueezingCategory.OUTPUT_BLOCK, resultStates)
            .orElse(resultStates.getFirst());
        RenderSupport.renderBlock(graphics, renderedState, 100, 25, 20);
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(
            AnvilCraftJeiPlugin.SQUEEZING,
            JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.SQUEEZING.get())
        );
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        AnvilCraftJeiPlugin.addAnvilCauldronCatalysts(registration, AnvilCraftJeiPlugin.SQUEEZING);
    }
}
