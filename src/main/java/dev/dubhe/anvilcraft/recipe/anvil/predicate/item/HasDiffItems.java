package dev.dubhe.anvilcraft.recipe.anvil.predicate.item;

import dev.anvilcraft.lib.v2.recipe.cache.item.ICacheInput;
import dev.anvilcraft.lib.v2.recipe.predicate.function.IPredicateFunction;
import dev.anvilcraft.lib.v2.recipe.predicate.item.HasItemIngredient;
import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeContext;
import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class HasDiffItems extends HasItemIngredient {
    /**
     * 构造一个物品原料条件谓词
     *
     * @param offset    偏移量
     * @param range     范围
     * @param item      物品原料谓词
     */
    public HasDiffItems(
        Vec3 offset,
        Vec3 range,
        ItemIngredientPredicate item,
        List<IPredicateFunction<?>> functions
    ) {
        super(offset, range, item, functions);
    }

    /**
     * 构造一个物品原料条件谓词
     *
     * @param offset    偏移量
     * @param range     范围
     */
    public static HasDiffItems fromPredicate(ItemIngredientPredicate predicate, Vec3 offset, Vec3 range) {
        return new HasDiffItems(offset, range, predicate, List.of());
    }

    @Override
    public boolean test(InWorldRecipeContext context) {
        ICacheInput cache = this.getItem(context);
        IntList counts = new IntArrayList();
        cache.apply(stack -> counts.add(stack.count()));
        if (counts.size() != this.getItem().count()) return false;
        for (int count : counts) {
            if (count < 1) return false;
        }
        return true;
    }
}
