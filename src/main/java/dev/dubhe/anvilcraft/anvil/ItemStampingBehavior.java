package dev.dubhe.anvilcraft.anvil;

import dev.dubhe.anvilcraft.api.anvil.IAnvilBehavior;
import dev.dubhe.anvilcraft.api.event.AnvilEvent;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.StampingUniqueItemsRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.input.ItemProcessInput;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.components.ChanceItemStack;
import dev.dubhe.anvilcraft.util.AnvilUtil;
import dev.dubhe.anvilcraft.util.RecipeUtil;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.Optional;

public class ItemStampingBehavior implements IAnvilBehavior {
    @Override
    public boolean handle(
        Level level,
        BlockPos hitBlockPos,
        BlockState hitBlockState,
        float fallDistance,
        AnvilEvent.OnLand event
    ) {
        return ItemStampingBehavior.itemProcess(
            ModRecipeTypes.STAMPING_UNIQUE_ITEMS_TYPE.get(),
            level,
            hitBlockPos,
            hitBlockPos.getCenter().add(0, 0.25, 0)
        );
    }

    public static boolean itemProcess(
        RecipeType<StampingUniqueItemsRecipe> recipeType,
        Level level,
        final BlockPos itemPos,
        final Vec3 resultPos
    ) {
        Map<ItemEntity, ItemStack> items = level.getEntitiesOfClass(ItemEntity.class, new AABB(itemPos)).stream()
            .map(it -> Map.entry(it, it.getItem()))
            .collect(Util.toMap());

        ItemProcessInput input = new ItemProcessInput(items.values().stream().toList());
        Optional<RecipeHolder<StampingUniqueItemsRecipe>> recipeOptional = level.getRecipeManager()
            .getRecipesFor(recipeType, input, level)
            .stream()
            .max(ItemStampingBehavior::compareRecipeHolders);
        if (recipeOptional.isPresent()) {
            RecipeHolder<StampingUniqueItemsRecipe> recipe = recipeOptional.get();
            int times = recipe.value().getMaxCraftTime();
            Object2IntMap<Item> results = new Object2IntOpenHashMap<>();
            LootContext context;
            if (level instanceof ServerLevel serverLevel) {
                context = RecipeUtil.emptyLootContext(serverLevel);
            } else {
                return false;
            }

            for (int i = 0; i < times; i++) {
                for (Ingredient ingredient : recipe.value().getIngredients()) {
                    for (ItemStack stack : items.values()) {
                        if (ingredient.test(stack)) {
                            if (stack.hasCraftingRemainingItem()) {
                                ItemStack remain = stack.getCraftingRemainingItem();
                                results.mergeInt(remain.getItem(), remain.getCount(), Integer::sum);
                            }
                            stack.shrink(1);
                            break;
                        }
                    }
                }
                for (ChanceItemStack stack : recipe.value().getResults()) {
                    int amount = stack.getStack().getCount() * stack.getCount().getInt(context);
                    results.mergeInt(stack.getStack().getItem(), amount, Integer::sum);
                }
            }
            AnvilUtil.dropItems(
                results.object2IntEntrySet().stream()
                    .map(entry -> new ItemStack(entry.getKey(), entry.getIntValue()))
                    .toList(),
                level,
                resultPos
            );
            items.forEach((k, v) -> {
                if (v.isEmpty()) {
                    k.discard();
                    return;
                }
                k.setItem(v.copy());
            });
            return true;
        }
        return false;
    }

    public static int compareRecipeHolders(
        RecipeHolder<StampingUniqueItemsRecipe> holderA,
        RecipeHolder<StampingUniqueItemsRecipe> holderB
    ) {
        StampingUniqueItemsRecipe a = holderA.value();
        StampingUniqueItemsRecipe b = holderB.value();
        if (a.mergedIngredients.size() == b.mergedIngredients.size()) {
            int countA = a.mergedIngredients.stream().mapToInt(Object2IntMap.Entry::getIntValue).sum();
            int countB = b.mergedIngredients.stream().mapToInt(Object2IntMap.Entry::getIntValue).sum();
            return countA - countB;
        }
        return a.mergedIngredients.size() - b.mergedIngredients.size();
    }
}
