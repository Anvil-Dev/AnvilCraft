package dev.dubhe.anvilcraft.block.entity;

import dev.anvilcraft.lib.v2.recipe.InWorldRecipe;
import dev.anvilcraft.lib.v2.recipe.predicate.IRecipePredicate;
import dev.anvilcraft.lib.v2.recipe.predicate.item.HasItemIngredient;
import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeManager;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTriggers;
import dev.dubhe.anvilcraft.recipe.anvil.predicate.block.HasCauldron;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.ItemCompressRecipe;
import net.minecraft.core.HolderSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/** 保留落砧配方的注册顺序，按首个物品原料缩小每个锅格需要尝试的配方范围。 */
final class LargeCauldronRecipes {
    private static final Map<InWorldRecipeManager, LargeCauldronRecipes> CACHE = new WeakHashMap<>();

    private final List<RecipeHolder<InWorldRecipe>> registered;
    private final List<Candidate> fluids = new ArrayList<>();
    private final List<Candidate> items = new ArrayList<>();
    private final Map<Item, List<Candidate>> byItem = new HashMap<>();

    private LargeCauldronRecipes(Collection<RecipeHolder<InWorldRecipe>> holders) {
        this.registered = List.copyOf(holders);
        for (var holder : holders) {
            var recipe = holder.value();
            var anchor = firstIngredient(recipe);
            if (anchor != null) {
                this.items.add(new Candidate(holder, anchor));
            } else if (hasCauldron(recipe)) {
                this.fluids.add(new Candidate(holder, null));
            }
        }
    }

    static synchronized LargeCauldronRecipes get(ServerLevel level) {
        var manager = level.recipeAccess().anvillib$getInWorldRecipeManager();
        var holders = manager.recipeHolders.get(ModRecipeTriggers.ON_ANVIL_FALL_ON.get());
        var cached = CACHE.get(manager);
        if (cached == null || !cached.stillRegistered(holders)) {
            cached = new LargeCauldronRecipes(holders);
            CACHE.put(manager, cached);
        }
        return cached;
    }

    private boolean stillRegistered(Collection<RecipeHolder<InWorldRecipe>> holders) {
        if (this.registered.size() != holders.size()) {
            return false;
        }
        // 附属模组可在同一个管理器内用相同 ID 替换配方，RecipeHolder 的相等判断只比较 ID。
        int index = 0;
        for (var holder : holders) {
            if (this.registered.get(index++) != holder) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasCauldron(InWorldRecipe recipe) {
        return recipe.nonConflicting().stream().anyMatch(HasCauldron.class::isInstance)
            || recipe.conflicting().stream().anyMatch(HasCauldron.class::isInstance);
    }

    private static @Nullable HasItemIngredient firstIngredient(InWorldRecipe recipe) {
        var ingredient = firstIngredient(recipe.nonConflicting());
        return ingredient == null ? firstIngredient(recipe.conflicting()) : ingredient;
    }

    private static @Nullable HasItemIngredient firstIngredient(List<IRecipePredicate<?>> predicates) {
        for (var predicate : predicates) {
            if (predicate instanceof HasItemIngredient ingredient) {
                return ingredient;
            }
        }
        return null;
    }

    List<Candidate> candidates(ItemStack stack) {
        if (stack.isEmpty()) {
            return this.fluids;
        }
        return this.byItem.computeIfAbsent(stack.getItem(), item -> this.items.stream()
            .filter(candidate -> candidate.acceptsItemType(stack))
            .toList());
    }

    static boolean matchesAnchor(InWorldRecipe recipe, ItemStack stack) {
        var ingredient = firstIngredient(recipe);
        return ingredient != null && ingredient.getItem().test(stack);
    }

    static final class Previews {
        private static final Map<RecipeMap, Previews> WITHOUT_HELPERS = new WeakHashMap<>();
        private static final Map<RecipeMap, Previews> WITH_HELPERS = new WeakHashMap<>();

        // 方块实体只保留版本令牌，以便数据包重载后释放旧配方。
        final Object revision = new Object();
        private final Collection<RecipeHolder<?>> source;
        private final List<RecipeHolder<InWorldRecipe>> recipes = new ArrayList<>();
        private final List<RecipeHolder<InWorldRecipe>> fluids = new ArrayList<>();
        private final BitSet withoutItems = new BitSet();
        private final BitSet dynamicItems = new BitSet();
        private final Map<Item, BitSet> byItem = new HashMap<>();

        private Previews(Collection<RecipeHolder<?>> source, boolean compressionLast) {
            this.source = source;
            for (RecipeHolder<?> holder : source) {
                if (holder.value() instanceof InWorldRecipe recipe && recipe.trigger().equals(ModRecipeTriggers.ON_ANVIL_FALL_ON.get())) {
                    this.recipes.add(new RecipeHolder<>(holder.id(), recipe));
                }
            }
            this.recipes.sort(Comparator.<RecipeHolder<InWorldRecipe>>comparingInt(
                holder -> compressionLast && holder.value() instanceof ItemCompressRecipe ? 1 : 0
            ).thenComparing(Comparator.comparingInt((RecipeHolder<InWorldRecipe> holder) -> holder.value().priority()).reversed())
                .thenComparing(holder -> holder.id().identifier()));
            for (int i = 0; i < this.recipes.size(); i++) {
                var holder = this.recipes.get(i);
                var recipe = holder.value();
                if (firstIngredient(recipe) == null) {
                    this.withoutItems.set(i);
                    if (hasCauldron(recipe)) {
                        this.fluids.add(holder);
                    }
                } else {
                    this.indexItems(recipe, recipe.nonConflicting(), i);
                    this.indexItems(recipe, recipe.conflicting(), i);
                }
            }
        }

        static synchronized Previews get(RecipeMap recipes, boolean compressionLast) {
            var cache = compressionLast ? WITH_HELPERS : WITHOUT_HELPERS;
            var source = recipes.values();
            var cached = cache.get(recipes);
            // AnvilLib 追加配方时会在同一个 RecipeMap 中替换不可变的配方表。
            if (cached == null || cached.source != source) {
                cached = new Previews(source, compressionLast);
                cache.put(recipes, cached);
            }
            return cached;
        }

        private void indexItems(InWorldRecipe recipe, List<IRecipePredicate<?>> predicates, int index) {
            for (var predicate : predicates) {
                if (!(predicate instanceof HasItemIngredient ingredient)) {
                    continue;
                }
                var types = ingredient.getItem().items().orElse(null);
                if ((ingredient.getClass() == HasItemIngredient.class || recipe.getClass() == ItemCompressRecipe.class)
                    && types instanceof HolderSet.Direct<Item> direct) {
                    for (var type : direct) {
                        this.byItem.computeIfAbsent(type.value(), ignored -> new BitSet()).set(index);
                    }
                } else {
                    // 标签、通配原料和自定义谓词在查询时按实际物品栈判定。
                    this.dynamicItems.set(index);
                }
            }
        }

        List<RecipeHolder<InWorldRecipe>> fluids() {
            return this.fluids;
        }

        List<RecipeHolder<InWorldRecipe>> forItem(ItemStack stack) {
            var candidates = (BitSet) this.dynamicItems.clone();
            this.addItem(candidates, stack);
            return candidates.stream().mapToObj(this.recipes::get)
                .filter(holder -> matchesAnchor(holder.value(), stack)).toList();
        }

        List<RecipeHolder<InWorldRecipe>> forTank(List<ItemStack> stacks) {
            var candidates = new BitSet();
            boolean hasItems = false;
            for (var stack : stacks) {
                if (!stack.isEmpty()) {
                    hasItems = true;
                    this.addItem(candidates, stack);
                }
            }
            if (hasItems) {
                candidates.or(this.dynamicItems);
            }
            if (candidates.isEmpty() && this.fluids.isEmpty()) {
                return List.of();
            }
            // 鱼缸的落砧会先处理更高优先级的世界配方，它们也可能只检查周围的方块。
            candidates.or(this.withoutItems);
            return candidates.stream().mapToObj(this.recipes::get).toList();
        }

        private void addItem(BitSet candidates, ItemStack stack) {
            var direct = this.byItem.get(stack.getItem());
            if (direct != null) {
                candidates.or(direct);
            }
        }
    }

    record Candidate(RecipeHolder<InWorldRecipe> holder, @Nullable HasItemIngredient anchor) {
        private boolean acceptsItemType(ItemStack stack) {
            if (this.anchor == null) {
                return false;
            }
            // 内置压缩配方只定制原料来源过滤，仍使用固定的物品谓词。
            if (this.anchor.getClass() != HasItemIngredient.class && this.holder.value().getClass() != ItemCompressRecipe.class) {
                return true;
            }
            var types = this.anchor.getItem().items().orElse(null);
            // 标签和自定义集合可以重新绑定，只有明确列举的物品可以永久排除。
            return !(types instanceof HolderSet.Direct<Item>) || stack.is(types);
        }

        boolean matchesInput(ItemStack stack) {
            return this.anchor == null || this.anchor.getItem().test(stack);
        }
    }
}
