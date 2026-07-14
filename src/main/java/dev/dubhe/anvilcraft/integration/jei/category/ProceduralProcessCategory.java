package dev.dubhe.anvilcraft.integration.jei.category;

import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.block.WipBlock;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.util.JeiBlockIngredientUtil;
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
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ProceduralProcessCategory implements IRecipeCategory<RecipeHolder<ProceduralProcessRecipe>> {
    private static final String INITIAL_BLOCK = "initial_block";
    public static final int WIDTH = 162;
    public static final int HEIGHT = 90;

    public static final int STEPS_LENGTH = 120;
    public static final int STEP_X = (WIDTH - STEPS_LENGTH) / 2 + 10;
    public static final int STEP_Y = 4;
    public static final int STEP_LENGTH = 20;

    public static final int ANVIL_Y = STEP_Y;
    public static final int ITEM_Y = 20;
    public static final int BLOCK_Y = 50;
    public static final int FLOW_Y = 72;

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
        JeiBlockIngredientUtil.addInputSlot(
            builder, INITIAL_BLOCK, STEP_X - 29, BLOCK_Y - 6, 18, 18, recipe.getInitialBlock()
        );

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

            for (int j = 0; j < stepRecipe.getInputBlocks().size(); j++) {
                int y = j == 0 ? BLOCK_Y - 6 : BLOCK_Y + 12 + 10 * (j - 1);
                int height = j == 0 ? 18 : 10;
                JeiBlockIngredientUtil.addInputSlot(
                    builder,
                    stepBlockSlotName(i, j),
                    stepX + i * stepDx - 9,
                    y,
                    18,
                    height,
                    stepRecipe.getInputBlocks().get(j)
                );
            }
        }

        // output
        JeiBlockIngredientUtil.addSlot(
            builder,
            RecipeIngredientRole.OUTPUT,
            "output_block",
            STEP_X + STEPS_LENGTH - 9,
            BLOCK_Y - 6,
            18,
            18,
            recipe.getResultBlock().state().getBlock()
        );
    }

    @Override
    public void createRecipeExtras(
        IRecipeExtrasBuilder builder, RecipeHolder<ProceduralProcessRecipe> recipeHolder, IFocusGroup focuses) {
        JeiBlockIngredientUtil.suppressHoverOverlays(builder);
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
        List<BlockState> initialStates = recipe.getInitialBlock().constructStatesForRender();
        JeiBlockIngredientUtil.getDisplayedState(recipeSlotsView, INITIAL_BLOCK, initialStates).ifPresent(blockState ->
            RenderSupport.renderBlock(
                guiGraphics, blockState, STEP_X - 20, BLOCK_Y, 10, 12, RenderSupport.SINGLE_BLOCK
            )
        );


        // step
        int size = Math.clamp(recipe.getSteps().size(), 1, 5);
        int gap = STEPS_LENGTH / size - STEP_LENGTH;
        int stepX = STEP_X + gap / 2;
        int stepDx = STEP_LENGTH + gap;

        for (int i = 0; i < size; i++) {
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
                BlockState renderedState = JeiBlockIngredientUtil
                    .getDisplayedState(recipeSlotsView, stepBlockSlotName(i, j), input)
                    .orElse(input.getFirst());
                if (renderedState.getBlock() instanceof WipBlock) {
                    RenderSupport.renderBlock(
                        guiGraphics,
                        renderedState,
                        stepX + i * stepDx,
                        BLOCK_Y + 10 * j,
                        10 - 10 * j,
                        12,
                        RenderSupport.wipEntity(recipeHolder.id())
                    );
                }
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
            AgeratumUtil.renderText(guiGraphics, text, WIDTH / 2 + 68, FLOW_Y + 4, 1.2f);
            this.cycle.draw(guiGraphics, WIDTH / 2 + 47, FLOW_Y);
        }
        this.arrowLong.draw(guiGraphics, WIDTH / 2 - 32, FLOW_Y + 4);

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
        AnvilCraftJeiPlugin.addAnvilProcessingCatalysts(registration, AnvilCraftJeiPlugin.PROCEDURAL_PROCESS);
    }

    private static String stepBlockSlotName(int step, int block) {
        return "step_" + step + "_block_" + block;
    }
}
