package dev.dubhe.anvilcraft.integration.jei.util;

import com.google.common.collect.ImmutableList;
import dev.anvilcraft.lib.v2.util.NumberProviderUtil;
import dev.anvilcraft.lib.v2.util.predicate.ChanceItemStack;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
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
import java.util.function.IntFunction;
import java.util.stream.Collectors;

public class JeiRecipeUtil {
    private static final DecimalFormat FORMATTER = new DecimalFormat();

    public static <I extends RecipeInput, T extends Recipe<I>> List<T> getRecipesFromType(RecipeType<T> recipeType) {
        return Minecraft.getInstance()
            .getConnection()
            .getRecipeManager()
            .getAllRecipesFor(recipeType)
            .stream()
            .map(RecipeHolder::value)
            .collect(Collectors.toCollection(ArrayList::new));
    }

    public static <I extends RecipeInput, T extends Recipe<I>> List<RecipeHolder<T>> getRecipeHoldersFromType(
        RecipeType<T> recipeType
    ) {
        return new ArrayList<>(
            Minecraft.getInstance()
                .getConnection()
                .getRecipeManager()
                .getAllRecipesFor(recipeType)
        );
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

    /**
     * 单个产出是否为概率产出（非必定掉落）。
     *
     * <p>必定掉落指数量为 {@link ConstantValue}；二项分布或其他波动数量都视为概率产出。
     */
    public static boolean isChance(ChanceItemStack chanceItemStack) {
        NumberProvider provider = chanceItemStack.count();
        return provider instanceof BinomialDistributionGenerator || provider.getClass() != ConstantValue.class;
    }

    /**
     * 按产出逐个选择槽位样式：必定掉落用普通槽，概率掉落用虚影槽。
     *
     * <p>整组产出只要有一个带概率便全部画成虚影会误导玩家，故逐格判断。
     *
     * @param guaranteed 必定掉落使用的槽位贴图
     * @param chance     概率掉落使用的槽位贴图
     */
    public static IntFunction<IDrawable> outputSlotFor(
        List<ChanceItemStack> results,
        IDrawable guaranteed,
        IDrawable chance
    ) {
        return index -> isChance(results.get(index)) ? chance : guaranteed;
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
