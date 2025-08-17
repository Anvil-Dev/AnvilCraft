package dev.dubhe.anvilcraft.recipe.anvil;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModRecipeTriggers;
import dev.dubhe.anvilcraft.recipe.anvil.builder.InWorldRecipeBuilder;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 世界内配方管理器类，用于管理和触发世界内配方
 * 该类负责注册配方和根据触发器执行相应的配方
 */
public class InWorldRecipeManager {
    /**
     * 存储配方的映射表，键为配方触发器，值为配方集合
     */
    public final Map<IRecipeTrigger, Set<InWorldRecipe>> recipes = new ConcurrentHashMap<>();

    /**
     * 构造一个新的世界内配方管理器，并注册一个默认配方
     */
    public InWorldRecipeManager() {
        InWorldRecipe recipe = InWorldRecipeBuilder
            .incompatible(ModRecipeTriggers.ON_ANVIL_FALL_ON.get())
            .hasItemIngredient(ItemTags.LOGS)
            .hasItemIngredient(Items.BIRCH_LOG)
            .damageAnvil()
            .build();
        this.register(recipe);
    }

    /**
     * 注册一个世界内配方
     *
     * @param recipe 要注册的配方
     */
    public void register(@NotNull InWorldRecipe recipe) {
        Set<InWorldRecipe> recipeSet = this.recipes.computeIfAbsent(
            recipe.getTrigger(),
            k -> Collections.synchronizedSet(new TreeSet<>())
        );
        recipeSet.add(recipe);
    }

    /**
     * 触发指定的配方触发器并执行匹配的配方
     *
     * @param trigger 配方触发器
     * @param ctx     配方上下文
     */
    public void trigger(IRecipeTrigger trigger, @NotNull InWorldRecipeContext ctx) {
        if (ctx.getLevel().isClientSide()) return;
        Set<InWorldRecipe> recipeSet = recipes.getOrDefault(trigger, Collections.emptySet());
        for (InWorldRecipe recipe : recipeSet) {
            boolean accept = false;
            for (int i = 0; i < AnvilCraft.config.anvilEfficiency; i++) {
                if (!recipe.matches(ctx, ctx.getLevel())) {
                    if (!accept) break;
                    return;
                }
                accept = true;
                recipe.assemble(ctx, ctx.getLevel().registryAccess());
            }
            if (accept) break;
        }
    }
}