package dev.dubhe.anvilcraft.integration.jei.category;

import dev.anvilcraft.lib.v2.util.predicate.ChanceItemStack;
import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.drawable.DrawableBlockStateIcon;
import dev.dubhe.anvilcraft.integration.jei.recipe.ComplexFluidJeiRecipe;
import dev.dubhe.anvilcraft.integration.jei.recipe.LiquidEnchantmentJeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiCauldronUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiFluidUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiItemUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.integration.jei.util.JeiSlotUtil;
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
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

public class SolidLiquidCategory extends AbstractLiquidReactionCategory {
    private final ITickTimer containerTimer;

    public SolidLiquidCategory(IGuiHelper helper) {
        super(
            helper,
            DrawableBlockStateIcon.alternatingDown(
                Blocks.ANVIL.defaultBlockState(),
                Blocks.WATER_CAULDRON.defaultBlockState(),
                ModBlocks.FISH_TANK.getDefaultState()
            )
        );
        // 炼药锅与鱼缸都能承接这些流体，每秒轮换一次提示两种容器都可用
        this.containerTimer = helper.createTickTimer(40, 1, false);
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
    public void setRecipe(
        IRecipeLayoutBuilder builder,
        RecipeHolder<FluidMixingRecipe> recipeHolder,
        IFocusGroup focuses
    ) {
        FluidMixingRecipe recipe = recipeHolder.value();
        if (recipe instanceof ComplexFluidJeiRecipe complexRecipe) {
            if (useSmallCauldron(complexRecipe)) {
                setSmallCauldronRecipe(builder, complexRecipe);
            } else {
                setComplexRecipe(builder, complexRecipe);
            }
        } else {
            setFluidMixingRecipe(builder, recipe);
        }
    }

    @Override
    public void draw(
        RecipeHolder<FluidMixingRecipe> recipeHolder,
        IRecipeSlotsView recipeSlotsView,
        GuiGraphics guiGraphics,
        double mouseX,
        double mouseY
    ) {
        if (recipeHolder.value() instanceof ComplexFluidJeiRecipe complexRecipe
            && useSmallCauldron(complexRecipe)) {
            this.drawSmallCauldron(complexRecipe, guiGraphics);
        } else {
            drawBigCauldron(recipeHolder, recipeSlotsView, guiGraphics, mouseX, mouseY);
        }
    }

    /**
     * 小锅布局：输入只有一组流体、输出不超过一组，且所有流体都不超过 1 桶。
     * 液态魔咒只能在大型炼药锅里处理，1 桶也一样，固定走大锅布局。
     */
    private static boolean useSmallCauldron(ComplexFluidJeiRecipe recipe) {
        List<FluidStack> fluids = displayFluids(recipe);
        if (JeiCauldronUtil.needsLargeCauldron(fluids) || JeiCauldronUtil.exceedsBucket(fluids)) return false;
        List<List<FluidStack>> inputs = recipe.getDisplayFluidInputs();
        return inputs.size() == 1 && recipe.getDisplayFluidResults().size() <= 1;
    }

    private static List<FluidStack> displayFluids(ComplexFluidJeiRecipe recipe) {
        List<FluidStack> fluids = new ArrayList<>();
        recipe.getDisplayFluidInputs().forEach(fluids::addAll);
        recipe.getDisplayFluidResults().forEach(fluids::addAll);
        return fluids;
    }

    /** 小锅布局的容器方块：可装炼药锅的流体在炼药锅与鱼缸之间轮换，其余只能用鱼缸。 */
    private BlockState smallContainerState(ComplexFluidJeiRecipe recipe) {
        return JeiCauldronUtil.smallContainer(SolidLiquidCategory.displayFluids(recipe), this.containerTimer.getValue() != 0);
    }

    private static void setSmallCauldronRecipe(IRecipeLayoutBuilder builder, ComplexFluidJeiRecipe recipe) {
        final List<ItemIngredientPredicate> itemInputs = recipe.getInputItems();
        final List<FluidStack> fluidInputs = recipe.getDisplayFluidInputs().getFirst();
        final List<ChanceItemStack> itemResults = recipe.getDisplayItemResults();
        final List<FluidStack> fluidResults = recipe.getDisplayFluidResults().isEmpty()
                                              ? List.of()
                                              : recipe.getDisplayFluidResults().getFirst();
        final boolean inputMixed = !itemInputs.isEmpty();
        if (inputMixed) {
            JeiItemUtil.addItemInputSlots(builder, itemInputs);
        }
        // 不消耗的输入流体按催化剂展示，避免被当成"要倒掉一整桶"
        final boolean notConsumed = recipe.isFluidInputNotConsumed();
        IRecipeSlotBuilder fluidInputSlot = addSmallFluidSlot(
            builder,
            notConsumed ? RecipeIngredientRole.CATALYST : RecipeIngredientRole.INPUT,
            JeiSlotUtil.INPUT_X,
            inputMixed ? JeiSlotUtil.FLUID_Y : JeiSlotUtil.DEFAULT_Y,
            fluidInputs,
            false
        );
        if (notConsumed && fluidInputSlot != null) {
            fluidInputSlot.addRichTooltipCallback(
                (slotView, tooltip) -> tooltip.add(JeiFluidUtil.TOOLTIP_NOT_CONSUMED)
            );
        }
        final boolean outputMixed = !itemResults.isEmpty() && !fluidResults.isEmpty();
        if (outputMixed) {
            JeiItemUtil.addItemOutputSlots(builder, itemResults);
        } else {
            JeiItemUtil.addDefaultOutputSlots(builder, itemResults);
        }
        addSmallFluidSlot(
            builder,
            RecipeIngredientRole.OUTPUT,
            JeiSlotUtil.OUTPUT_X,
            outputMixed ? JeiSlotUtil.FLUID_Y : JeiSlotUtil.DEFAULT_Y,
            fluidResults,
            true
        );
    }

    private static @Nullable IRecipeSlotBuilder addSmallFluidSlot(
        IRecipeLayoutBuilder builder,
        RecipeIngredientRole role,
        int x,
        int y,
        List<FluidStack> fluids,
        boolean showCapacity
    ) {
        if (fluids.isEmpty()) return null;
        return JeiFluidUtil.addFluidSlot(
            builder,
            role,
            x,
            y,
            16,
            16,
            fluids.getFirst().getAmount(),
            showCapacity,
            fluids
        );
    }

    private void drawSmallCauldron(ComplexFluidJeiRecipe recipe, GuiGraphics guiGraphics) {
        final List<ItemIngredientPredicate> itemInputs = recipe.getInputItems();
        final List<ChanceItemStack> itemResults = recipe.getDisplayItemResults();
        final boolean hasInputFluid = !recipe.getDisplayFluidInputs().isEmpty();
        final boolean hasOutputFluid = !recipe.getDisplayFluidResults().isEmpty();

        final float anvilYOffset = JeiRenderHelper.getAnvilAnimationOffset(this.timer);
        RenderSupport.renderBlock(
            guiGraphics,
            Blocks.ANVIL.defaultBlockState(),
            81,
            12 + anvilYOffset,
            20,
            12,
            RenderSupport.SINGLE_BLOCK
        );
        RenderSupport.renderBlock(
            guiGraphics,
            this.smallContainerState(recipe),
            81,
            30,
            10,
            12,
            RenderSupport.SINGLE_BLOCK
        );
        this.arrowIn.draw(guiGraphics, 54, 22);
        this.arrowOut.draw(guiGraphics, 92, 22);

        final IntFunction<IDrawable> outputSlot = JeiRecipeUtil.outputSlotFor(
            itemResults,
            this.slot,
            this.slotProbability
        );
        final boolean inputMixed = !itemInputs.isEmpty() && hasInputFluid;
        if (!itemInputs.isEmpty()) {
            if (inputMixed) {
                JeiSlotUtil.drawItemInputSlots(guiGraphics, this.slot, itemInputs.size());
            } else {
                JeiSlotUtil.drawDefaultInputSlots(guiGraphics, this.slot, itemInputs.size());
            }
        }
        if (hasInputFluid) {
            if (inputMixed) {
                JeiSlotUtil.drawFluidInputSlots(guiGraphics, this.slot, 1);
            } else {
                JeiSlotUtil.drawDefaultInputSlots(guiGraphics, this.slot, 1);
            }
        }
        final boolean outputMixed = !itemResults.isEmpty() && hasOutputFluid;
        if (!itemResults.isEmpty()) {
            if (outputMixed) {
                JeiSlotUtil.drawItemOutputSlots(guiGraphics, outputSlot, itemResults.size());
            } else {
                JeiSlotUtil.drawDefaultOutputSlots(guiGraphics, outputSlot, itemResults.size());
            }
        }
        if (hasOutputFluid) {
            if (outputMixed) {
                JeiSlotUtil.drawFluidOutputSlots(guiGraphics, this.slot, 1);
            } else {
                JeiSlotUtil.drawDefaultOutputSlots(guiGraphics, this.slot, 1);
            }
        }
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        List<RecipeHolder<FluidMixingRecipe>> recipes = new ArrayList<>();
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
        recipes.add(new RecipeHolder<>(
            AnvilCraft.of("jei/solid_liquid/enchanted_gold_block"),
            ComplexFluidJeiRecipe.enchantGoldBlock()
        ));
        registration.addRecipes(AnvilCraftJeiPlugin.SOLID_LIQUID, recipes);
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        AnvilCraftJeiPlugin.ANVIL_PROCESSING_CATALYSTS.forEach(item ->
        registration.addRecipeCatalyst(new ItemStack(item), AnvilCraftJeiPlugin.SOLID_LIQUID));
        registration.addRecipeCatalyst(new ItemStack(Items.CAULDRON), AnvilCraftJeiPlugin.SOLID_LIQUID);
        registration.addRecipeCatalyst(ModBlocks.FISH_TANK.asStack(), AnvilCraftJeiPlugin.SOLID_LIQUID);
        registration.addRecipeCatalyst(ModBlocks.LARGE_CAULDRON.asStack(), AnvilCraftJeiPlugin.SOLID_LIQUID);
        registration.addRecipeCatalyst(ModBlocks.GIANT_ANVIL.asStack(), AnvilCraftJeiPlugin.SOLID_LIQUID);
    }
}
