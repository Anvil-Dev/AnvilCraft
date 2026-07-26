package dev.dubhe.anvilcraft.integration.jei.category;

import dev.dubhe.anvilcraft.block.LargeCauldronBlock;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import dev.dubhe.anvilcraft.block.state.GiantAnvilCube;
import dev.dubhe.anvilcraft.block.workstation.GiantAnvilBlock;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.recipe.FluidMixingRecipe;
import mezz.jei.api.gui.ITickTimer;
import mezz.jei.api.gui.builder.IIngredientAcceptor;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class FluidMixingCategory implements IRecipeCategory<RecipeHolder<FluidMixingRecipe>> {
    public static final int WIDTH = 162;
    public static final int HEIGHT = 64;
    private static final float MODEL_SCALE = 7.5F;

    private final IDrawable icon;
    private final IDrawable slot;
    private final IDrawable arrowIn;
    private final IDrawable arrowOut;
    private final ITickTimer timer;
    private final BlockState largeCauldron;
    private final BlockState giantAnvil;

    public FluidMixingCategory(IGuiHelper helper) {
        this.icon = helper.createDrawableItemStack(ModBlocks.LARGE_CAULDRON.asStack());
        this.slot = JeiRenderHelper.getSlotDefault(helper);
        this.arrowIn = JeiRenderHelper.getArrowInput(helper);
        this.arrowOut = JeiRenderHelper.getArrowOutput(helper);
        this.timer = helper.createTickTimer(30, 60, true);
        this.largeCauldron = ModBlocks.LARGE_CAULDRON.getDefaultState()
            .setValue(LargeCauldronBlock.HALF, Cube3x3PartHalf.MID_CENTER);
        this.giantAnvil = ModBlocks.GIANT_ANVIL.getDefaultState()
            .setValue(GiantAnvilBlock.HALF, Cube3x3PartHalf.MID_CENTER)
            .setValue(GiantAnvilBlock.CUBE, GiantAnvilCube.CENTER);
    }

    @Override
    public IRecipeHolderType<FluidMixingRecipe> getRecipeType() {
        return AnvilCraftJeiPlugin.FLUID_MIXING;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("gui.anvilcraft.category.fluid_mixing");
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
        IRecipeLayoutBuilder builder,
        RecipeHolder<FluidMixingRecipe> recipeHolder,
        IFocusGroup focuses
    ) {
        FluidMixingRecipe recipe = recipeHolder.value();

        // Input Fluid
        List<SizedFluidIngredient> ingredients = recipe.getFluidIngredients();
        IIngredientAcceptor<?> bucketIngredients = builder.addInvisibleIngredients(RecipeIngredientRole.INPUT);
        for (int index = 0; index < ingredients.size(); index++) {
            SizedFluidIngredient ingredient = ingredients.get(index);
            SlotPosition position = inputPosition(ingredients.size(), index);
            IRecipeSlotBuilder recipeSlot = builder.addSlot(
                RecipeIngredientRole.INPUT,
                position.x() + 1,
                position.y() + 1
            ).setFluidRenderer(ingredient.amount(), false, 16, 16);
            for (var fluidHolder : ingredient.ingredient().fluids()) {
                Item bucket = fluidHolder.value().getBucket();
                recipeSlot.add(fluidHolder.value(), ingredient.amount());
                if (bucket != Items.AIR) bucketIngredients.add(new ItemStack(bucket));
            }
        }

        List<ItemStack> itemResults = recipe.getItemResults();
        List<FluidStack> fluidResults = recipe.getFluidResults();
        boolean splitOutputColumns = !itemResults.isEmpty() && !fluidResults.isEmpty();
        for (int index = 0; index < itemResults.size(); index++) {
            SlotPosition position = itemOutputPosition(itemResults.size(), index, splitOutputColumns);
            builder.addSlot(RecipeIngredientRole.OUTPUT, position.x() + 1, position.y() + 1)
                .add(itemResults.get(index).copy());
        }
        IIngredientAcceptor<?> outputBuckets = builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT);
        for (int index = 0; index < fluidResults.size(); index++) {
            FluidStack fluid = fluidResults.get(index);
            SlotPosition position = fluidOutputPosition(fluidResults.size(), index, splitOutputColumns);
            builder.addSlot(RecipeIngredientRole.OUTPUT, position.x() + 1, position.y() + 1)
                .setFluidRenderer(fluid.getAmount(), true, 16, 16)
                .add(fluid.getFluid(), fluid.getAmount(), fluid.getComponentsPatch());
            Item bucket = fluid.getFluid().getBucket();
            if (bucket != Items.AIR) outputBuckets.add(new ItemStack(bucket));
        }
    }

    @Override
    public void draw(
        RecipeHolder<FluidMixingRecipe> recipeHolder,
        IRecipeSlotsView recipeSlotsView,
        GuiGraphicsExtractor guiGraphics,
        double mouseX,
        double mouseY
    ) {
        FluidMixingRecipe recipe = recipeHolder.value();
        for (int index = 0; index < recipe.getFluidIngredients().size(); index++) {
            SlotPosition position = inputPosition(recipe.getFluidIngredients().size(), index);
            this.slot.draw(guiGraphics, position.x(), position.y());
        }
        for (int index = 0; index < recipe.getItemResults().size(); index++) {
            SlotPosition position = itemOutputPosition(
                recipe.getItemResults().size(),
                index,
                !recipe.getItemResults().isEmpty() && !recipe.getFluidResults().isEmpty()
            );
            this.slot.draw(guiGraphics, position.x(), position.y());
        }
        for (int index = 0; index < recipe.getFluidResults().size(); index++) {
            SlotPosition position = fluidOutputPosition(
                recipe.getFluidResults().size(),
                index,
                !recipe.getItemResults().isEmpty() && !recipe.getFluidResults().isEmpty()
            );
            this.slot.draw(guiGraphics, position.x(), position.y());
        }

        this.arrowIn.draw(guiGraphics, 47, 30);
        this.arrowOut.draw(guiGraphics, 99, 29);

        float anvilYOffset = JeiRenderHelper.getAnvilAnimationOffset(this.timer) / 3.0F;
        RenderSupport.renderBlock(guiGraphics, this.giantAnvil, 71, 13 + anvilYOffset, MODEL_SCALE * 2);
        RenderSupport.renderBlock(guiGraphics, this.largeCauldron, 71, 35, MODEL_SCALE * 2);
    }

    private static SlotPosition inputPosition(int count, int index) {
        if (count == 1) return new SlotPosition(15, 23);
        if (count == 2) return new SlotPosition(6 + index * 19, 23);
        if (count == 3) {
            return index == 0
                ? new SlotPosition(15, 14)
                : new SlotPosition(6 + (index - 1) * 19, 33);
        }
        return new SlotPosition(6 + index % 2 * 19, 14 + index / 2 * 19);
    }

    private static SlotPosition itemOutputPosition(int count, int index, boolean splitColumns) {
        return new SlotPosition(splitColumns ? 119 : 129, outputRow(count, index));
    }

    private static SlotPosition fluidOutputPosition(int count, int index, boolean splitColumns) {
        return new SlotPosition(splitColumns ? 138 : 129, outputRow(count, index));
    }

    private static int outputRow(int count, int index) {
        if (count == 1) return 24;
        if (count == 2) return 14 + index * 19;
        return 5 + index * 19;
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(
            AnvilCraftJeiPlugin.FLUID_MIXING,
            JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.FLUID_MIXING.get())
        );
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addCraftingStation(
            AnvilCraftJeiPlugin.FLUID_MIXING,
            ModBlocks.LARGE_CAULDRON,
            ModBlocks.GIANT_ANVIL
        );
    }

    private record SlotPosition(int x, int y) {
    }
}
