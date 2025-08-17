package dev.dubhe.anvilcraft.recipe.generate;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Table;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.MeshRecipe;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.AzaleaBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SaplingBlock;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MeshRecipeGeneratingCache extends BaseGeneratingCache<MeshRecipe> {
    private static final Logger logger = logger();

    private final HashMultimap<Item, Item> leavesAndSaplings = HashMultimap.create();

    public MeshRecipeGeneratingCache(HolderLookup.Provider registries) {
        super(registries, "mesh", "mesh recipe");
        Table<String, Item, List<Item>> treeIdAndLeavesAndSaplings = HashBasedTable.create();
        for (Holder<Item> holder : registries.lookupOrThrow(Registries.ITEM).listElements().toList()) {
            if (holder.value() instanceof BlockItem blockItem && blockItem.getBlock() instanceof LeavesBlock block) {
                ResourceLocation leavesId = BuiltInRegistries.ITEM.getKey(blockItem);
                logger.debug(
                    "Add a leaves block {} for generating mesh recipes", leavesId);
                treeIdAndLeavesAndSaplings.put(getTreeId(block, leavesId), blockItem, new ArrayList<>());
            }
        }
        for (Holder<Item> holder : registries.lookupOrThrow(Registries.ITEM).listElements().toList()) {
            if (holder.value() instanceof BlockItem blockItem && (
                blockItem.getBlock() instanceof SaplingBlock || blockItem.getBlock() instanceof AzaleaBlock
            )) {
                ResourceLocation saplingId = BuiltInRegistries.ITEM.getKey(blockItem);
                logger.debug(
                    "Add a sapling {} for generating mesh recipes", saplingId);
                String treeId = getTreeId(blockItem.getBlock(), saplingId);
                if (treeIdAndLeavesAndSaplings.containsRow(treeId)) {
                    treeIdAndLeavesAndSaplings.row(treeId).values().forEach(list -> list.add(blockItem));
                }
            }
        }
        for (Map<Item, List<Item>> leavesAndSaplings : treeIdAndLeavesAndSaplings.rowMap().values()) {
            leavesAndSaplings.forEach((leaves, saplings) -> {
                if (saplings.isEmpty()) return;
                this.leavesAndSaplings.putAll(leaves, saplings);
            });
        }
    }

    @Override
    public Optional<List<RecipeHolder<MeshRecipe>>> buildRecipes() {
        if (this.leavesAndSaplings.isEmpty()) return Optional.empty();

        List<RecipeHolder<MeshRecipe>> recipeHolders = new ArrayList<>();

        for (Item leaves : this.leavesAndSaplings.keySet()) {
            Optional<ResourceKey<Item>> leavesKey = BuiltInRegistries.ITEM.getResourceKey(leaves);
            if (leavesKey.isEmpty()) continue;
            MeshRecipe recipeLeaves = MeshRecipe.builder()
                .requires(leaves)
                .result(leaves.getDefaultInstance(), 0.5f)
                .buildRecipe();
            recipeHolders.add(new RecipeHolder<>(generateRecipeId("leaves", leaves, leaves), recipeLeaves));
            for (Item sapling : this.leavesAndSaplings.get(leaves)) {
                MeshRecipe recipeSapling = MeshRecipe.builder()
                    .requires(leaves)
                    .result(sapling.getDefaultInstance(), 0.2f)
                    .buildRecipe();
                recipeHolders.add(new RecipeHolder<>(generateRecipeId("leaves", leaves, sapling), recipeSapling));
            }
        }

        return Optional.of(recipeHolders);
    }

    private static String getTreeId(Block source, ResourceLocation idFull) {
        int lastUnderscore = idFull.getPath().trim().lastIndexOf('_');
        if (lastUnderscore == -1 || (source instanceof BushBlock && !idFull.getPath().contains("sapling"))) return idFull.getPath();
        return idFull.getPath().trim().substring(0, lastUnderscore);
    }
}
