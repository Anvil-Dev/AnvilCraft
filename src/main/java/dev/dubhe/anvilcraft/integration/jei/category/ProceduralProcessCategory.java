package dev.dubhe.anvilcraft.integration.jei.category;

import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.block.WipBlock;
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
    private static final int STEPS_LENGTH = 120;
    private static final int STEP_X = (WIDTH - STEPS_LENGTH) / 2 + 10;
    private static final int STEP_LENGTH = 20;
    private static final int ITEM_Y = 14;
    private static final int BLOCK_Y = 36;

    private final IDrawable icon;
    private final IDrawable slot;
    private final IDrawable longArrow;
    private final IDrawable cycle;
    private final Component title;

    public ProceduralProcessCategory(IGuiHelper helper) {
        this.icon = helper.createDrawableItemStack(new ItemStack(Items.ANVIL));
        this.slot = JeiRenderHelper.getSlotDefault(helper);
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
        renderPredicate(graphics, recipe.getInitialBlock(), holder, STEP_X - 20, BLOCK_Y, 18);

        int visibleSteps = Math.min(recipe.getSteps().size(), MAX_VISIBLE_STEPS);
        for (int index = 0; index < visibleSteps; index++) {
            ProceduralProcessStep step = recipe.getSteps().get(index);
            if (!(step.getContent() instanceof AbstractProcessRecipe<?> process)) continue;
            int x = stepX(index, visibleSteps);
            RenderSupport.renderBlock(graphics, Blocks.ANVIL.defaultBlockState(), x - 10, 3, 20);
            if (!process.getInputItems().isEmpty()) {
                this.slot.draw(graphics, x - 9, ITEM_Y);
            }
            List<BlockStatePredicate> inputBlocks = process.getInputBlocks();
            for (int inputIndex = inputBlocks.size() - 1; inputIndex >= 0; inputIndex--) {
                renderPredicate(
                    graphics,
                    inputBlocks.get(inputIndex),
                    holder,
                    x - 9,
                    BLOCK_Y + inputIndex * 10,
                    18
                );
            }
        }

        this.longArrow.draw(graphics, WIDTH / 2 - 32, BLOCK_Y + 20);
        if (recipe.getLoop() > 1) {
            this.cycle.draw(graphics, WIDTH / 2 + 52, BLOCK_Y + 14);
            AgeratumUtil.renderText(
                graphics,
                Component.literal(String.valueOf(recipe.getLoop())),
                WIDTH / 2 + 68,
                BLOCK_Y + 18,
                1.2F
            );
        }
        RenderSupport.renderBlock(graphics, recipe.getResultBlock().state(), 142, BLOCK_Y, 20);
    }

    private static int stepX(int index, int visibleSteps) {
        int gap = STEPS_LENGTH / Math.max(visibleSteps, 1) - STEP_LENGTH;
        return STEP_X + gap / 2 + index * (STEP_LENGTH + gap);
    }

    private static void renderPredicate(
        GuiGraphicsExtractor graphics,
        BlockStatePredicate predicate,
        RecipeHolder<ProceduralProcessRecipe> holder,
        int x,
        int y,
        int size
    ) {
        List<BlockState> states = predicate.constructStatesForRender();
        if (states.isEmpty()) return;
        int index = (int) ((System.currentTimeMillis() / 1000) % states.size());
        BlockState state = states.get(index);
        if (state.getBlock() instanceof WipBlock) {
            RenderSupport.renderWipBlock(graphics, holder.id().identifier(), x, y, size);
        } else {
            RenderSupport.renderBlock(graphics, state, x, y, size);
        }
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
