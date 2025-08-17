package dev.dubhe.anvilcraft.recipe.anvil.cache;

import dev.dubhe.anvilcraft.recipe.JewelCraftingRecipe;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;

/**
 * 配方缓存管理类，用于管理各种类型的配方缓存
 * 该类提供了对宝石工艺配方缓存的加载、同步和访问功能
 */
public class RecipeCaches {
    /**
     * 宝石工艺配方缓存实例
     */
    private static JewelCraftingRecipeCache jewelCraftingRecipeCache;

    /**
     * 重新加载配方缓存
     *
     * @param recipeManager 配方管理器
     */
    public static void reload(RecipeManager recipeManager) {
        jewelCraftingRecipeCache = new JewelCraftingRecipeCache();
        jewelCraftingRecipeCache.buildJewelCraftingCache(recipeManager);
    }

    /**
     * 同步配方缓存到指定玩家
     *
     * @param player 服务器玩家
     */
    public static void sync(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, jewelCraftingRecipeCache.intoPacket());
    }

    /**
     * 网络同步配方缓存数据
     *
     * @param data 配方数据映射表
     */
    public static void networkSynced(Map<ItemStack, RecipeHolder<JewelCraftingRecipe>> data) {
        jewelCraftingRecipeCache = new JewelCraftingRecipeCache();
        jewelCraftingRecipeCache.buildJewelCraftingCache(data);
    }

    /**
     * 卸载配方缓存
     */
    public static void unload() {
        jewelCraftingRecipeCache = null;
    }

    /**
     * 根据结果物品获取宝石工艺配方
     *
     * @param stack 结果物品堆
     * @return 宝石工艺配方持有者，如果不存在则返回null
     */
    public static @Nullable RecipeHolder<JewelCraftingRecipe> getJewelRecipeByResult(ItemStack stack) {
        return jewelCraftingRecipeCache.getJewelRecipeByResult(stack);
    }

    /**
     * 获取所有宝石工艺结果物品
     *
     * @return 宝石工艺结果物品集合
     */
    public static Set<Item> getAllJewelResultItem() {
        return jewelCraftingRecipeCache.getAllJewelResultItem();
    }
}