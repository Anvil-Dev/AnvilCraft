package dev.dubhe.anvilcraft.integration.jei.category;

import dev.anvilcraft.lib.v2.util.predicate.ChanceItemStack;
import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.LargeCauldronBlock;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import dev.dubhe.anvilcraft.block.state.GiantAnvilCube;
import dev.dubhe.anvilcraft.block.workstation.GiantAnvilBlock;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.recipe.ComplexFluidJeiRecipe;
import dev.dubhe.anvilcraft.integration.jei.recipe.LiquidEnchantmentJeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiItemUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.recipe.FluidMixingRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.SolidLiquidRecipe;
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
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class FluidMixingCategory implements IRecipeCategory<RecipeHolder<FluidMixingRecipe>> {
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
        return FluidMixingCategory.WIDTH;
    }

    @Override
    public int getHeight() {
        return FluidMixingCategory.HEIGHT;
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
            FluidMixingCategory.setComplexRecipe(builder, complexRecipe);
        } else {
            FluidMixingCategory.setFluidMixingRecipe(builder, recipe);
        }
    }

    private static void setFluidMixingRecipe(IRecipeLayoutBuilder builder, FluidMixingRecipe recipe) {
        List<SizedFluidIngredient> ingredients = recipe.getFluidIngredients();
        IIngredientAcceptor<?> bucketIngredients = builder.addInvisibleIngredients(RecipeIngredientRole.INPUT);
        for (int index = 0; index < ingredients.size(); index++) {
            SizedFluidIngredient ingredient = ingredients.get(index);
            SlotPosition position = FluidMixingCategory.inputPosition(ingredients.size(), index);
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
            SlotPosition position = FluidMixingCategory.itemOutputPosition(itemResults.size(), index, splitOutputColumns);
            builder.addSlot(RecipeIngredientRole.OUTPUT, position.x() + 1, position.y() + 1)
                .add(itemResults.get(index).copy());
        }
        IIngredientAcceptor<?> outputBuckets = builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT);
        for (int index = 0; index < fluidResults.size(); index++) {
            FluidStack fluid = fluidResults.get(index);
            SlotPosition position = FluidMixingCategory.fluidOutputPosition(fluidResults.size(), index, splitOutputColumns);
            builder.addSlot(RecipeIngredientRole.OUTPUT, position.x() + 1, position.y() + 1)
                .setFluidRenderer(fluid.getAmount(), true, 16, 16)
                .add(fluid.getFluid(), fluid.getAmount(), fluid.getComponentsPatch());
            Item bucket = fluid.getFluid().getBucket();
            if (bucket != Items.AIR) outputBuckets.add(new ItemStack(bucket));
        }
    }

    private static void setComplexRecipe(IRecipeLayoutBuilder builder, ComplexFluidJeiRecipe recipe) {
        List<List<FluidStack>> fluidInputs = recipe.getDisplayFluidInputs();
        List<ItemIngredientPredicate> itemInputs = recipe.getInputItems();
        int inputCount = fluidInputs.size() + itemInputs.size();
        int inputGridSize = recipe.isHeaterRequired() ? FluidMixingCategory.HEATER_INPUT_GRID_SIZE : inputCount;
        List<IRecipeSlotBuilder> fluidInputSlots = new ArrayList<>(fluidInputs.size());
        for (int index = 0; index < fluidInputs.size(); index++) {
            SlotPosition position = FluidMixingCategory.inputPosition(inputGridSize, index);
            fluidInputSlots.add(FluidMixingCategory.addFluidSlot(
                builder,
                RecipeIngredientRole.INPUT,
                position,
                fluidInputs.get(index),
                false
            ));
        }
        for (int index = 0; index < itemInputs.size(); index++) {
            SlotPosition position = FluidMixingCategory.inputPosition(inputGridSize, fluidInputs.size() + index);
            JeiItemUtil.addSlotWithCount(
                builder,
                position.x() + 1,
                position.y() + 1,
                itemInputs.get(index)
            );
        }
        if (recipe.isHeaterRequired()) {
            builder.addSlot(
                    RecipeIngredientRole.RENDER_ONLY,
                    FluidMixingCategory.HEATER_POSITION.x() + 1,
                    FluidMixingCategory.HEATER_POSITION.y() + 1
            ).addItemStacks(List.of(ModBlocks.HEATER.asStack(), ModBlocks.BURNING_HEATER.asStack()))
                .addRichTooltipCallback((slotView, tooltip) -> tooltip.add(FluidMixingCategory.HEATER_ACTIVE));
        }

        List<ChanceItemStack> itemResults = recipe.getDisplayItemResults();
        List<List<FluidStack>> fluidResults = recipe.getDisplayFluidResults();
        boolean splitOutputColumns = !itemResults.isEmpty() && !fluidResults.isEmpty();
        for (int index = 0; index < itemResults.size(); index++) {
            SlotPosition position = FluidMixingCategory.itemOutputPosition(itemResults.size(), index, splitOutputColumns);
            JeiItemUtil.addOutputSlot(
                builder,
                position.x() + 1,
                position.y() + 1,
                itemResults.get(index)
            );
        }
        List<IRecipeSlotBuilder> fluidOutputSlots = new ArrayList<>(fluidResults.size());
        for (int index = 0; index < fluidResults.size(); index++) {
            SlotPosition position = FluidMixingCategory.fluidOutputPosition(fluidResults.size(), index, splitOutputColumns);
            fluidOutputSlots.add(FluidMixingCategory.addFluidSlot(
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
        IRecipeSlotBuilder slot = builder.addSlot(role, position.x() + 1, position.y() + 1)
            .setFluidRenderer(amount, showCapacity, 16, 16);
        for (FluidStack fluid : fluids) {
            slot.add(fluid.getFluid(), fluid.getAmount(), fluid.getComponentsPatch());
        }
        List<ItemStack> buckets = fluids.stream()
            .map(FluidStack::getFluid)
            .map(Fluid::getBucket)
            .filter(bucket -> bucket != Items.AIR)
            .distinct()
            .map(ItemStack::new)
            .toList();
        if (!buckets.isEmpty()) builder.addInvisibleIngredients(role).addItemStacks(buckets);
        return slot;
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
        int itemOutputCount;
        int fluidOutputCount;
        if (recipe instanceof ComplexFluidJeiRecipe complexRecipe) {
            int inputCount = complexRecipe.getDisplayFluidInputCount() + complexRecipe.getInputItems().size();
            int inputGridSize = complexRecipe.isHeaterRequired() ? FluidMixingCategory.HEATER_INPUT_GRID_SIZE : inputCount;
            for (int index = 0; index < inputCount; index++) {
                SlotPosition position = FluidMixingCategory.inputPosition(inputGridSize, index);
                this.slot.draw(guiGraphics, position.x(), position.y());
            }
            if (complexRecipe.isHeaterRequired()) {
                this.slot.draw(guiGraphics, FluidMixingCategory.HEATER_POSITION.x(), FluidMixingCategory.HEATER_POSITION.y());
            }
            itemOutputCount = complexRecipe.getDisplayItemResults().size();
            fluidOutputCount = complexRecipe.getDisplayFluidResultCount();
        } else {
            int inputCount = recipe.getFluidIngredients().size();
            for (int index = 0; index < inputCount; index++) {
                SlotPosition position = FluidMixingCategory.inputPosition(inputCount, index);
                this.slot.draw(guiGraphics, position.x(), position.y());
            }
            itemOutputCount = recipe.getItemResults().size();
            fluidOutputCount = recipe.getFluidResults().size();
        }

        boolean splitOutputColumns = itemOutputCount > 0 && fluidOutputCount > 0;
        for (int index = 0; index < itemOutputCount; index++) {
            SlotPosition position = FluidMixingCategory.itemOutputPosition(itemOutputCount, index, splitOutputColumns);
            this.slot.draw(guiGraphics, position.x(), position.y());
        }
        for (int index = 0; index < fluidOutputCount; index++) {
            SlotPosition position = FluidMixingCategory.fluidOutputPosition(fluidOutputCount, index, splitOutputColumns);
            this.slot.draw(guiGraphics, position.x(), position.y());
        }

        this.arrowIn.draw(guiGraphics, 47, 30);
        this.arrowOut.draw(guiGraphics, 99, 29);

        float anvilYOffset = JeiRenderHelper.getAnvilAnimationOffset(this.timer) / 3.0F;
        RenderSupport.render3x3Block(guiGraphics, this.largeCauldron, 58, 22, FluidMixingCategory.MODEL_SCALE * 6);
        RenderSupport.render3x3Block(guiGraphics, this.giantAnvil, 58, anvilYOffset, FluidMixingCategory.MODEL_SCALE * 6);
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
        return new SlotPosition(splitColumns ? 119 : 129, FluidMixingCategory.outputRow(count, index));
    }

    private static SlotPosition fluidOutputPosition(int count, int index, boolean splitColumns) {
        return new SlotPosition(splitColumns ? 138 : 129, FluidMixingCategory.outputRow(count, index));
    }

    private static int outputRow(int count, int index) {
        if (count == 1) return 24;
        if (count == 2) return 14 + index * 19;
        return 5 + index * 19;
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        List<RecipeHolder<FluidMixingRecipe>> recipes = new ArrayList<>(
            JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.FLUID_MIXING.get())
        );
        for (RecipeHolder<SolidLiquidRecipe> holder
            : JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.SOLID_LIQUID.get())) {
            if (!ComplexFluidJeiRecipe.isComplex(holder.value())) continue;
            recipes.add(new RecipeHolder<>(
                holder.id(),
                ComplexFluidJeiRecipe.fromSolidLiquid(holder.value())
            ));
        }

        var enchantments = LiquidEnchantmentJeiRecipeUtil.getEnchantments(false);
        if (!enchantments.isEmpty()) {
            recipes.add(new RecipeHolder<>(
                ResourceKey.create(Registries.RECIPE, AnvilCraft.of("jei/fluid_mixing/liquid_enchantment_assimilation")),
                ComplexFluidJeiRecipe.assimilation(enchantments)
            ));
            recipes.add(new RecipeHolder<>(
                ResourceKey.create(Registries.RECIPE, AnvilCraft.of("jei/fluid_mixing/liquid_enchantment_cleanse")),
                ComplexFluidJeiRecipe.cleanse(enchantments)
            ));
        }
        var curses = LiquidEnchantmentJeiRecipeUtil.getEnchantments(true);
        if (!curses.isEmpty()) {
            recipes.add(new RecipeHolder<>(
                ResourceKey.create(Registries.RECIPE, AnvilCraft.of("jei/fluid_mixing/cursed_gold_ingot")),
                ComplexFluidJeiRecipe.curseGoldIngot(curses)
            ));
            recipes.add(new RecipeHolder<>(
                ResourceKey.create(Registries.RECIPE, AnvilCraft.of("jei/fluid_mixing/cursed_gold_block")),
                ComplexFluidJeiRecipe.curseGoldBlock(curses)
            ));
        }
        registration.addRecipes(AnvilCraftJeiPlugin.FLUID_MIXING, recipes);
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
