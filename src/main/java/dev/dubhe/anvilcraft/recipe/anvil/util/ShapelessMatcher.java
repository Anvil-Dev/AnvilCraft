package dev.dubhe.anvilcraft.recipe.anvil.util;

import dev.dubhe.anvilcraft.recipe.anvil.IRecipePredicate;
import dev.dubhe.anvilcraft.recipe.anvil.InWorldRecipeContext;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * 无序匹配器
 * <p>
 * 用于处理无序配方谓词的匹配逻辑，支持不兼容和兼容的匹配方式
 * </p>
 */
public class ShapelessMatcher {
    /**
     * 检查谓词列表是否不兼容（即是否存在一种排列使得所有谓词都能匹配）
     *
     * @param predicates 谓词列表
     * @param ctx        配方上下文
     * @return 是否不兼容
     */
    public static boolean incompatible(@NotNull List<IRecipePredicate<?>> predicates, @NotNull InWorldRecipeContext ctx) {
        if (predicates.isEmpty()) return true;
        for (IRecipePredicate<?> predicate : predicates) {
            if (!predicate.test(ctx)) continue;
            List<IRecipePredicate<?>> next = new ArrayList<>();
            for (IRecipePredicate<?> predicate1 : predicates) {
                if (predicate1 == predicate) continue;
                next.add(predicate1);
            }
            ctx.push(predicate);
            if (next.isEmpty()) return true;
            boolean flag = incompatible(next, ctx);
            if (flag) return true;
            ctx.pop(predicate);
        }
        return false;
    }

    /**
     * 检查谓词列表是否兼容（即所有谓词都能同时匹配）
     *
     * @param predicates 谓词列表
     * @param ctx        配方上下文
     * @return 是否兼容
     */
    public static boolean compatible(@NotNull List<IRecipePredicate<?>> predicates, @NotNull InWorldRecipeContext ctx) {
        for (IRecipePredicate<?> predicate : predicates) {
            if (!predicate.test(ctx)) return false;
            ctx.push(predicate);
        }
        return true;
    }
}