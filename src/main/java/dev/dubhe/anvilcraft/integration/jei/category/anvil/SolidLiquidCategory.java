package dev.dubhe.anvilcraft.integration.jei.category.anvil;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.block.state.Color;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.drawable.DrawableBlockStateIcon;
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
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class SolidLiquidCategory implements IRecipeCategory<RecipeHolder<SolidLiquidRecipe>> {
    public static final int WIDTH = 162;
    public static final int HEIGHT = 64;

    private final IDrawable icon;
    private final IDrawable slotDefault;
    private final IDrawable slotProbability;
    private final Component title;
    private final ITickTimer timer;
    private final ITickTimer fluidTagTimer;

    private final IDrawable arrowIn;
    private final IDrawable arrowOut;

    public SolidLiquidCategory(IGuiHelper helper) {
        icon = new DrawableBlockStateIcon(
            Blocks.ANVIL.defaultBlockState(),
            CauldronUtil.fullState(Blocks.WATER_CAULDRON)
        );
        slotDefault = JeiRenderHelper.getSlotDefault(helper);
        slotProbability = JeiRenderHelper.getSlotProbability(helper);
        title = Component.translatable("gui.anvilcraft.category.solid_liquid");
        timer = helper.createTickTimer(30, 60, true);
        fluidTagTimer = helper.createTickTimer(20 * Color.values().length, Color.values().length - 1, false);

        arrowIn = JeiRenderHelper.getArrowInput(helper);
        arrowOut = JeiRenderHelper.getArrowOutput(helper);
    }

    @Override
    public RecipeType<RecipeHolder<SolidLiquidRecipe>> getRecipeType() {
        return AnvilCraftJeiPlugin.SOLID_LIQUID;
    }

    @Override
    public Component getTitle() {
        return title;
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
        return icon;
    }

    @Override
    public void setRecipe(
        IRecipeLayoutBuilder builder,
        RecipeHolder<SolidLiquidRecipe> recipeHolder,
        IFocusGroup focuses
    ) {
        SolidLiquidRecipe recipe = recipeHolder.value();
        JeiSlotUtil.addInputSlots(builder, recipe.getInputItems());
        if (!recipe.getResultItems().isEmpty()) {
            JeiSlotUtil.addOutputSlots(builder, recipe.getResultItems());
        }
        addFluidIngredients(builder, recipe);
    }

    @Override
    public void draw(
        RecipeHolder<SolidLiquidRecipe> recipeHolder,
        IRecipeSlotsView recipeSlotsView,
        GuiGraphics guiGraphics,
        double mouseX,
        double mouseY) {
        SolidLiquidRecipe recipe = recipeHolder.value();
        float anvilYOffset = JeiRenderHelper.getAnvilAnimationOffset(timer);
        RenderSupport.renderBlock(
            guiGraphics,
            Blocks.ANVIL.defaultBlockState(),
            81,
            22 + anvilYOffset,
            20,
            12,
            RenderSupport.SINGLE_BLOCK);
        Block material = getDisplayedInputCauldron(recipe);
        BlockState state = CauldronUtil.fullState(material);
        RenderSupport.renderBlock(guiGraphics, state, 81, 40, 10, 12, RenderSupport.SINGLE_BLOCK);

        arrowIn.draw(guiGraphics, 54, 30);
        arrowOut.draw(guiGraphics, 92, 29);

        JeiSlotUtil.drawInputSlots(guiGraphics, slotDefault, recipe.getInputItems().size());
        if (!recipe.getResultItems().isEmpty()) {
            if (JeiRecipeUtil.isChance(recipe.getResultItems())) {
                JeiSlotUtil.drawOutputSlots(guiGraphics, slotProbability, recipe.getResultItems().size());
            } else {
                JeiSlotUtil.drawOutputSlots(guiGraphics, slotDefault, recipe.getResultItems().size());
            }
            HasCauldronSimple hasCauldron = recipe.getHasCauldron();
            if (recipe.isConsumeFluid()) {
                PoseStack pose = guiGraphics.pose();
                pose.pushPose();
                pose.scale(0.8f, 0.8f, 1.0f);
                guiGraphics.drawString(
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
                pose.popPose();
            } else if (recipe.isProduceFluid()) {
                PoseStack pose = guiGraphics.pose();
                pose.pushPose();
                pose.scale(0.8f, 0.8f, 1.0f);
                guiGraphics.drawString(
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
                pose.popPose();
            }
        } else {
            Block result = recipe.getHasCauldron().getTransformCauldron();
            if (recipe.isConsumeFluid() && recipe.isProduceFluid()) {
                // 既消耗又产生 = 流体替换，显示满的输出炼药锅
                state = CauldronUtil.fullState(result);
            } else if (recipe.isConsumeFluid()) {
                state = CauldronUtil.getStateFromContentAndLevel(result, CauldronUtil.maxLevel(result) - 1);
            } else if (recipe.isProduceFluid()) {
                state = CauldronUtil.getStateFromContentAndLevel(result, 1);
            } else {
                state = CauldronUtil.fullState(result);
            }
            RenderSupport.renderBlock(guiGraphics, state, 133, 30, 0, 12, RenderSupport.SINGLE_BLOCK);
        }
    }

    @Override
    public void getTooltip(
        ITooltipBuilder tooltip,
        RecipeHolder<SolidLiquidRecipe> recipeHolder,
        IRecipeSlotsView recipeSlotsView,
        double mouseX,
        double mouseY
    ) {
        SolidLiquidRecipe recipe = recipeHolder.value();
        if (mouseX >= 72 && mouseX <= 90) {
            if (mouseY >= 34 && mouseY <= 53) {
                HasCauldronSimple hasCauldron = recipe.getHasCauldron();
                Block material = getDisplayedInputCauldron(recipe);
                Component text;
                if (hasCauldron.fluidTag() != null) {
                    text = material.getName();
                } else if (recipe.isFromWater()) {
                    text = Blocks.WATER_CAULDRON.getName();
                } else if (recipe.isConsumeFluid()) {
                    text = material.getName();
                } else if (recipe.isProduceFluid()) {
                    text = Blocks.CAULDRON.getName();
                } else {
                    text = material.getName();
                }
                tooltip.add(text);
            }
        }
        if (mouseX >= 124 && mouseX <= 140) {
            if (mouseY >= 24 && mouseY <= 42) {
                Block result = recipe.getHasCauldron().getTransformCauldron();
                Component text;
                if (recipe.getResultItems().isEmpty()) {
                    if (recipe.isConsumeFluid() && recipe.isProduceFluid()) {
                        // 既消耗又产生 = 流体替换，显示输出炼药锅名称
                        text = result.getName();
                    } else if (recipe.isConsumeFluid()) {
                        if (CauldronUtil.maxLevel(result) > 1) {
                            text = result.getName();
                        } else {
                            text = Blocks.CAULDRON.getName();
                        }
                    } else {
                        text = result.getName();
                    }
                    tooltip.add(text);
                }
            }
        }
    }

    private void addFluidIngredients(IRecipeLayoutBuilder builder, SolidLiquidRecipe recipe) {
        HasCauldronSimple hasCauldron = recipe.getHasCauldron();
        if (hasCauldron.fluidTag() != null) {
            BuiltInRegistries.FLUID.getTag(TagKey.create(Registries.FLUID, hasCauldron.fluidTag()))
                .ifPresent(fluids -> {
                    var ingredients = builder.addInvisibleIngredients(RecipeIngredientRole.INPUT);
                    fluids.stream()
                        .filter(holder -> holder.value().defaultFluidState().isSource())
                        .forEach(holder -> ingredients.addFluidStack(holder.value()));
                });
        } else if (HasCauldron.isNotEmpty(hasCauldron.fluid())) {
            builder.addInvisibleIngredients(RecipeIngredientRole.INPUT)
                .addFluidStack(BuiltInRegistries.FLUID.get(hasCauldron.fluid()));
        }
        if (HasCauldron.isNotEmpty(hasCauldron.transform())) {
            builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT)
                .addFluidStack(BuiltInRegistries.FLUID.get(hasCauldron.transform()));
        }
    }

    private Block getDisplayedInputCauldron(SolidLiquidRecipe recipe) {
        return getDisplayedTaggedFluid(recipe)
            .map(BuiltInRegistries.FLUID::getKey)
            .map(HasCauldron::getDefaultCauldron)
            .orElseGet(() -> recipe.getHasCauldron().getFluidCauldron());
    }

    private Optional<Fluid> getDisplayedTaggedFluid(SolidLiquidRecipe recipe) {
        ResourceLocation fluidTag = recipe.getHasCauldron().fluidTag();
        if (fluidTag == null) return Optional.empty();
        return BuiltInRegistries.FLUID.getTag(TagKey.create(Registries.FLUID, fluidTag))
            .flatMap(fluids -> {
                List<Holder<Fluid>> values = fluids.stream()
                    .filter(holder -> holder.value().defaultFluidState().isSource())
                    .toList();
                if (values.isEmpty()) return Optional.empty();
                return Optional.of(values.get(fluidTagTimer.getValue() % values.size()).value());
            });
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        List<RecipeHolder<SolidLiquidRecipe>> holders = JeiRecipeUtil.getRecipeHoldersFromType(
            ModRecipeTypes.SOLID_LIQUID_TYPE.get()
        );
        registration.addRecipes(AnvilCraftJeiPlugin.SOLID_LIQUID, holders);
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        AnvilCraftJeiPlugin.addAnvilProcessingCatalysts(registration, AnvilCraftJeiPlugin.SOLID_LIQUID);
        registration.addRecipeCatalyst(new ItemStack(Items.CAULDRON), AnvilCraftJeiPlugin.SOLID_LIQUID);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.FISH_TANK), AnvilCraftJeiPlugin.SOLID_LIQUID);
    }
}
