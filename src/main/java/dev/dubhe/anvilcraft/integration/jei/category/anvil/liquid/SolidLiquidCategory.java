package dev.dubhe.anvilcraft.integration.jei.category.anvil.liquid;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.GiantAnvilBlock;
import dev.dubhe.anvilcraft.block.LargeCauldronBlock;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import dev.dubhe.anvilcraft.block.state.GiantAnvilCube;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.drawable.DrawableBlockStateIcon;
import dev.dubhe.anvilcraft.integration.jei.recipe.LiquidEnchantmentJeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.recipe.LiquidEnchantmentSolidLiquidRecipe;
import dev.dubhe.anvilcraft.integration.jei.util.JeiItemUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.integration.jei.util.JeiSlotUtil;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.SolidLiquidRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;

public class SolidLiquidCategory extends AbstractLiquidCategory<SolidLiquidRecipe> {
    private static final float MODEL_SCALE = 7.5F;

    private final BlockState largeCauldron;
    private final BlockState giantAnvil;

    public SolidLiquidCategory(IGuiHelper helper) {
        super(
            helper,
            new DrawableBlockStateIcon(
                Blocks.ANVIL.defaultBlockState(),
                Blocks.WATER_CAULDRON.defaultBlockState()
            ),
            Component.translatable("gui.anvilcraft.category.solid_liquid")
        );
        this.largeCauldron = ModBlocks.LARGE_CAULDRON.getDefaultState()
            .setValue(LargeCauldronBlock.HALF, Cube3x3PartHalf.MID_CENTER);
        this.giantAnvil = ModBlocks.GIANT_ANVIL.getDefaultState()
            .setValue(GiantAnvilBlock.HALF, Cube3x3PartHalf.MID_CENTER)
            .setValue(GiantAnvilBlock.CUBE, GiantAnvilCube.CENTER);
    }

    @Override
    public RecipeType<RecipeHolder<SolidLiquidRecipe>> getRecipeType() {
        return AnvilCraftJeiPlugin.SOLID_LIQUID;
    }

    @Override
    public void setRecipe(
        IRecipeLayoutBuilder builder,
        RecipeHolder<SolidLiquidRecipe> recipeHolder,
        IFocusGroup focuses
    ) {
        SolidLiquidRecipe recipe = recipeHolder.value();
        if (!(recipe instanceof LiquidEnchantmentSolidLiquidRecipe liquidEnchantmentRecipe)) {
            super.setRecipe(builder, recipeHolder, focuses);
            return;
        }

        JeiItemUtil.addItemInputSlots(builder, recipe.getInputItems());
        addFluidSlot(
            builder,
            RecipeIngredientRole.INPUT,
            JeiSlotUtil.INPUT_X,
            JeiSlotUtil.FLUID_Y,
            liquidEnchantmentRecipe.getInputFluids()
        );
        if (!recipe.getResultItems().isEmpty()) {
            if (liquidEnchantmentRecipe.getOutputFluids().isEmpty()) {
                JeiItemUtil.addDefaultOutputSlots(builder, recipe.getResultItems());
            } else {
                JeiItemUtil.addItemOutputSlots(builder, recipe.getResultItems());
            }
        }
        if (!liquidEnchantmentRecipe.getOutputFluids().isEmpty()) {
            int outputY = recipe.getResultItems().isEmpty() ? JeiSlotUtil.DEFAULT_Y : JeiSlotUtil.FLUID_Y;
            addFluidSlot(
                builder,
                RecipeIngredientRole.OUTPUT,
                JeiSlotUtil.OUTPUT_X,
                outputY,
                liquidEnchantmentRecipe.getOutputFluids()
            );
        }
    }

    private static void addFluidSlot(
        IRecipeLayoutBuilder builder,
        RecipeIngredientRole role,
        int x,
        int y,
        List<FluidStack> fluids
    ) {
        int amount = fluids.getFirst().getAmount();
        IRecipeSlotBuilder slot = builder.addSlot(role, x, y)
            .setFluidRenderer(amount, false, 16, 16);
        for (FluidStack fluid : fluids) {
            slot.addFluidStack(fluid.getFluid(), fluid.getAmount(), fluid.getComponentsPatch());
        }
    }

    @Override
    public void draw(
        RecipeHolder<SolidLiquidRecipe> recipeHolder,
        IRecipeSlotsView recipeSlotsView,
        GuiGraphics guiGraphics,
        double mouseX,
        double mouseY
    ) {
        SolidLiquidRecipe recipe = recipeHolder.value();
        if (!(recipe instanceof LiquidEnchantmentSolidLiquidRecipe liquidEnchantmentRecipe)) {
            super.draw(recipeHolder, recipeSlotsView, guiGraphics, mouseX, mouseY);
            return;
        }

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
        this.arrowIn.draw(guiGraphics, 54, 22);
        this.arrowOut.draw(guiGraphics, 92, 22);

        JeiSlotUtil.drawItemInputSlots(guiGraphics, this.slotDefault, recipe.getInputItems().size());
        JeiSlotUtil.drawFluidInputSlots(guiGraphics, this.slotDefault, 1);
        if (!recipe.getResultItems().isEmpty()) {
            if (liquidEnchantmentRecipe.getOutputFluids().isEmpty()) {
                JeiSlotUtil.drawDefaultOutputSlots(guiGraphics, this.slotDefault, recipe.getResultItems().size());
            } else {
                JeiSlotUtil.drawItemOutputSlots(guiGraphics, this.slotDefault, recipe.getResultItems().size());
            }
        }
        if (!liquidEnchantmentRecipe.getOutputFluids().isEmpty()) {
            if (recipe.getResultItems().isEmpty()) {
                JeiSlotUtil.drawDefaultOutputSlots(guiGraphics, this.slotDefault, 1);
            } else {
                JeiSlotUtil.drawFluidOutputSlots(guiGraphics, this.slotDefault, 1);
            }
        }
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        List<RecipeHolder<SolidLiquidRecipe>> recipes = new ArrayList<>(
            JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.SOLID_LIQUID_TYPE.get())
        );
        var enchantments = LiquidEnchantmentJeiRecipeUtil.getEnchantments(false);
        if (!enchantments.isEmpty()) {
            recipes.add(new RecipeHolder<>(
                AnvilCraft.of("jei/solid_liquid/liquid_enchantment_cleanse"),
                LiquidEnchantmentSolidLiquidRecipe.cleanse(
                    LiquidEnchantmentJeiRecipeUtil.createFluidStacks(enchantments, 8)
                )
            ));
        }
        var curses = LiquidEnchantmentJeiRecipeUtil.getEnchantments(true);
        if (!curses.isEmpty()) {
            recipes.add(new RecipeHolder<>(
                AnvilCraft.of("jei/solid_liquid/cursed_gold_ingot"),
                LiquidEnchantmentSolidLiquidRecipe.curseGoldIngot(
                    LiquidEnchantmentJeiRecipeUtil.createFluidStacks(curses, 1)
                )
            ));
            recipes.add(new RecipeHolder<>(
                AnvilCraft.of("jei/solid_liquid/cursed_gold_block"),
                LiquidEnchantmentSolidLiquidRecipe.curseGoldBlock(
                    LiquidEnchantmentJeiRecipeUtil.createFluidStacks(curses, 9)
                )
            ));
        }
        registration.addRecipes(AnvilCraftJeiPlugin.SOLID_LIQUID, recipes);
    }
}
