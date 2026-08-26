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
import dev.dubhe.anvilcraft.integration.jei.util.JeiFluidUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiItemUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.integration.jei.util.JeiSlotUtil;
import dev.dubhe.anvilcraft.recipe.FluidMixingRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.SolidLiquidRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
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
import net.neoforged.neoforge.fluids.CauldronFluidContent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;

import java.util.ArrayList;
import java.util.List;

public class SolidLiquidCategory extends AbstractLiquidReactionCategory {
    public SolidLiquidCategory(IGuiHelper helper) {
        super(
            helper,
            new DrawableBlockStateIcon(
                Blocks.ANVIL.defaultBlockState(),
                Blocks.WATER_CAULDRON.defaultBlockState()
            )
        );
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
            drawSmallCauldron(complexRecipe, guiGraphics);
        } else {
            drawBigCauldron(recipeHolder, recipeSlotsView, guiGraphics, mouseX, mouseY);
        }
    }

    /**
     * 只需要一种流体且输出的流体不超过一种（各不超 1 桶）时使用小炼药锅展示。
     */
    private static boolean useSmallCauldron(ComplexFluidJeiRecipe recipe) {
        List<List<FluidStack>> inputs = recipe.getDisplayFluidInputs();
        List<List<FluidStack>> results = recipe.getDisplayFluidResults();
        if (inputs.size() != 1 || results.size() > 1) return false;
        List<FluidStack> inputFluids = inputs.getFirst();
        if (inputFluids.isEmpty()
            || inputFluids.getFirst().getAmount() > FluidType.BUCKET_VOLUME) {
            return false;
        }
        if (inputFluids.stream()
            .anyMatch(fluid -> CauldronFluidContent.getForFluid(fluid.getFluid()) == null)) {
            return false;
        }
        if (results.isEmpty()) return true;
        List<FluidStack> resultFluids = results.getFirst();
        return resultFluids.size() == 1
               && resultFluids.getFirst().getAmount() <= FluidType.BUCKET_VOLUME;
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
        addSmallFluidSlot(
            builder,
            RecipeIngredientRole.INPUT,
            JeiSlotUtil.INPUT_X,
            inputMixed ? JeiSlotUtil.FLUID_Y : JeiSlotUtil.DEFAULT_Y,
            fluidInputs,
            false
        );
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

    private static void addSmallFluidSlot(
        IRecipeLayoutBuilder builder,
        RecipeIngredientRole role,
        int x,
        int y,
        List<FluidStack> fluids,
        boolean showCapacity
    ) {
        if (fluids.isEmpty()) return;
        JeiFluidUtil.addFluidSlot(
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
            Blocks.CAULDRON.defaultBlockState(),
            81,
            30,
            10,
            12,
            RenderSupport.SINGLE_BLOCK
        );
        this.arrowIn.draw(guiGraphics, 54, 22);
        this.arrowOut.draw(guiGraphics, 92, 22);

        final IDrawable outputSlot = JeiRecipeUtil.isChance(itemResults) ? this.slotProbability : this.slot;
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
