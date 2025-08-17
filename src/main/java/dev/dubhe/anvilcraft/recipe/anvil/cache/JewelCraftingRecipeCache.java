package dev.dubhe.anvilcraft.recipe.anvil.cache;

import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.network.RecipeCacheSyncPacket;
import dev.dubhe.anvilcraft.recipe.JewelCraftingRecipe;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.Util;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;
import java.util.Set;

/**
 * 宝石工艺配方缓存类，用于缓存和管理宝石工艺配方
 * 该类提供了对宝石工艺配方的缓存、构建和同步功能
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class JewelCraftingRecipeCache {
    /**
     * 宝石工艺配方缓存映射表
     */
    private Map<Item, RecipeHolder<JewelCraftingRecipe>> jewelCraftingCache;

    /**
     * 根据结果物品获取宝石工艺配方
     *
     * @param result 结果物品堆
     * @return 宝石工艺配方持有者，如果不存在则返回null
     */
    public @Nullable RecipeHolder<JewelCraftingRecipe> getJewelRecipeByResult(ItemStack result) {
        return jewelCraftingCache.get(result.getItem());
    }

    /**
     * 获取所有宝石工艺结果物品
     *
     * @return 宝石工艺结果物品集合
     */
    public Set<Item> getAllJewelResultItem() {
        return jewelCraftingCache.keySet();
    }

    /**
     * 根据配方管理器构建宝石工艺配方缓存
     *
     * @param recipeManager 配方管理器
     */
    public void buildJewelCraftingCache(RecipeManager recipeManager) {
        jewelCraftingCache = recipeManager.getAllRecipesFor(ModRecipeTypes.JEWEL_CRAFTING_TYPE.get())
            .stream()
            .map(it -> Map.entry(it.value().result.getItem(), it))
            .collect(Util.toMap());
    }

    /**
     * 根据数据映射表构建宝石工艺配方缓存
     *
     * @param data 数据映射表
     */
    public void buildJewelCraftingCache(Map<ItemStack, RecipeHolder<JewelCraftingRecipe>> data) {
        jewelCraftingCache = data.entrySet()
            .stream()
            .map(it -> Map.entry(it.getKey().getItem(), it.getValue()))
            .collect(Util.toMap());
    }

    /**
     * 转换为配方缓存同步数据包
     *
     * @return 配方缓存同步数据包
     */
    public RecipeCacheSyncPacket intoPacket() {
        return new RecipeCacheSyncPacket(
            jewelCraftingCache.entrySet()
                .stream()
                .map(it -> Map.entry(it.getKey().getDefaultInstance(), it.getValue()))
                .collect(Util.toMap())
        );
    }
}