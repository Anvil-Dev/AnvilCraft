package dev.dubhe.anvilcraft.util;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.storage.loot.providers.number.BinomialDistributionGenerator;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class RecipeUtil {
    private static final byte CONSTANT_TYPE = 1;
    private static final byte UNIFORM_TYPE = 2;
    private static final byte BINOMIAL_TYPE = 3;
    private static final byte UNKNOWN_TYPE = -1;

    public static void toNetwork(RegistryFriendlyByteBuf buf, NumberProvider numberProvider) {
        switch (numberProvider) {
            case ConstantValue constantValue -> {
                buf.writeByte(CONSTANT_TYPE);
                buf.writeFloat(constantValue.value());
            }
            case UniformGenerator uniformGenerator -> {
                buf.writeByte(UNIFORM_TYPE);
                toNetwork(buf, uniformGenerator.min());
                toNetwork(buf, uniformGenerator.max());
            }
            case BinomialDistributionGenerator binomialDistributionGenerator -> {
                buf.writeByte(BINOMIAL_TYPE);
                toNetwork(buf, binomialDistributionGenerator.n());
                toNetwork(buf, binomialDistributionGenerator.p());
            }
            default -> buf.writeByte(UNKNOWN_TYPE);
        }
    }

    public static NumberProvider fromNetwork(RegistryFriendlyByteBuf buf) {
        return switch (buf.readByte()) {
            case CONSTANT_TYPE -> ConstantValue.exactly(buf.readFloat());
            case UNIFORM_TYPE -> new UniformGenerator(fromNetwork(buf), fromNetwork(buf));
            case BINOMIAL_TYPE -> new BinomialDistributionGenerator(fromNetwork(buf), fromNetwork(buf));
            default -> ConstantValue.exactly(1);
        };
    }

    public static double getExpectedValue(NumberProvider numberProvider) {
        return switch (numberProvider) {
            case ConstantValue constantValue -> constantValue.value();
            case UniformGenerator uniformGenerator -> (getExpectedValue(uniformGenerator.min())
                            + getExpectedValue(uniformGenerator.max()))
                    / 2;
            case BinomialDistributionGenerator binomialDistributionGenerator -> getExpectedValue(
                            binomialDistributionGenerator.n())
                    * getExpectedValue(binomialDistributionGenerator.p());
            default -> -1;
        };
    }

    public static boolean isIngredientsEqual(Ingredient first, Ingredient second) {
        if (first == second) return true;

        if (!first.isCustom() && !second.isCustom()) {
            ObjectArrayList<Ingredient.Value> firstValues = new ObjectArrayList<>(first.getValues());
            ObjectArrayList<Ingredient.Value> secondValues = new ObjectArrayList<>(second.getValues());

            if (firstValues.size() == secondValues.size()) {
                outer:
                for (int i = 0; i < firstValues.size(); i++) {
                    var firstValue = firstValues.get(i);

                    for (int j = 0; j < firstValues.size(); j++) {
                        if (isValuesEqual(firstValue, secondValues.get(j))) {
                            firstValues.remove(i);
                            secondValues.remove(j);
                            i--;

                            continue outer;
                        }
                    }
                    return false;
                }
                return true;
            }
        }
        return false;
    }

    private static boolean isValuesEqual(Ingredient.Value firstValue, Ingredient.Value secondValue) {
        Class<?> firstKlass = firstValue.getClass();
        Class<?> secondKlass = secondValue.getClass();

        if (firstKlass == secondKlass) {
            if (firstKlass == Ingredient.ItemValue.class) {
                return ItemStack.matches(
                        ((Ingredient.ItemValue) firstValue).item(), ((Ingredient.ItemValue) secondValue).item());
            } else if (firstKlass == Ingredient.TagValue.class) {
                return ((Ingredient.TagValue) firstValue).tag() == ((Ingredient.TagValue) secondValue).tag();
            } else {
                var firstItems = firstValue.getItems();
                var secondItems = secondValue.getItems();
                var len = firstItems.size();

                if (len == secondItems.size()) {
                    Iterator<ItemStack> firstIter = firstItems.iterator();
                    Iterator<ItemStack> secondIter = secondItems.iterator();

                    while (firstIter.hasNext()) {
                        if (!ItemStack.matches(firstIter.next(), secondIter.next())) {
                            return false;
                        }
                    }
                } else {
                    return false;
                }
                return true;
            }
        } else {
            return false;
        }
    }

    public static List<Object2IntMap.Entry<Ingredient>> mergeIngredient(List<Ingredient> ingredients) {
        Object2IntMap<Ingredient> margeIngredients = new Object2IntOpenHashMap<>();
        for (Ingredient ingredient : ingredients) {
            boolean flag = false;
            for (Ingredient key : margeIngredients.keySet()) {
                if (isIngredientsEqual(ingredient, key)) {
                    margeIngredients.put(key, margeIngredients.getInt(key) + 1);
                    flag = true;
                }
            }
            if (!flag) {
                margeIngredients.put(ingredient, 1);
            }
        }
        return new ArrayList<>(margeIngredients.object2IntEntrySet());
    }
}
