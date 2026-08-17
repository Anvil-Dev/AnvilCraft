package dev.dubhe.anvilcraft.integration.jei.category;

import dev.anvilcraft.lib.v2.util.predicate.ChanceItemStack;
import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.GiantAnvilBlock;
import dev.dubhe.anvilcraft.block.LargeCauldronBlock;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import dev.dubhe.anvilcraft.block.state.GiantAnvilCube;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.recipe.ComplexFluidJeiRecipe;
import dev.dubhe.anvilcraft.integration.jei.recipe.LiquidEnchantmentJeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiFluidUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiItemUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.recipe.FluidMixingRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.SolidLiquidRecipe;
import mezz.jei.api.gui.ITickTimer;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SolidLiquidCategory implements IRecipeCategory<RecipeHolder<FluidMixingRecipe>> {
    public static final int WIDTH = 162;
    public static final int HEIGHT = 64;
    private static final float MODEL_SCALE = 7.5F;
    private static final int HEATER_INPUT_GRID_SIZE = 4;
    private static final SlotPosition HEATER_POSITION = new SlotPosition(100, 41);
    private static final Component HEATER_ACTIVE = Component.translatable(
        "gui.anvilcraft.category.super_heating.need_activated"
    ).withStyle(ChatFormatting.GOLD);

    private final IDrawable icon;
    private final IDrawable slot;
    private final IDrawable arrowIn;
    private final IDrawable arrowOut;
    private final ITickTimer timer;
    private final BlockState largeCauldron;
    private final BlockState giantAnvil;

    public SolidLiquidCategory(IGuiHelper helper) {
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
    public RecipeType<RecipeHolder<FluidMixingRecipe>> getRecipeType() {
        return AnvilCraftJeiPlugin.SOLID_LIQUID;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("gui.anvilcraft.category.solid_liquid");
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
        if (recipe instanceof ComplexFluidJeiRecipe complexRecipe) {
            setComplexRecipe(builder, complexRecipe);
        } else {
            setFluidMixingRecipe(builder, recipe);
        }
    }

    private static void setFluidMixingRecipe(IRecipeLayoutBuilder builder, FluidMixingRecipe recipe) {
        List<SizedFluidIngredient> ingredients = recipe.getFluidIngredients();
        for (int index = 0; index < ingredients.size(); index++) {
            SizedFluidIngredient ingredient = ingredients.get(index);
            SlotPosition position = inputPosition(ingredients.size(), index);
            JeiFluidUtil.addFluidSlot(
                builder,
                RecipeIngredientRole.INPUT,
                position.x() + 1,
                position.y() + 1,
                16,
                16,
                ingredient.amount(),
                false,
                List.of(ingredient.getFluids())
            );
        }

        List<ItemStack> itemResults = recipe.getItemResults();
        List<FluidStack> fluidResults = recipe.getFluidResults();
        boolean splitOutputColumns = !itemResults.isEmpty() && !fluidResults.isEmpty();
        for (int index = 0; index < itemResults.size(); index++) {
            SlotPosition position = itemOutputPosition(itemResults.size(), index, splitOutputColumns);
            builder.addSlot(RecipeIngredientRole.OUTPUT, position.x() + 1, position.y() + 1)
                .addItemStack(itemResults.get(index).copy());
        }
        for (int index = 0; index < fluidResults.size(); index++) {
            FluidStack fluid = fluidResults.get(index);
            SlotPosition position = fluidOutputPosition(fluidResults.size(), index, splitOutputColumns);
            JeiFluidUtil.addFluidSlot(
                builder,
                RecipeIngredientRole.OUTPUT,
                position.x() + 1,
                position.y() + 1,
                16,
                16,
                fluid.getAmount(),
                true,
                List.of(fluid)
            );
        }
    }

    private static void setComplexRecipe(IRecipeLayoutBuilder builder, ComplexFluidJeiRecipe recipe) {
        List<List<FluidStack>> fluidInputs = recipe.getDisplayFluidInputs();
        List<ItemIngredientPredicate> itemInputs = recipe.getInputItems();
        int inputCount = fluidInputs.size() + itemInputs.size();
        int inputGridSize = recipe.isHeaterRequired() ? HEATER_INPUT_GRID_SIZE : inputCount;
        List<IRecipeSlotBuilder> fluidInputSlots = new ArrayList<>(fluidInputs.size());
        for (int index = 0; index < fluidInputs.size(); index++) {
            SlotPosition position = inputPosition(inputGridSize, index);
            fluidInputSlots.add(addFluidSlot(
                builder,
                RecipeIngredientRole.INPUT,
                position,
                fluidInputs.get(index),
                false
            ));
        }
        for (int index = 0; index < itemInputs.size(); index++) {
            SlotPosition position = inputPosition(inputGridSize, fluidInputs.size() + index);
            JeiItemUtil.addSlotWithCount(
                builder,
                position.x() + 1,
                position.y() + 1,
                itemInputs.get(index)
            );
        }
        if (recipe.isHeaterRequired()) {
            builder.addSlot(
                    RecipeIngredientRole.CATALYST,
                    HEATER_POSITION.x() + 1,
                    HEATER_POSITION.y() + 1
                ).addItemStacks(List.of(ModBlocks.HEATER.asStack(), ModBlocks.BURNING_HEATER.asStack()))
                .addRichTooltipCallback((slotView, tooltip) -> tooltip.add(HEATER_ACTIVE));
        }

        List<ChanceItemStack> itemResults = recipe.getDisplayItemResults();
        List<List<FluidStack>> fluidResults = recipe.getDisplayFluidResults();
        boolean splitOutputColumns = !itemResults.isEmpty() && !fluidResults.isEmpty();
        for (int index = 0; index < itemResults.size(); index++) {
            SlotPosition position = itemOutputPosition(itemResults.size(), index, splitOutputColumns);
            JeiItemUtil.addOutputSlot(
                builder,
                position.x() + 1,
                position.y() + 1,
                itemResults.get(index)
            );
        }
        List<IRecipeSlotBuilder> fluidOutputSlots = new ArrayList<>(fluidResults.size());
        for (int index = 0; index < fluidResults.size(); index++) {
            SlotPosition position = fluidOutputPosition(fluidResults.size(), index, splitOutputColumns);
            fluidOutputSlots.add(addFluidSlot(
                builder,
                RecipeIngredientRole.OUTPUT,
                position,
                fluidResults.get(index),
                true
            ));
        }
        if (recipe.shouldLinkFluidVariants()
            && !fluidInputSlots.isEmpty()
            && !fluidOutputSlots.isEmpty()) {
            builder.createFocusLink(fluidInputSlots.getLast(), fluidOutputSlots.getFirst());
        }
    }

    private static IRecipeSlotBuilder addFluidSlot(
        IRecipeLayoutBuilder builder,
        RecipeIngredientRole role,
        SlotPosition position,
        List<FluidStack> fluids,
        boolean showCapacity
    ) {
        int amount = fluids.getFirst().getAmount();
        return JeiFluidUtil.addFluidSlot(
            builder,
            role,
            position.x() + 1,
            position.y() + 1,
            16,
            16,
            amount,
            showCapacity,
            fluids
        );
    }

    @Override
    public void draw(
        RecipeHolder<FluidMixingRecipe> recipeHolder,
        IRecipeSlotsView recipeSlotsView,
        GuiGraphics guiGraphics,
        double mouseX,
        double mouseY
    ) {
        FluidMixingRecipe recipe = recipeHolder.value();
        int itemOutputCount;
        int fluidOutputCount;
        if (recipe instanceof ComplexFluidJeiRecipe complexRecipe) {
            int inputCount = complexRecipe.getDisplayFluidInputCount() + complexRecipe.getInputItems().size();
            int inputGridSize = complexRecipe.isHeaterRequired() ? HEATER_INPUT_GRID_SIZE : inputCount;
            for (int index = 0; index < inputCount; index++) {
                SlotPosition position = inputPosition(inputGridSize, index);
                this.slot.draw(guiGraphics, position.x(), position.y());
            }
            if (complexRecipe.isHeaterRequired()) {
                this.slot.draw(guiGraphics, HEATER_POSITION.x(), HEATER_POSITION.y());
            }
            itemOutputCount = complexRecipe.getDisplayItemResults().size();
            fluidOutputCount = complexRecipe.getDisplayFluidResultCount();
        } else {
            int inputCount = recipe.getFluidIngredients().size();
            for (int index = 0; index < inputCount; index++) {
                SlotPosition position = inputPosition(inputCount, index);
                this.slot.draw(guiGraphics, position.x(), position.y());
            }
            itemOutputCount = recipe.getItemResults().size();
            fluidOutputCount = recipe.getFluidResults().size();
        }

        boolean splitOutputColumns = itemOutputCount > 0 && fluidOutputCount > 0;
        for (int index = 0; index < itemOutputCount; index++) {
            SlotPosition position = itemOutputPosition(itemOutputCount, index, splitOutputColumns);
            this.slot.draw(guiGraphics, position.x(), position.y());
        }
        for (int index = 0; index < fluidOutputCount; index++) {
            SlotPosition position = fluidOutputPosition(fluidOutputCount, index, splitOutputColumns);
            this.slot.draw(guiGraphics, position.x(), position.y());
        }

        this.arrowIn.draw(guiGraphics, 47, 30);
        this.arrowOut.draw(guiGraphics, 99, 29);

        float anvilYOffset = JeiRenderHelper.getAnvilAnimationOffset(this.timer) / 3.0F;
        RenderSupport.renderBlock(
            guiGraphics,
            this.giantAnvil,
            81,
            23 + anvilYOffset,
            20,
            MODEL_SCALE,
            RenderSupport.SINGLE_BLOCK
        );
        RenderSupport.renderBlock(
            guiGraphics,
            this.largeCauldron,
            81,
            45,
            10,
            MODEL_SCALE,
            RenderSupport.SINGLE_BLOCK
        );
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
        List<RecipeHolder<FluidMixingRecipe>> recipes = new ArrayList<>();
        for (RecipeHolder<FluidMixingRecipe> holder
            : JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.FLUID_MIXING_TYPE.get())) {
            recipes.add(holder);
        }
        for (RecipeHolder<SolidLiquidRecipe> holder
            : JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.SOLID_LIQUID_TYPE.get())) {
            recipes.add(new RecipeHolder<>(
                holder.id(),
                ComplexFluidJeiRecipe.fromSolidLiquid(holder.value())
            ));
        }

        var enchantments = LiquidEnchantmentJeiRecipeUtil.getEnchantments(false);
        if (!enchantments.isEmpty()) {
            recipes.add(new RecipeHolder<>(
                AnvilCraft.of("jei/solid_liquid/liquid_enchantment_assimilation"),
                ComplexFluidJeiRecipe.assimilation(enchantments)
            ));
            recipes.add(new RecipeHolder<>(
                AnvilCraft.of("jei/solid_liquid/liquid_enchantment_cleanse"),
                ComplexFluidJeiRecipe.cleanse(enchantments)
            ));
        }
        var curses = LiquidEnchantmentJeiRecipeUtil.getEnchantments(true);
        if (!curses.isEmpty()) {
            recipes.add(new RecipeHolder<>(
                AnvilCraft.of("jei/solid_liquid/cursed_gold_ingot"),
                ComplexFluidJeiRecipe.curseGoldIngot(curses)
            ));
            recipes.add(new RecipeHolder<>(
                AnvilCraft.of("jei/solid_liquid/cursed_gold_block"),
                ComplexFluidJeiRecipe.curseGoldBlock(curses)
            ));
        }
        recipes.add(new RecipeHolder<>(
            AnvilCraft.of("jei/solid_liquid/enchanted_gold_ingot"),
            ComplexFluidJeiRecipe.enchantGoldIngot()
        ));
        registration.addRecipes(AnvilCraftJeiPlugin.SOLID_LIQUID, recipes);
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(ModBlocks.LARGE_CAULDRON.asStack(), AnvilCraftJeiPlugin.SOLID_LIQUID);
        registration.addRecipeCatalyst(ModBlocks.GIANT_ANVIL.asStack(), AnvilCraftJeiPlugin.SOLID_LIQUID);
    }

    private record SlotPosition(int x, int y) {
    }
}
