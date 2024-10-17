package dev.dubhe.anvilcraft.util;

import dev.dubhe.anvilcraft.recipe.anvil.input.IItemsInput;
import dev.dubhe.anvilcraft.recipe.multiblock.BlockPattern;
import dev.dubhe.anvilcraft.recipe.multiblock.BlockPredicateWithState;

import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.providers.number.BinomialDistributionGenerator;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

        for (int x = 0; x < pattern.getSize(); x++) {
            for (int y = 0; y < pattern.getSize(); y++) {
                for (int z = 0; z < pattern.getSize(); z++) {
                    BlockPredicateWithState predicate = pattern.getPredicate(x, y, z);
                    BlockState state = predicate.getBlock().defaultBlockState();
                    for (Property<?> property :
                        predicate.getBlock().getStateDefinition().getProperties()) {
                        if (predicate.hasProperty(property)) {
                            switch (property) {
                                case IntegerProperty integerProperty -> {
                                    Integer value = predicate.getPropertyValue(integerProperty);
                                    if (value != null) {
                                        state = state.setValue(integerProperty, value);
                                    }
                                }
                                case BooleanProperty boolProperty -> {
                                    Boolean value = predicate.getPropertyValue(boolProperty);
                                    if (value != null) {
                                        state = state.setValue(boolProperty, value);
                                    }
                                }
                                case DirectionProperty directionProperty -> {
                                    Direction value = predicate.getPropertyValue(directionProperty);
                                    if (value != null) {
                                        state = state.setValue(directionProperty, value);
                                    }
                                }
                                case EnumProperty<?> enumProperty -> {
                                    if (enumProperty.getValueClass() == Direction.Axis.class) {
                                        EnumProperty<Direction.Axis> axisProperty =
                                            (EnumProperty<Direction.Axis>) enumProperty;
                                        Direction.Axis value = predicate.getPropertyValue(axisProperty);
                                        if (value != null) {
                                            state = state.setValue(axisProperty, value);
                                        }
                                    }
                                }
                                default -> {
                                }
                            }
                        }
                    }
                    levelLike.setBlockState(new BlockPos(x, y, z), state);
                }
            }
        }
        // levelLike.setBlockState(new BlockPos(1, 1, 1), Blocks.TNT.defaultBlockState());
        // levelLike.setBlockState(new BlockPos(1, 0, 1), Blocks.REDSTONE_BLOCK.defaultBlockState());
        //
        // levelLike.setBlockState(new BlockPos(2, 2, 2), Blocks.DIAMOND_BLOCK.defaultBlockState());
        // levelLike.setBlockState(new BlockPos(2, 1, 2), ModBlocks.HEAVY_IRON_BLOCK.getDefaultState());
        // levelLike.setBlockState(new BlockPos(2, 0, 2), ModBlocks.HEAVY_IRON_COLUMN.getDefaultState());
        //
        // for (int i = 0; i < 4; i++) {
        //     levelLike.setBlockState(new BlockPos(3, i, 3), Blocks.NETHERITE_BLOCK.defaultBlockState());
        // }
        //
        // for (int i = 0; i <= 4; i++) {
        //     levelLike.setBlockState(new BlockPos(4, i, 4), Blocks.GLASS.defaultBlockState());
        // }
        //
        // for (int x = 0; x < 5; x++) {
        //     for (int z = 0; z < 5; z++) {
        //         levelLike.setBlockState(
        //                 new BlockPos(x, 0, z), Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 8));
        //     }
        // }
        // levelLike.setBlockState(BlockPos.ZERO, ModBlocks.MENGER_SPONGE.getDefaultState());

        return levelLike;
    }
}
