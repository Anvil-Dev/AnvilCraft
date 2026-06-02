package dev.dubhe.anvilcraft.integration.jei.util;

import com.google.common.collect.ImmutableList;
import dev.anvilcraft.lib.v2.util.NumberProviderUtil;
import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.anvilcraft.lib.v2.util.predicate.ChanceBlockState;
import dev.anvilcraft.lib.v2.util.predicate.ChanceItemStack;
import dev.dubhe.anvilcraft.recipe.sync.RecipesRecord;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
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

public class JeiRecipeUtil {
    private static final DecimalFormat FORMATTER = new DecimalFormat();

    public static <I extends RecipeInput, T extends Recipe<I>> List<T> getRecipesFromType(RecipeType<T> recipeType) {
        List<T> recipes = new ArrayList<>();
        for (RecipeHolder<T> holder : RecipesRecord.CLIENTSIDE.byType(recipeType)) {
            recipes.add(holder.value());
        }
        return recipes;
    }

    public static <I extends RecipeInput, T extends Recipe<I>> List<RecipeHolder<T>> getRecipeHoldersFromType(RecipeType<T> recipeType) {
        return new ArrayList<>(RecipesRecord.CLIENTSIDE.byType(recipeType));
    }

    public static void addInvisibleInput(IRecipeLayoutBuilder builder, BlockStatePredicate predicate) {
        JeiRecipeUtil.addInvisibleIngredients(builder, RecipeIngredientRole.INPUT, predicate);
    }

    public static void addInvisibleInputs(IRecipeLayoutBuilder builder, Iterable<BlockStatePredicate> predicates) {
        for (BlockStatePredicate predicate : predicates) {
            JeiRecipeUtil.addInvisibleIngredients(builder, RecipeIngredientRole.INPUT, predicate);
        }
    }

    public static void addInvisibleOutput(IRecipeLayoutBuilder builder, ChanceBlockState state) {
        JeiRecipeUtil.addInvisibleIngredients(builder, RecipeIngredientRole.OUTPUT, state);
    }

    public static void addInvisibleIngredients(IRecipeLayoutBuilder builder, RecipeIngredientRole role, BlockStatePredicate predicate) {
        builder.addInvisibleIngredients(role).addItemStacks(
            predicate.getBlocks().stream().map(holder -> new ItemStack(holder.value())).toList()
        );
    }

    public static void addInvisibleIngredients(IRecipeLayoutBuilder builder, RecipeIngredientRole role, ChanceBlockState state) {
        builder.addInvisibleIngredients(role).add(state.state().getBlock());
    }

    public static List<Component> getTooltips(NumberProvider provider) {
        ImmutableList.Builder<Component> tooltipLines = new ImmutableList.Builder<>();

        if (provider instanceof BinomialDistributionGenerator(NumberProvider n, NumberProvider p)) {
            if (n instanceof ConstantValue(float value) && value == 1) {
                String chance = FORMATTER.format(NumberProviderUtil.expected(p) * 100);
                tooltipLines.add(Component.translatable("gui.anvilcraft.category.chance", chance).withStyle(ChatFormatting.GRAY));
            } else {
                addAvgOutput(tooltipLines, NumberProviderUtil.expected(provider));
            }
            addMinMax(tooltipLines, 0, getMax(n));
        } else if (provider.getClass() != ConstantValue.class) {
            double val = NumberProviderUtil.expected(provider);
            if (val != -1) {
                addAvgOutput(tooltipLines, val);
                if (provider instanceof UniformGenerator) {
                    addMinMax(tooltipLines, getMin(provider), getMax(provider));
                }
            }
        } else {
            ConstantValue constant = (ConstantValue) provider;
            float value = constant.value();
            if (value != 1) {
                tooltipLines.add(Component.translatable(
                    "gui.anvilcraft.category.chance",
                    FORMATTER.format(value * 100)
                ).withStyle(ChatFormatting.GRAY));
            }
        }

        return tooltipLines.build();
    }

    public static void addTooltips(IRecipeSlotBuilder slot, int count, NumberProvider provider) {
        ImmutableList.Builder<Component> tooltipLines = new ImmutableList.Builder<>();

        if (provider instanceof BinomialDistributionGenerator(NumberProvider n, NumberProvider p)) {
            if (n instanceof ConstantValue(float value) && value == 1) {
                String chance = FORMATTER.format(NumberProviderUtil.expected(p) * 100);
                tooltipLines.add(Component.translatable("gui.anvilcraft.category.chance", chance)
                    .withStyle(ChatFormatting.GRAY));
            } else {
                addAvgOutput(tooltipLines, count * NumberProviderUtil.expected(provider));
            }
            addMinMax(tooltipLines, 0, getMax(n));
        } else if (provider.getClass() != ConstantValue.class) {
            double val = count * NumberProviderUtil.expected(provider);
            if (val != -1) {
                addAvgOutput(tooltipLines, val);
                if (provider instanceof UniformGenerator) {
                    addMinMax(tooltipLines, getMin(provider), getMax(provider));
                }
            }
        }

        slot.addRichTooltipCallback((slotView, tooltip) -> tooltip.addAll(tooltipLines.build()));
    }

    public static boolean isChance(List<ChanceItemStack> chanceItemStacks) {
        for (ChanceItemStack chanceItemStack : chanceItemStacks) {
            NumberProvider provider = chanceItemStack.count();
            if (provider instanceof BinomialDistributionGenerator) {
                return true;
            } else if (provider.getClass() != ConstantValue.class) {
                return true;
            }
        }
        return false;
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
