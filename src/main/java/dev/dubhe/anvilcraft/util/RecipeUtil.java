package dev.dubhe.anvilcraft.util;

import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RecipeUtil {
    public static final ItemIngredientPredicate EMPTY_ITEM_INGREDIENT = ItemIngredientPredicate.Builder.item().build();

    public static LootContext emptyLootContext(ServerLevel level) {
        return new LootContext.Builder(new LootParams(level, Map.of(), Map.of(), 0)).create(Optional.empty());
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
                    BlockState state = getStateForRender(predicate);
                    if (state.isAir() && Math.max(levelLike.horizontalSize(), levelLike.verticalSize()) >= size) continue;
                    levelLike.setBlockState(new BlockPos(x, y, z), state);
                }
            }
        }

        return levelLike;
    }

    /**
     * 获取用于渲染的 BlockState。标签谓词解析为标签中的第一个方块，方块谓词使用默认状态。
     */
    @OnlyIn(Dist.CLIENT)
    private static BlockState getStateForRender(BlockPredicateWithState predicate) {
        if (predicate.getTag() != null) {
            var blocks = BuiltInRegistries.BLOCK.getTag(predicate.getTag())
                .map(tag -> tag.stream().map(Holder::value).toList())
                .orElse(List.of());
            if (!blocks.isEmpty()) {
                return blocks.getFirst().defaultBlockState();
            }
            return Blocks.AIR.defaultBlockState();
        }
        return predicate.getDefaultState();
    }

    /**
     * 标签渲染槽位：记录需要动态切换方块的模式位置及其可用方块列表。
     */
    public record TagRenderSlot(BlockPos pos, List<Block> blocks) {}

    /**
     * 收集模式中所有标签谓词的位置及其可用方块列表。
     */
    @OnlyIn(Dist.CLIENT)
    public static List<TagRenderSlot> getTagRenderSlots(BlockPattern pattern) {
        List<TagRenderSlot> slots = new ArrayList<>();
        int size = pattern.getSize();
        for (int y = size - 1; y >= 0; y--) {
            for (int x = size - 1; x >= 0; x--) {
                for (int z = size - 1; z >= 0; z--) {
                    BlockPredicateWithState predicate = pattern.getPredicate(x, y, z);
                    if (predicate.getTag() != null) {
                        var blocks = BuiltInRegistries.BLOCK.getTag(predicate.getTag())
                            .map(tag -> tag.stream().map(Holder::value).toList())
                            .orElse(List.of());
                        if (!blocks.isEmpty()) {
                            slots.add(new TagRenderSlot(new BlockPos(x, y, z), blocks));
                        }
                    }
                }
            }
        }
        return slots;
    }

    /**
     * 根据变体索引更新 LevelLike 中所有标签槽位的方块状态。
     */
    @OnlyIn(Dist.CLIENT)
    public static void cycleTagBlocks(LevelLike level, List<TagRenderSlot> slots, int variantIndex) {
        if (slots.isEmpty()) return;
        for (TagRenderSlot slot : slots) {
            int idx = variantIndex % slot.blocks().size();
            BlockState state = slot.blocks().get(idx).defaultBlockState();
            level.setBlockState(slot.pos(), state);
        }
    }
}
