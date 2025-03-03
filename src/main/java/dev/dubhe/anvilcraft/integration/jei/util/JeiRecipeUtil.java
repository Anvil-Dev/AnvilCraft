package dev.dubhe.anvilcraft.integration.jei.util;

import com.google.common.collect.ImmutableList;
import dev.dubhe.anvilcraft.util.RecipeUtil;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.storage.loot.providers.number.BinomialDistributionGenerator;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class JeiRecipeUtil {
    private static final DecimalFormat FORMATTER = new DecimalFormat();

    public static <I extends RecipeInput, T extends Recipe<I>> List<T> getRecipesFromType(RecipeType<T> recipeType) {
        return Minecraft.getInstance().getConnection().getRecipeManager().getAllRecipesFor(recipeType).stream()
            .map(RecipeHolder::value)
            .collect(Collectors.toCollection(ArrayList::new));
    }

    public static <I extends RecipeInput, T extends Recipe<I>> List<RecipeHolder<T>> getRecipeHoldersFromType(
        RecipeType<T> recipeType) {
        return new ArrayList<>(
            Minecraft.getInstance().getConnection().getRecipeManager().getAllRecipesFor(recipeType));
    }

    public static void addTooltips(IRecipeSlotBuilder slot, NumberProvider provider) {
        ImmutableList.Builder<Component> tooltipLines = new ImmutableList.Builder<>();

        if (provider instanceof BinomialDistributionGenerator binomial) {
            if (binomial.n() instanceof ConstantValue constantValue && constantValue.value() == 1) {
                String chance = FORMATTER.format(RecipeUtil.getExpectedValue(binomial.p()) * 100);
                tooltipLines.add(Component.translatable("gui.anvilcraft.category.chance", chance)
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
        tooltipLines.add(Component.translatable("gui.anvilcraft.category.average_output", avgOutput)
            .withStyle(ChatFormatting.GRAY));
    }

    private static void addMinMax(ImmutableList.Builder<Component> tooltipLines, double min, double max) {
        String minOutput = FORMATTER.format(min);
        String maxOutput = FORMATTER.format(max);

        tooltipLines.add(Component.translatable("gui.anvilcraft.category.min_output", minOutput)
            .withStyle(ChatFormatting.GRAY));
        tooltipLines.add(Component.translatable("gui.anvilcraft.category.max_output", maxOutput)
            .withStyle(ChatFormatting.GRAY));
    }
}
