package dev.dubhe.anvilcraft.integration.jei.category.anvil;

import dev.anvilcraft.lib.v2.util.MathUtil;
import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.anvilcraft.lib.v2.util.predicate.ChanceBlockState;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.recipe.anvil.predicate.block.HasCauldron;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.SqueezingRecipe;
import dev.dubhe.anvilcraft.util.CauldronUtil;
import dev.dubhe.anvilcraft.util.TooltipUtil;
import mezz.jei.api.gui.ITickTimer;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeHolderType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SqueezingCategory implements IRecipeCategory<RecipeHolder<SqueezingRecipe>> {
    public static final int WIDTH = 162;
    public static final int HEIGHT = 64;

    private final IDrawable arrowDefault;
    private final IDrawable icon;
    private final ITickTimer timer;
    private final Component title;

    public SqueezingCategory(IGuiHelper helper) {
        this.arrowDefault = JeiRenderHelper.getArrowDefault(helper);
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
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public void setRecipe(
        IRecipeLayoutBuilder builder, RecipeHolder<SqueezingRecipe> recipeHolder, IFocusGroup focuses) {
        SqueezingRecipe recipe = recipeHolder.value();
        JeiRecipeUtil.addInvisibleInputs(builder, recipe.getInputBlocks());
        builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT)
            .addItemStacks(recipe.getResultItems().stream().map(stack -> stack.stack().create()).toList());
    }

    @Override
    public void draw(
        RecipeHolder<SqueezingRecipe> recipeHolder,
        IRecipeSlotsView view,
        GuiGraphicsExtractor graphics,
        double mouseX,
        double mouseY
    ) {
        SqueezingRecipe recipe = recipeHolder.value();
        int anvilYOffset = JeiRenderHelper.getAnvilAnimationOffset(this.timer);
        RenderSupport.renderBlock(graphics, Blocks.ANVIL.defaultBlockState(), 50, 12 + anvilYOffset, 20);

        List<BlockState> input = new ArrayList<>();
        for (BlockStatePredicate predicate : recipe.getInputBlocks()) {
            input.addAll(predicate.constructStatesForRender());
        }
        if (input.isEmpty()) return;
        BlockState renderedState = input.get((int) ((System.currentTimeMillis() / 1000) % input.size()));
        if (renderedState == null) return;
        RenderSupport.renderBlock(graphics, renderedState, 50, 30, 20);
        RenderSupport.renderBlock(graphics, Blocks.CAULDRON.defaultBlockState(), 50, 40, 20);

        this.arrowDefault.draw(graphics, 73, 35);

        RenderSupport.renderBlock(graphics, Blocks.ANVIL.defaultBlockState(), 110, 20, 20);
        RenderSupport.renderBlock(graphics, getCauldron(recipe), 110, 40, 20);
        List<ChanceBlockState> result = recipe.getResultBlocks();
        if (result.isEmpty()) return;
        renderedState = result.get((int) ((System.currentTimeMillis() / 1000) % result.size())).state();
        RenderSupport.renderBlock(graphics, renderedState, 110, 30, 20);
    }

    @Override
    public void getTooltip(
        ITooltipBuilder tooltip,
        RecipeHolder<SqueezingRecipe> recipeHolder,
        IRecipeSlotsView view,
        double mouseX,
        double mouseY
    ) {
        SqueezingRecipe recipe = recipeHolder.value();
        Identifier id = getIdentifier(recipeHolder);
        if (MathUtil.isInRange(mouseX, 40, 58)) {
            if (MathUtil.isInRange(mouseY, 24, 42)) {
                tooltip.addAll(TooltipUtil.tooltip(recipe.getInputBlocks().getFirst().constructStatesForRender().getFirst().getBlock()));
            }
            if (MathUtil.isInRange(mouseY, 42, 52)) {
                tooltip.addAll(TooltipUtil.tooltip(Blocks.CAULDRON));
            }
        }
        if (MathUtil.isInRange(mouseX, 100, 120)) {
            if (MathUtil.isInRange(mouseY, 24, 42)) {
                List<ChanceBlockState> result = recipe.getResultBlocks();
                if (result.isEmpty()) return;
                tooltip.addAll(
                    TooltipUtil.tooltip(result.get((int) ((System.currentTimeMillis() / 1000) % result.size())).state().getBlock())
                );
            }
            if (MathUtil.isInRange(mouseY, 42, 52)) {
                Block block = recipe.getHasCauldron().getTransformCauldron();
                if (id != null) {
                    tooltip.addAll(TooltipUtil.recipeIDTooltip(block, id));
                } else {
                    tooltip.addAll(TooltipUtil.tooltip(block));
                }
            }
        }
    }

    static BlockState getCauldron(SqueezingRecipe recipe) {
        if (recipe.isProduceFluid()) {
            return Blocks.CAULDRON.defaultBlockState();
        } else {
            return CauldronUtil.fullState(HasCauldron.getDefaultCauldron(recipe.getHasCauldron().transform()));
        }
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(
            AnvilCraftJeiPlugin.SQUEEZING,
            JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.SQUEEZING.get())
        );
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        AnvilCraftJeiPlugin.addAnvilProcessingCatalysts(registration, AnvilCraftJeiPlugin.SQUEEZING);
        AnvilCraftJeiPlugin.addCauldronCatalysts(registration, AnvilCraftJeiPlugin.SQUEEZING);
    }
}
