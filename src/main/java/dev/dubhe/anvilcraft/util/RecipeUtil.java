package dev.dubhe.anvilcraft.util;

import dev.dubhe.anvilcraft.recipe.anvil.AbstractItemProcessRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.input.IItemsInput;
import dev.dubhe.anvilcraft.recipe.multiblock.BlockPattern;
import dev.dubhe.anvilcraft.recipe.multiblock.BlockPredicateWithState;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.providers.number.BinomialDistributionGenerator;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RecipeUtil {
    private static final byte CONSTANT_TYPE = 1;
    private static final byte UNIFORM_TYPE = 2;
    private static final byte BINOMIAL_TYPE = 3;
    private static final byte UNKNOWN_TYPE = -1;

    public static StreamCodec<RegistryFriendlyByteBuf, NumberProvider> NUMBER_PROVIDER_STREAM_CODEC = StreamCodec.of(
        RecipeUtil::toNetwork,
        RecipeUtil::fromNetwork
    );

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

    public static LootContext emptyLootContext(ServerLevel level) {
        return new LootContext.Builder(new LootParams(level, Map.of(), Map.of(), 0)).create(Optional.empty());
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
        Object2IntMap<Ingredient> margeIngredients = new Object2IntLinkedOpenHashMap<>();
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

    public static int getMaxCraftTime(IItemsInput input, List<Ingredient> ingredients) {
        Object2IntMap<Item> contents = new Object2IntOpenHashMap<>();
        Object2BooleanMap<Ingredient> ingredientFlags = new Object2BooleanOpenHashMap<>();
        Object2BooleanMap<Item> flags = new Object2BooleanOpenHashMap<>();
        for (Ingredient ingredient : ingredients) {
            ingredientFlags.put(ingredient, false);
        }
        for (ItemStack stack : input.items()) {
            if (stack.isEmpty()) continue;
            contents.mergeInt(stack.getItem(), stack.getCount(), Integer::sum);
            flags.put(stack.getItem(), false);
        }
        int times = 0;
        while (true) {
            for (Ingredient ingredient : ingredients) {
                for (Item item : contents.keySet()) {
                    if (ingredient.test(new ItemStack(item))) {
                        contents.put(item, contents.getInt(item) - 1);
                        ingredientFlags.put(ingredient, true);
                        flags.put(item, true);
                    }
                }
            }
            if (ingredientFlags.values().stream().anyMatch(flag -> !flag)
                || flags.values().stream().anyMatch(flag -> !flag)) {
                return 0;
            }
            if (contents.values().intStream().allMatch(i -> i >= 0)) {
                times += 1;
            } else {
                return times;
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static LevelLike asLevelLike(BlockPattern pattern) {
        @SuppressWarnings("DataFlowIssue")
        LevelLike levelLike = new LevelLike(Minecraft.getInstance().level);

        int size = pattern.getSize();
        for (int y = size - 1; y >= 0; y--) {
            for (int x = size - 1; x >= 0; x--) {
                for (int z = size - 1; z >= 0; z--) {
                    BlockPredicateWithState predicate = pattern.getPredicate(x, y, z);
                    BlockState state = predicate.getDefaultState();
                    if (state.isAir() && Math.max(levelLike.horizontalSize(), levelLike.verticalSize()) >= size)
                        continue;
                    levelLike.setBlockState(new BlockPos(x, y, z), state);
                }
            }
        }

        return levelLike;
    }

    public static <T extends AbstractItemProcessRecipe> int compareRecipeHolders(RecipeHolder<T> holderA, RecipeHolder<T> holderB) {
        T a = holderA.value();
        T b = holderB.value();
        if (a.mergedIngredients.size() == b.mergedIngredients.size()) {
            int countA = a.mergedIngredients.stream().mapToInt(Object2IntMap.Entry::getIntValue).sum();
            int countB = b.mergedIngredients.stream().mapToInt(Object2IntMap.Entry::getIntValue).sum();
            return countA - countB;
        }
        return a.mergedIngredients.size() - b.mergedIngredients.size();
    }


    public static boolean allIngredientEquals(NonNullList<Ingredient> ingredients) {
        if (ingredients.size() == 1) return true;
        for (int i = 0; i < ingredients.size(); i++) {
            for (int j = i; j < ingredients.size(); j++) {
                Ingredient a = ingredients.get(i);
                Ingredient b = ingredients.get(j);
                if (!isIngredientsEqual(a, b)) return false;
            }
        }
        return true;
    }

    @SafeVarargs
    public static boolean ingredientMatchingTags(Ingredient ingredient, TagKey<Item>... tagKey) {
        AtomicBoolean result = new AtomicBoolean(false);
        ICondition.IContext ctx = ServerLifecycleHooks.getCurrentServer().getServerResources().managers().getConditionContext();
        Map<ResourceLocation, Collection<Holder<Item>>> allTags = ctx.getAllTags(Registries.ITEM);
        for (Ingredient.Value value : ingredient.getValues()) {
            if (value instanceof Ingredient.TagValue(TagKey<Item> tag)) {
                if (allTags.containsKey(tag.location())) {
                    Collection<Holder<Item>> holders = allTags.get(tag.location());
                    if (holders.stream().anyMatch(it -> Arrays.stream(tagKey)
                        .anyMatch(tk -> it.value().getDefaultInstance().is(tk))
                    )) {
                        result.set(true);
                    }
                }
            }
            if (value instanceof Ingredient.ItemValue(ItemStack item)) {
                for (TagKey<Item> itemTagKey : tagKey) {
                    if (item.is(itemTagKey)) {
                        result.set(true);
                    }
                }
            }
        }
        if (ingredient.isCustom() && ingredient.getCustomIngredient() != null) {
            ICustomIngredient customIngredient = ingredient.getCustomIngredient();
            customIngredient.getItems().forEach(it -> {
                for (TagKey<Item> itemTagKey : tagKey) {
                    if (it.is(itemTagKey)) {
                        result.set(true);
                    }
                }
            });
        }
        return result.get();
    }
}
