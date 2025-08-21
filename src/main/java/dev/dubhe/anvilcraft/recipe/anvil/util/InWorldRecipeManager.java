package dev.dubhe.anvilcraft.recipe.anvil.util;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.event.InWorldRecipeEvent;
import dev.dubhe.anvilcraft.recipe.anvil.InWorldRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.trigger.IRecipeTrigger;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.function.Supplier;

/**
 * 世界内配方管理器类，用于管理和触发世界内配方
 * 该类负责注册配方和根据触发器执行相应的配方
 */
public class InWorldRecipeManager {
    /**
     * 存储配方的映射表，键为配方触发器，值为配方集合
     */
    public final Multimap<IRecipeTrigger, @NotNull RecipeHolder<InWorldRecipe>> recipeHolders = MultimapBuilder.hashKeys()
        .<RecipeHolder<InWorldRecipe>>treeSetValues(Comparator.comparing(RecipeHolder::value))
        .build();

    /**
     * 构造一个新的世界内配方管理器，并注册一个默认配方
     */
    public InWorldRecipeManager() {
    }

    /**
     * 注册一个世界内配方
     *
     * @param recipe 要注册的配方
     */
    public void register(@NotNull RecipeHolder<InWorldRecipe> recipe) {
        recipeHolders.put(recipe.value().getTrigger(), recipe);
    }

    /**
     * 触发指定的配方触发器并执行匹配的配方
     *
     * @param trigger 配方触发器
     * @param ctx     配方上下文
     */
    public void trigger(IRecipeTrigger trigger, @NotNull InWorldRecipeContext ctx) {
        if (ctx.getLevel().isClientSide()) return;
        for (RecipeHolder<InWorldRecipe> holder : recipeHolders.get(trigger)) {
            InWorldRecipe recipe = holder.value();
            boolean accept = false;
            for (int i = 0; i < AnvilCraft.config.anvilEfficiency; i++) {
                if (!recipe.matches(ctx, ctx.getLevel())) {
                    if (!accept) break;
                    return;
                }
                accept = true;
                recipe.assemble(ctx, ctx.getLevel().registryAccess());
                NeoForge.EVENT_BUS.post(new InWorldRecipeEvent(recipe.getType(), holder.id(), recipe, ctx));
            }
            if (accept) break;
        }
    }

    /**
     * 触发指定的配方触发器并执行匹配的配方
     *
     * @param trigger 配方触发器
     * @param ctx     配方上下文
     */
    public void trigger(@NotNull Supplier<IRecipeTrigger> trigger, @NotNull InWorldRecipeContext ctx) {
        this.trigger(trigger.get(), ctx);
    }
}