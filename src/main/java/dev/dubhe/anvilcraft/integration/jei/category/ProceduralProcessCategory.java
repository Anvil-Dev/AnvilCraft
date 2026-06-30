package dev.dubhe.anvilcraft.integration.jei.category;

import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.recipe.anvil.procedural.ProceduralProcessRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.procedural.ProceduralProcessStep;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.AbstractProcessRecipe;
import dev.dubhe.anvilcraft.util.AgeratumUtil;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
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
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ProceduralProcessCategory implements IRecipeCategory<RecipeHolder<ProceduralProcessRecipe>> {
    public static final int WIDTH = 162;
    public static final int HEIGHT = 64;

    public static final int STEPS_LENGTH = 120;
    public static final int STEP_X = (WIDTH - STEPS_LENGTH) / 2 + 10;
    public static final int STEP_Y = 4;
    public static final int STEP_LENGTH = 20;

    public static final int ANVIL_Y = STEP_Y;
    public static final int ITEM_Y = STEP_Y + 10;
    public static final int BLOCK_Y = STEP_Y + 32;

    private final IDrawable slotDefault;
    private final IDrawable cycle;
    private final IDrawable arrowLong;
    private final IDrawable icon;
    private final Component title;

    public ProceduralProcessCategory(IGuiHelper helper) {
        this.slotDefault = JeiRenderHelper.getSlotDefault(helper);
        this.cycle = JeiRenderHelper.getCycle(helper);
        this.arrowLong = JeiRenderHelper.getArrowLong(helper);
        this.icon = helper.createDrawableItemLike(Blocks.ANVIL);
        this.title = Component.translatable("gui.anvilcraft.category.procedural_process");
    }

    @Override
    public RecipeType<RecipeHolder<ProceduralProcessRecipe>> getRecipeType() {
        return AnvilCraftJeiPlugin.PROCEDURAL_PROCESS;
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
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<ProceduralProcessRecipe> recipeHolder, IFocusGroup focuses) {
        ProceduralProcessRecipe recipe = recipeHolder.value();

        // input
        builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).addIngredients(Ingredient.of(
            recipe.getInitialBlock().getBlocks().stream().map(holder -> new ItemStack(holder.value())).toArray(ItemStack[]::new)));

        // step
        int size = Math.clamp(recipe.getSteps().size(), 1, 5);
        int gap = STEPS_LENGTH / size - STEP_LENGTH;
        int stepX = STEP_X + gap / 2;
        int stepDx = STEP_LENGTH + gap;

        for (int i = 0; i < size; i++) {
            ProceduralProcessStep step = recipe.getSteps().get(i);
            if (!(step.getContent() instanceof AbstractProcessRecipe<?> stepRecipe)) continue;

            if (!stepRecipe.getInputItems().isEmpty()) {
                ItemIngredientPredicate ingredient = stepRecipe.getInputItems().getFirst();
                IRecipeSlotBuilder slot = builder.addSlot(RecipeIngredientRole.INPUT, stepX + i * stepDx - 8, ITEM_Y + 1);
                slot.addIngredients(Ingredient.of(ingredient.getItems()));
            }

            for (BlockStatePredicate inputBlock : stepRecipe.getInputBlocks()) {
                builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).addIngredients(Ingredient.of(
                    inputBlock.getBlocks().stream().map(holder -> new ItemStack(holder.value())).toArray(ItemStack[]::new)));
            }
        }

        // output
        builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT)
            .addItemStack(new ItemStack(recipe.getResultBlock().state().getBlock()));
    }

    @Override
    public void draw(
        RecipeHolder<ProceduralProcessRecipe> recipeHolder,
        IRecipeSlotsView recipeSlotsView,
        GuiGraphics guiGraphics,
        double mouseX,
        double mouseY
    ) {
        ProceduralProcessRecipe recipe = recipeHolder.value();

        // input
        HolderSet<Block> blocks = recipe.getInitialBlock().getBlocks();
        Holder<Block> blockHolder = blocks.get((int) ((System.currentTimeMillis() / 1000) % blocks.size()));
        BlockState blockState = blockHolder.value().defaultBlockState();
        RenderSupport.renderBlock(guiGraphics, blockState, STEP_X - 20, BLOCK_Y, 10, 12, RenderSupport.SINGLE_BLOCK);


        // step
        int size = Math.clamp(recipe.getSteps().size(), 1, 5);
        int gap = STEPS_LENGTH / size - STEP_LENGTH;
        int stepX = STEP_X + gap / 2;
        int stepDx = STEP_LENGTH + gap;

        for (int i = 0; i < recipe.getSteps().size(); i++) {
            ProceduralProcessStep step = recipe.getSteps().get(i);
            if (!(step.getContent() instanceof AbstractProcessRecipe<?> stepRecipe)) continue;

            // anvil
            RenderSupport.renderBlock(
                guiGraphics,
                Blocks.ANVIL.defaultBlockState(),
                stepX + i * stepDx,
                ANVIL_Y,
                20,
                12,
                RenderSupport.SINGLE_BLOCK
            );

            // item

            if (!stepRecipe.getInputItems().isEmpty()) {
                this.slotDefault.draw(guiGraphics, stepX + i * stepDx - 9, ITEM_Y);
            }

            // block
            for (int j = stepRecipe.getInputBlocks().size() - 1; j >= 0; j--) {
                List<BlockState> input = stepRecipe.getInputBlocks().get(j).constructStatesForRender();
                if (input.isEmpty()) continue;
                BlockState renderedState = input.get((int) ((System.currentTimeMillis() / 1000) % input.size()));
                if (renderedState == null) continue;
                RenderSupport.renderBlock(
                    guiGraphics,
                    renderedState,
                    stepX + i * stepDx,
                    BLOCK_Y + 10 * j,
                    10 - 10 * j,
                    12,
                    RenderSupport.SINGLE_BLOCK
                );
            }
        }

        // loop
        if (recipe.getLoop() > 1) {
            Component text = Component.literal(String.valueOf(recipe.getLoop())).withColor(0xFFFFFF);
            AgeratumUtil.renderText(guiGraphics, text, WIDTH / 2 + 68, BLOCK_Y + 18, 1.2f);
            this.cycle.draw(guiGraphics, WIDTH / 2 + 52, BLOCK_Y + 14);
        }
        this.arrowLong.draw(guiGraphics, WIDTH / 2 - 32, BLOCK_Y + 20);

        // result
        RenderSupport.renderBlock(
            guiGraphics, recipe.getResultBlock().state(), STEP_X + STEPS_LENGTH, BLOCK_Y, 0, 12, RenderSupport.SINGLE_BLOCK
        );
    }

    @Override
    public void getTooltip(
        ITooltipBuilder tooltip,
        RecipeHolder<ProceduralProcessRecipe> recipe,
        IRecipeSlotsView recipeSlotsView,
        double mouseX,
        double mouseY
    ) {
        IRecipeCategory.super.getTooltip(tooltip, recipe, recipeSlotsView, mouseX, mouseY);
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(
            AnvilCraftJeiPlugin.PROCEDURAL_PROCESS,
            JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.PROCEDURAL_PROCESS.get())
        );
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        AnvilCraftJeiPlugin.addAnvilProcessingCatalysts(registration, AnvilCraftJeiPlugin.BOILING);
    }
}
