package dev.dubhe.anvilcraft.api.anvil.impl;

import dev.dubhe.anvilcraft.api.anvil.AnvilBehavior;
import dev.dubhe.anvilcraft.api.event.entity.AnvilFallOnLandEvent;
import dev.dubhe.anvilcraft.recipe.MeshRecipe;
import dev.dubhe.anvilcraft.recipe.cache.RecipeCaches;
import dev.dubhe.anvilcraft.util.AnvilUtil;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ItemMeshBehavior implements AnvilBehavior {
    @Override
    public void handle(Level level, BlockPos hitBlockPos, BlockState hitBlockState, float fallDistance, AnvilFallOnLandEvent event) {
        BlockPos pos = hitBlockPos.above();
        List<ItemEntity> entities = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos));
        for (ItemEntity entity : entities) {
            ItemStack stack = entity.getItem();
            List<RecipeHolder<MeshRecipe>> cacheMeshRecipes = RecipeCaches.getCacheMeshRecipes(stack);
            if (cacheMeshRecipes != null && !cacheMeshRecipes.isEmpty()) {
                LootContext context = new LootContext.Builder(
                    new LootParams((ServerLevel) level, Map.of(), Map.of(), 0))
                    .create(Optional.empty());
                Object2IntMap<Item> itemCounts = new Object2IntOpenHashMap<>();
                for (int i = 0; i < stack.getCount(); i++) {
                    for (RecipeHolder<MeshRecipe> recipe : cacheMeshRecipes) {
                        int amount = recipe.value().resultAmount.getInt(context);
                        itemCounts.mergeInt(recipe.value().result.getItem(), amount, Integer::sum);
                    }
                }
                List<ItemStack> outputs = itemCounts.object2IntEntrySet().stream()
                    .map(entry -> new ItemStack(entry.getKey(), entry.getIntValue()))
                    .toList();
                AnvilUtil.dropItems(outputs, level, pos.below().getCenter());
                entity.remove(Entity.RemovalReason.DISCARDED);
            }
        }
    }
}
