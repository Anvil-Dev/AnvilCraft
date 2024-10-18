package dev.dubhe.anvilcraft.recipe.anvil.cache;

import dev.dubhe.anvilcraft.recipe.JewelCraftingRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.MeshRecipe;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;

import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class RecipeCaches {
    private static MeshRecipeCache meshRecipeCache;
    private static JewelCraftingRecipeCache jewelCraftingRecipeCache;

    public static void reload(RecipeManager recipeManager) {
        meshRecipeCache = new MeshRecipeCache(recipeManager);
        jewelCraftingRecipeCache = new JewelCraftingRecipeCache();
        jewelCraftingRecipeCache.buildJewelCraftingCache(recipeManager);
    }

    public static void sync(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, jewelCraftingRecipeCache.intoPacket());
    }

    public static void networkSynced(Map<ItemStack, RecipeHolder<JewelCraftingRecipe>> data) {
        jewelCraftingRecipeCache = new JewelCraftingRecipeCache();
        jewelCraftingRecipeCache.buildJewelCraftingCache(data);
    }

    public static void unload() {
        meshRecipeCache = null;
        jewelCraftingRecipeCache = null;
    }

    public static @Nullable List<RecipeHolder<MeshRecipe>> getCacheMeshRecipes(ItemStack stack) {
        return meshRecipeCache.getMeshRecipes(stack);
    }

    public static @Nullable RecipeHolder<JewelCraftingRecipe> getJewelRecipeByResult(ItemStack stack) {
        return jewelCraftingRecipeCache.getJewelRecipeByResult(stack);
    }

    public static Set<Item> getAllJewelResultItem() {
        return jewelCraftingRecipeCache.getAllJewelResultItem();
    }
}
