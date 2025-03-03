package dev.dubhe.anvilcraft.api.anvil.impl;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.anvil.IAnvilBehavior;
import dev.dubhe.anvilcraft.api.event.anvil.AnvilFallOnLandEvent;
import dev.dubhe.anvilcraft.recipe.anvil.MeshRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.cache.RecipeCaches;
import dev.dubhe.anvilcraft.util.AnvilUtil;
import dev.dubhe.anvilcraft.util.RecipeUtil;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ItemMeshBehavior implements IAnvilBehavior {
    @Override
    public boolean handle(
        Level level,
        BlockPos hitBlockPos,
        BlockState hitBlockState,
        float fallDistance,
        AnvilFallOnLandEvent event
    ) {
        BlockPos pos = hitBlockPos.above();
        Map<ItemEntity, ItemStack> items = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos)).stream()
            .map(it -> Map.entry(it, it.getItem()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        int processed = 0;
        for (ItemStack stack : items.values()) {
            List<RecipeHolder<MeshRecipe>> cacheMeshRecipes = RecipeCaches.getCacheMeshRecipes(stack);
            if (cacheMeshRecipes != null && !cacheMeshRecipes.isEmpty()) {
                LootContext context = RecipeUtil.emptyLootContext((ServerLevel) level);
                Object2IntMap<Item> itemCounts = new Object2IntOpenHashMap<>();
                int times = stack.getCount();
                for (int i = 0; i < times; i++) {
                    for (RecipeHolder<MeshRecipe> recipe : cacheMeshRecipes) {
                        int amount = recipe.value().resultAmount.getInt(context);
                        itemCounts.mergeInt(recipe.value().result.getItem(), amount, Integer::sum);
                    }
                    stack.shrink(1);
                    processed++;
                    if (processed >= AnvilCraft.config.anvilEfficiency) {
                        break;
                    }
                }
                List<ItemStack> outputs = itemCounts.object2IntEntrySet().stream()
                    .map(entry -> new ItemStack(entry.getKey(), entry.getIntValue()))
                    .toList();
                AnvilUtil.dropItems(outputs, level, pos.below().getCenter());
            }
            if (processed >= AnvilCraft.config.anvilEfficiency) {
                break;
            }
        }
        items.forEach((k, v) -> {
            if (v.isEmpty()) {
                k.discard();
                return;
            }
            k.setItem(v.copy());
        });
        return false;
    }
}
