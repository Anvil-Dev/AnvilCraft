package dev.dubhe.anvilcraft.integration.jei.category.anvil;

import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.integration.jei.util.TextureConstants;
import dev.dubhe.anvilcraft.recipe.anvil.util.BlockStatePredicate;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.SqueezingRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.components.ChanceBlockState;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.components.ChanceItemStack;
import dev.dubhe.anvilcraft.util.CauldronUtil;
import dev.dubhe.anvilcraft.util.RenderHelper;
import mezz.jei.api.gui.ITickTimer;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class SqueezingCategory implements IRecipeCategory<RecipeHolder<SqueezingRecipe>> {
    public static final int WIDTH = 162;
    public static final int HEIGHT = 64;

    private final IDrawable progress;
    private final IDrawable icon;
    private final ITickTimer timer;
    private final Component title;

    public SqueezingCategory(IGuiHelper helper) {
        progress = helper.drawableBuilder(TextureConstants.PROGRESS, 0, 0, 24, 16)
            .setTextureSize(24, 16)
            .build();
        icon = helper.createDrawableItemStack(new ItemStack(Items.ANVIL));
        title = Component.translatable("gui.anvilcraft.category.squeezing");
        timer = helper.createTickTimer(30, 60, true);
    }

    @Override
    public RecipeType<RecipeHolder<SqueezingRecipe>> getRecipeType() {
        return AnvilCraftJeiPlugin.SQUEEZING;
    }

    @Override
    public Component getTitle() {
        return title;
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
        return icon;
    }

    @Override
    public void setRecipe(
        IRecipeLayoutBuilder builder, RecipeHolder<SqueezingRecipe> recipeHolder, IFocusGroup focuses) {
        SqueezingRecipe recipe = recipeHolder.value();
        for (BlockStatePredicate input : recipe.getInputBlocks()) {
            builder.addInvisibleIngredients(RecipeIngredientRole.INPUT)
                .addIngredients(Ingredient.of(
                    input.getBlocks().stream().map(holder -> new ItemStack(holder.value())).toArray(ItemStack[]::new)));
        }
        for (ChanceItemStack output : recipe.getResultItems()) {
            builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT)
                .addItemStack(output.getStack().copyWithCount(output.getMaxCount()));
        }
    }

    @Override
    public void getTooltip(
        ITooltipBuilder tooltip,
        RecipeHolder<SqueezingRecipe> recipeHolder,
        IRecipeSlotsView recipeSlotsView,
        double mouseX,
        double mouseY) {
        SqueezingRecipe recipe = recipeHolder.value();
        if (mouseX >= 40 && mouseX <= 58) {
            if (mouseY >= 24 && mouseY <= 42) {
                tooltip.add(recipe.getInputBlocks().getFirst().constructStatesForRender().getFirst().getBlock().getName());
            }
            if (mouseY >= 42 && mouseY <= 52) {
                tooltip.add(Blocks.CAULDRON.getName());
            }
        }
        if (mouseX >= 100 && mouseX <= 120) {
            if (mouseY >= 24 && mouseY <= 42) {
                List<ChanceBlockState> result = recipe.getResultBlocks();
                if (result.isEmpty()) return;
                tooltip.add(result.get((int) ((System.currentTimeMillis() / 1000) % result.size())).getState().getBlock().getName());
            }
            if (mouseY >= 42 && mouseY <= 52) {
                tooltip.add(BuiltInRegistries.BLOCK.get(recipe.getHasCauldron().getFluid().withSuffix("_cauldron")).getName());
            }
        }
    }

    @Override
    public void draw(
        RecipeHolder<SqueezingRecipe> recipeHolder,
        IRecipeSlotsView recipeSlotsView,
        GuiGraphics guiGraphics,
        double mouseX,
        double mouseY) {
        SqueezingRecipe recipe = recipeHolder.value();
        float anvilYOffset = JeiRenderHelper.getAnvilAnimationOffset(timer);
        RenderHelper.renderBlock(
            guiGraphics,
            Blocks.ANVIL.defaultBlockState(),
            50,
            12 + anvilYOffset,
            20,
            12,
            RenderHelper.SINGLE_BLOCK
        );

        List<BlockState> input = new ArrayList<>();
        for (BlockStatePredicate predicate : recipe.getInputBlocks()) {
            input.addAll(predicate.constructStatesForRender());
        }
        if (input.isEmpty()) return;
        BlockState renderedState = input.get((int) ((System.currentTimeMillis() / 1000) % input.size()));
        if (renderedState == null) return;
        RenderHelper.renderBlock(guiGraphics, renderedState, 50, 30, 10, 12, RenderHelper.SINGLE_BLOCK);
        RenderHelper.renderBlock(guiGraphics, Blocks.CAULDRON.defaultBlockState(), 50, 40, 0, 12, RenderHelper.SINGLE_BLOCK);

        progress.draw(guiGraphics, 69, 30);

        RenderHelper.renderBlock(guiGraphics, Blocks.ANVIL.defaultBlockState(), 110, 20, 20, 12, RenderHelper.SINGLE_BLOCK);
        RenderHelper.renderBlock(guiGraphics, getCauldron(recipe), 110, 40, 0, 12, RenderHelper.SINGLE_BLOCK);
        List<ChanceBlockState> result = recipe.getResultBlocks();
        if (result.isEmpty()) return;
        renderedState = result.get((int) ((System.currentTimeMillis() / 1000) % result.size())).getState();
        RenderHelper.renderBlock(guiGraphics, renderedState, 110, 30, 10, 12, RenderHelper.SINGLE_BLOCK);
    }

    static BlockState getCauldron(SqueezingRecipe recipe) {
        if (recipe.isProduceFluid()) {
            return Blocks.CAULDRON.defaultBlockState();
        } else {
            return CauldronUtil.fullState(BuiltInRegistries.BLOCK.get(recipe.getHasCauldron().getTransform().withSuffix("_cauldron")));
        }
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(
            AnvilCraftJeiPlugin.SQUEEZING,
            JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.SQUEEZING_TYPE.get()));
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(Items.ANVIL), AnvilCraftJeiPlugin.SQUEEZING);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.ROYAL_ANVIL), AnvilCraftJeiPlugin.SQUEEZING);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.EMBER_ANVIL), AnvilCraftJeiPlugin.SQUEEZING);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.GIANT_ANVIL), AnvilCraftJeiPlugin.SQUEEZING);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.SPECTRAL_ANVIL), AnvilCraftJeiPlugin.SQUEEZING);
        registration.addRecipeCatalyst(new ItemStack(Items.CAULDRON), AnvilCraftJeiPlugin.SQUEEZING);
    }
}
