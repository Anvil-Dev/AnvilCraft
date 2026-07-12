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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;
import java.util.List;

public class ProceduralProcessCategory implements IRecipeCategory<RecipeHolder<ProceduralProcessRecipe>> {
    public static final int WIDTH = 162;
    public static final int HEIGHT = 70;
    private static final int MAX_VISIBLE_STEPS = 5;
    private static final int STEPS_START_X = 38;
    private static final int STEPS_WIDTH = 86;
    private static final int ITEM_Y = 25;
    private static final int BLOCK_Y = 46;

    private final IDrawable icon;
    private final IDrawable slot;
    private final IDrawable arrow;
    private final IDrawable longArrow;
    private final IDrawable cycle;
    private final Component title;

    public ProceduralProcessCategory(IGuiHelper helper) {
        this.icon = helper.createDrawableItemStack(new ItemStack(Items.ANVIL));
        this.slot = JeiRenderHelper.getSlotDefault(helper);
        this.arrow = JeiRenderHelper.getArrowDefault(helper);
        this.longArrow = JeiRenderHelper.getArrowLong(helper);
        this.cycle = JeiRenderHelper.getCycle(helper);
        this.title = Component.translatable("gui.anvilcraft.category.procedural_process");
    }

    @Override
    public IRecipeHolderType<ProceduralProcessRecipe> getRecipeType() {
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
    public IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public void setRecipe(
        IRecipeLayoutBuilder builder,
        RecipeHolder<ProceduralProcessRecipe> holder,
        IFocusGroup focuses
    ) {
        ProceduralProcessRecipe recipe = holder.value();
        JeiRecipeUtil.addInvisibleInput(builder, recipe.getInitialBlock());
        JeiRecipeUtil.addInvisibleOutput(builder, recipe.getResultBlock());

        int visibleSteps = Math.min(recipe.getSteps().size(), MAX_VISIBLE_STEPS);
        for (int index = 0; index < visibleSteps; index++) {
            ProceduralProcessStep step = recipe.getSteps().get(index);
            if (!(step.getContent() instanceof AbstractProcessRecipe<?> process)) continue;
            JeiRecipeUtil.addInvisibleInputs(builder, process.getInputBlocks());
            if (process.getInputItems().isEmpty()) continue;

            ItemIngredientPredicate ingredient = process.getInputItems().getFirst();
            IRecipeSlotBuilder slotBuilder = builder.addSlot(
                RecipeIngredientRole.INPUT,
                stepX(index, visibleSteps) - 8,
                ITEM_Y + 1
            );
            slotBuilder.addItemStacks(
                Arrays.stream(ingredient.getItems()).map(ItemStackTemplate::create).toList()
            );
        }
    }

    @Override
    public void draw(
        RecipeHolder<ProceduralProcessRecipe> holder,
        IRecipeSlotsView view,
        GuiGraphicsExtractor graphics,
        double mouseX,
        double mouseY
    ) {
        ProceduralProcessRecipe recipe = holder.value();
        renderPredicate(graphics, recipe.getInitialBlock(), 5, BLOCK_Y, 16);
        this.arrow.draw(graphics, 20, BLOCK_Y + 2);

        int visibleSteps = Math.min(recipe.getSteps().size(), MAX_VISIBLE_STEPS);
        for (int index = 0; index < visibleSteps; index++) {
            ProceduralProcessStep step = recipe.getSteps().get(index);
            if (!(step.getContent() instanceof AbstractProcessRecipe<?> process)) continue;
            int x = stepX(index, visibleSteps);
            RenderSupport.renderBlock(graphics, Blocks.ANVIL.defaultBlockState(), x - 8, 4, 16);
            if (!process.getInputItems().isEmpty()) {
                this.slot.draw(graphics, x - 9, ITEM_Y);
            }
            List<BlockStatePredicate> inputBlocks = process.getInputBlocks();
            for (int inputIndex = inputBlocks.size() - 1; inputIndex >= 0; inputIndex--) {
                int offset = Math.min(inputIndex, 2);
                renderPredicate(
                    graphics,
                    inputBlocks.get(inputIndex),
                    x - 7 + offset * 2,
                    BLOCK_Y - offset * 5,
                    14
                );
            }
        }

        this.longArrow.draw(graphics, 49, 60);
        if (recipe.getLoop() > 1) {
            this.cycle.draw(graphics, 4, 4);
            AgeratumUtil.renderText(
                graphics,
                Component.literal(String.valueOf(recipe.getLoop())),
                21,
                8
            );
        }
        this.arrow.draw(graphics, 126, BLOCK_Y + 2);
        RenderSupport.renderBlock(graphics, recipe.getResultBlock().state(), 145, BLOCK_Y, 16);
    }

    private static int stepX(int index, int visibleSteps) {
        if (visibleSteps <= 1) return STEPS_START_X + STEPS_WIDTH / 2;
        return STEPS_START_X + index * STEPS_WIDTH / (visibleSteps - 1);
    }

    private static void renderPredicate(
        GuiGraphicsExtractor graphics,
        BlockStatePredicate predicate,
        int x,
        int y,
        int size
    ) {
        List<BlockState> states = predicate.constructStatesForRender();
        if (states.isEmpty()) return;
        int index = (int) ((System.currentTimeMillis() / 1000) % states.size());
        RenderSupport.renderBlock(graphics, states.get(index), x, y, size);
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
}
