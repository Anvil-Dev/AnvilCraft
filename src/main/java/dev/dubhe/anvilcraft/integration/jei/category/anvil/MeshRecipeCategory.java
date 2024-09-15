package dev.dubhe.anvilcraft.integration.jei.category.anvil;

import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.recipe.MeshRecipeGroup;
import dev.dubhe.anvilcraft.util.RecipeUtil;
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
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.providers.number.BinomialDistributionGenerator;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.neoforged.neoforge.common.util.Lazy;

import com.google.common.collect.ImmutableList;
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
import org.jetbrains.annotations.Nullable;

import java.text.DecimalFormat;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class MeshRecipeCategory implements IRecipeCategory<MeshRecipeGroup> {
    private static final DecimalFormat FORMATTER = new DecimalFormat();

    public static final int WIDTH = 162;
    public static final int ROW_START = 28;

    private final Lazy<IDrawable> background;
    private final IDrawable slot;
    private final IDrawable icon;
    private final Component title;

    public MeshRecipeCategory(IGuiHelper helper) {
        this.background = Lazy.of(() -> helper.createBlankDrawable(WIDTH, ROW_START + MeshRecipeGroup.maxRows * 18));
        this.slot = helper.getSlotDrawable();
        this.icon = helper.createDrawableItemStack(new ItemStack(Items.SCAFFOLDING));
        this.title = Component.translatable("gui.anvilcraft.category.mesh");
    }

    @Override
    public RecipeType<MeshRecipeGroup> getRecipeType() {
        return AnvilCraftJeiPlugin.MESH;
    }

    @Override
    public Component getTitle() {
        return title;
    }

    @Override
    public IDrawable getBackground() {
        return background.get();
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, MeshRecipeGroup recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 73, 1).addIngredients(recipe.ingredient());

        for (int i = 0; i < recipe.results().size(); i++) {
            MeshRecipeGroup.Result result = recipe.results().get(i);
            IRecipeSlotBuilder slot = builder.addSlot(
                            RecipeIngredientRole.OUTPUT, 1 + (i % 9) * 18, 1 + ROW_START + 18 * (i / 9))
                    .addItemStack(result.item);
            addTooltips(slot, result.provider);
        }
    }

    @Override
    public void draw(
            MeshRecipeGroup recipe,
            IRecipeSlotsView recipeSlotsView,
            GuiGraphics guiGraphics,
            double mouseX,
            double mouseY) {
        this.slot.draw(guiGraphics, 72, 0);

        for (int row = 0; row < MeshRecipeGroup.maxRows; row++) {
            for (int column = 0; column < 9; column++) {
                this.slot.draw(guiGraphics, column * 18, ROW_START + row * 18);
            }
        }
    }

    public static void addTooltips(IRecipeSlotBuilder slot, NumberProvider provider) {
        ImmutableList.Builder<Component> tooltipLines = new ImmutableList.Builder<>();

        if (provider instanceof BinomialDistributionGenerator binomial) {
            if (binomial.n() instanceof ConstantValue constantValue && constantValue.value() == 1) {
                String chance = FORMATTER.format(RecipeUtil.getExpectedValue(binomial.p()) * 100);
                tooltipLines.add(Component.translatable("gui.anvilcraft.category.mesh.chance", chance)
                        .withStyle(ChatFormatting.GRAY));
            } else {
                addAvgOutput(tooltipLines, RecipeUtil.getExpectedValue(provider));
            }
            addMinMax(tooltipLines, 0, getMax(binomial.n()));
        } else if (provider.getClass() != ConstantValue.class) {
            double val = RecipeUtil.getExpectedValue(provider);
            if (val != -1) {
                addAvgOutput(tooltipLines, val);
                if (provider instanceof UniformGenerator) {
                    addMinMax(tooltipLines, getMin(provider), getMax(provider));
                }
            }
        }

        slot.addRichTooltipCallback((slotView, tooltip) -> tooltip.addAll(tooltipLines.build()));
    }

    private static double getMin(NumberProvider provider) {
        return switch (provider) {
            case ConstantValue value -> value.value();
            case UniformGenerator uniform -> getMin(uniform.min());
            default -> 0;
        };
    }

    private static double getMax(NumberProvider provider) {
        return switch (provider) {
            case ConstantValue value -> value.value();
            case UniformGenerator uniform -> getMax(uniform.max());
            case BinomialDistributionGenerator binomial -> getMax(binomial.n());
            default -> 0;
        };
    }

    private static void addAvgOutput(ImmutableList.Builder<Component> tooltipLines, double avgValue) {
        String avgOutput = FORMATTER.format(avgValue);
        tooltipLines.add(Component.translatable("gui.anvilcraft.category.mesh.average_output", avgOutput)
                .withStyle(ChatFormatting.GRAY));
    }

    private static void addMinMax(ImmutableList.Builder<Component> tooltipLines, double min, double max) {
        String minOutput = FORMATTER.format(min);
        String maxOutput = FORMATTER.format(max);

        tooltipLines.add(Component.translatable("gui.anvilcraft.category.mesh.min_output", minOutput)
                .withStyle(ChatFormatting.GRAY));
        tooltipLines.add(Component.translatable("gui.anvilcraft.category.mesh.max_output", maxOutput)
                .withStyle(ChatFormatting.GRAY));
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(AnvilCraftJeiPlugin.MESH, MeshRecipeGroup.getAllRecipesGrouped());
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(Items.SCAFFOLDING), AnvilCraftJeiPlugin.MESH);
    }
}
