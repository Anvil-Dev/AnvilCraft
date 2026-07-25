package dev.dubhe.anvilcraft.integration.jei.category.anvil;

import dev.anvilcraft.lib.v2.util.MathUtil;
import dev.anvilcraft.lib.v2.util.TooltipUtil;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.drawable.DrawableBlockStateIcon;
import dev.dubhe.anvilcraft.integration.jei.util.JeiFluidIngredientUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.integration.jei.util.JeiSlotUtil;
import dev.dubhe.anvilcraft.recipe.anvil.predicate.block.HasCauldron;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.SolidLiquidRecipe;
import dev.dubhe.anvilcraft.recipe.component.HasCauldronSimple;
import dev.dubhe.anvilcraft.util.CauldronUtil;
import mezz.jei.api.gui.ITickTimer;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.types.IRecipeHolderType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SolidLiquidCategory extends AbstractProgressCategory<SolidLiquidRecipe> {
    private static final String INPUT_FLUID = "input_fluid";
    private static final String OUTPUT_FLUID = "output_fluid";

    /// 流体标签配方要轮播标签内的各个流体，用比主动画更慢的计时器切换
    private final ITickTimer fluidTagTimer;

    public SolidLiquidCategory(IGuiHelper helper) {
        super(
            helper,
            new DrawableBlockStateIcon(
                Blocks.ANVIL.defaultBlockState(),
                CauldronUtil.fullState(Blocks.WATER_CAULDRON)
            ),
            Component.translatable("gui.anvilcraft.category.solid_liquid")
        );
        this.fluidTagTimer = helper.createTickTimer(40, Integer.MAX_VALUE, true);
    }

    @Override
    public IRecipeHolderType<SolidLiquidRecipe> getRecipeType() {
        return AnvilCraftJeiPlugin.SOLID_LIQUID;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<SolidLiquidRecipe> recipeHolder, IFocusGroup focuses) {
        SolidLiquidRecipe recipe = recipeHolder.value();
        JeiSlotUtil.addInputSlots(builder, recipe.getInputItems());
        if (!recipe.getResultItems().isEmpty()) JeiSlotUtil.addOutputSlots(builder, recipe.getResultItems());
        JeiFluidIngredientUtil.addInputSlot(
            builder,
            INPUT_FLUID,
            72,
            34,
            18,
            19,
            recipe.getHasCauldron()
        );
        if (recipe.getResultItems().isEmpty()) {
            JeiFluidIngredientUtil.addOutputSlot(
                builder,
                OUTPUT_FLUID,
                124,
                24,
                18,
                19,
                recipe.getHasCauldron()
            );
        } else {
            JeiFluidIngredientUtil.addOutputIngredients(builder, recipe.getHasCauldron());
        }
    }

    @Override
    public void createRecipeExtras(
        IRecipeExtrasBuilder builder,
        RecipeHolder<SolidLiquidRecipe> recipeHolder,
        IFocusGroup focuses
    ) {
        JeiFluidIngredientUtil.suppressHoverOverlays(builder);
    }

    @Override
    public void draw(
        RecipeHolder<SolidLiquidRecipe> recipeHolder,
        IRecipeSlotsView view,
        GuiGraphicsExtractor graphics,
        double mouseX,
        double mouseY
    ) {
        SolidLiquidRecipe recipe = recipeHolder.value();

        this.arrowIn.draw(graphics, 54, 30);
        this.arrowOut.draw(graphics, 92, 29);

        BlockState state;
        if (recipe.getHasCauldron().fluidTag() != null) {
            state = CauldronUtil.fullState(this.getDisplayedInputCauldron(recipe, view));
        } else if (recipe.isFromWater()) {
            state = CauldronUtil.fullState(Blocks.WATER_CAULDRON);
        } else if (recipe.isProduceFluid()) {
            state = Blocks.CAULDRON.defaultBlockState();
        } else {
            state = recipe.getHasCauldron().getTransformCauldron().defaultBlockState();
        }
        RenderSupport.renderBlock(graphics, state, 71, 35, 20);
        int anvilYOffset = JeiRenderHelper.getAnvilAnimationOffset(this.timer);
        RenderSupport.renderBlock(graphics, Blocks.ANVIL.defaultBlockState(), 71, 17 + anvilYOffset, 20);

        JeiSlotUtil.drawInputSlots(graphics, this.slotDefault, recipe.getInputItems().size());
        if (!recipe.getResultItems().isEmpty()) {
            if (JeiRecipeUtil.isChance(recipe.getResultItems())) {
                JeiSlotUtil.drawOutputSlots(graphics, this.slotProbability, recipe.getResultItems().size());
            } else {
                JeiSlotUtil.drawOutputSlots(graphics, this.slotDefault, recipe.getResultItems().size());
            }
            HasCauldronSimple hasCauldron = recipe.getHasCauldron();
            if (recipe.isConsumeFluid()) {
                Matrix3x2fStack pose = graphics.pose();
                pose.pushMatrix();
                pose.scale(0.8f, 0.8f);
                graphics.text(
                    Minecraft.getInstance().font,
                    Component.translatable(
                        "gui.anvilcraft.category.solid_liquid.consume_fluid",
                        hasCauldron.consume(),
                        hasCauldron.getFluidCauldron().getName()
                    ),
                    0,
                    70,
                    0xFF000000,
                    false
                );
                pose.popMatrix();
            } else if (recipe.isProduceFluid()) {
                Matrix3x2fStack pose = graphics.pose();
                pose.pushMatrix();
                pose.scale(0.8f, 0.8f);
                graphics.text(
                    Minecraft.getInstance().font,
                    Component.translatable(
                        "gui.anvilcraft.category.solid_liquid.produce_fluid",
                        hasCauldron.produce(),
                        hasCauldron.getTransformCauldron().getName()
                    ),
                    0,
                    70,
                    0xFF000000,
                    false
                );
                pose.popMatrix();
            }
        } else {
            Block result = recipe.getHasCauldron().getTransformCauldron();
            if (recipe.isConsumeFluid() && recipe.isProduceFluid()) {
                state = CauldronUtil.fullState(result);
            } else if (recipe.isConsumeFluid()) {
                state = CauldronUtil.getStateFromContentAndLevel(result, CauldronUtil.maxLevel(result) - 1);
            } else if (recipe.isProduceFluid()) {
                state = CauldronUtil.getStateFromContentAndLevel(result, 1);
            } else {
                state = CauldronUtil.fullState(result);
            }
            RenderSupport.renderBlock(graphics, state, 123, 25, 20);
        }
    }

    @Override
    public void getTooltip(
        ITooltipBuilder tooltip,
        RecipeHolder<SolidLiquidRecipe> recipeHolder,
        IRecipeSlotsView view,
        double mouseX,
        double mouseY
    ) {
        SolidLiquidRecipe recipe = recipeHolder.value();
        if (MathUtil.isInRange(mouseX, mouseY, 72, 34, 90, 53)) {
            Block material = this.getDisplayedInputCauldron(recipe, view);
            if (recipe.getHasCauldron().fluidTag() == null) {
                if (recipe.isFromWater()) {
                    material = Blocks.WATER_CAULDRON;
                } else if (recipe.isProduceFluid()) {
                    material = Blocks.CAULDRON;
                }
            }
            tooltip.addAll(TooltipUtil.tooltip(material));
        }
        if (MathUtil.isInRange(mouseX, mouseY, 124, 24, 140, 42)) {
            if (!recipe.getResultItems().isEmpty()) return;
            Block result = recipe.getHasCauldron().getTransformCauldron();
            if (recipe.isConsumeFluid() && recipe.isProduceFluid()) {
                tooltip.addAll(TooltipUtil.tooltip(result));
                return;
            }
            if (recipe.isConsumeFluid() && CauldronUtil.maxLevel(result) <= 1) {
                result = Blocks.CAULDRON;
            }
            tooltip.addAll(TooltipUtil.tooltip(result));
        }
    }

    /// 取 JEI 当前展示的输入流体对应的炼药锅，取不到时回退到配方声明的流体
    private Block getDisplayedInputCauldron(SolidLiquidRecipe recipe, IRecipeSlotsView slotsView) {
        return JeiFluidIngredientUtil.getDisplayedFluid(slotsView, INPUT_FLUID)
            .or(() -> this.getDisplayedTaggedFluid(recipe))
            .map(BuiltInRegistries.FLUID::getKey)
            .map(HasCauldron::getDefaultCauldron)
            .orElseGet(() -> recipe.getHasCauldron().getFluidCauldron());
    }

    private Optional<Fluid> getDisplayedTaggedFluid(SolidLiquidRecipe recipe) {
        Identifier fluidTag = recipe.getHasCauldron().fluidTag();
        if (fluidTag == null) return Optional.empty();
        List<Fluid> values = new ArrayList<>();
        for (Holder<Fluid> holder : BuiltInRegistries.FLUID.getTagOrEmpty(TagKey.create(Registries.FLUID, fluidTag))) {
            if (holder.value().defaultFluidState().isSource()) values.add(holder.value());
        }
        if (values.isEmpty()) return Optional.empty();
        return Optional.of(values.get(this.fluidTagTimer.getValue() % values.size()));
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        List<RecipeHolder<SolidLiquidRecipe>> holders = JeiRecipeUtil.getRecipeHoldersFromType(
            ModRecipeTypes.SOLID_LIQUID.get()
        );
        registration.addRecipes(AnvilCraftJeiPlugin.SOLID_LIQUID, holders);
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        AnvilCraftJeiPlugin.addAnvilProcessingCatalysts(registration, AnvilCraftJeiPlugin.SOLID_LIQUID);
        AnvilCraftJeiPlugin.addCauldronCatalysts(registration, AnvilCraftJeiPlugin.SOLID_LIQUID);
    }
}
