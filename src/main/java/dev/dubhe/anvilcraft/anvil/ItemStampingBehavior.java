package dev.dubhe.anvilcraft.anvil;

import dev.anvilcraft.lib.v2.util.predicate.ChanceItemStack;
import dev.dubhe.anvilcraft.api.anvil.IAnvilBehavior;
import dev.dubhe.anvilcraft.api.event.AnvilEvent;
import dev.dubhe.anvilcraft.api.itemhandler.ItemHandlerUtil;
import dev.dubhe.anvilcraft.block.entity.StampingPlatformBlockEntity;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.StampingUniqueItemsRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.input.ItemProcessInput;
import dev.dubhe.anvilcraft.util.AnvilUtil;
import dev.dubhe.anvilcraft.util.RecipeUtil;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 冲压台行为：铁砧砸落时直接读取冲压台方块实体中的原料执行冲压配方。
 */
public class ItemStampingBehavior implements IAnvilBehavior {
    @Override
    public boolean handle(
        Level level,
        BlockPos hitBlockPos,
        BlockState hitBlockState,
        float fallDistance,
        AnvilEvent.OnLand event
    ) {
        if (!(level.getBlockEntity(hitBlockPos) instanceof StampingPlatformBlockEntity platform)) return false;
        return ItemStampingBehavior.processPlatform(platform, level);
    }

    /**
     * 从冲压台原料槽读取物品执行冲压配方，产物优先写回产物槽，放不下的掉落在台面上。
     */
    public static boolean processPlatform(StampingPlatformBlockEntity platform, Level level) {
        if (!(level instanceof ServerLevel serverLevel)) return false;
        IItemHandler inputHandler = platform.getInput();
        List<ItemStack> items = new ArrayList<>();
        for (int slot = 0; slot < inputHandler.getSlots(); slot++) {
            ItemStack stack = inputHandler.getStackInSlot(slot);
            if (!stack.isEmpty()) items.add(stack);
        }
        if (items.isEmpty()) return false;

        ItemProcessInput input = new ItemProcessInput(items);
        Optional<RecipeHolder<StampingUniqueItemsRecipe>> recipeOptional = level.getRecipeManager()
            .getRecipesFor(ModRecipeTypes.STAMPING_UNIQUE_ITEMS_TYPE.get(), input, level)
            .stream()
            .max(ItemStampingBehavior::compareRecipeHolders);
        if (recipeOptional.isEmpty()) return false;

        RecipeHolder<StampingUniqueItemsRecipe> holder = recipeOptional.get();
        int times = holder.value().getMaxCraftTime();
        LootContext context = RecipeUtil.emptyLootContext(serverLevel);
        Object2IntMap<Item> results = new Object2IntOpenHashMap<>();

        for (int time = 0; time < times; time++) {
            for (Ingredient ingredient : holder.value().getIngredients()) {
                for (int slot = 0; slot < inputHandler.getSlots(); slot++) {
                    ItemStack stackInSlot = inputHandler.getStackInSlot(slot);
                    if (!ingredient.test(stackInSlot)) continue;
                    ItemStack extracted = inputHandler.extractItem(slot, 1, false);
                    if (extracted.hasCraftingRemainingItem()) {
                        ItemStack remain = extracted.getCraftingRemainingItem();
                        results.mergeInt(remain.getItem(), remain.getCount(), Integer::sum);
                    }
                    break;
                }
            }
            for (ChanceItemStack stack : holder.value().getResults()) {
                int amount = stack.stack().getCount() * stack.count().getInt(context);
                results.mergeInt(stack.stack().getItem(), amount, Integer::sum);
            }
        }

        insertResults(platform, level, results);
        return true;
    }

    private static void insertResults(
        StampingPlatformBlockEntity platform,
        Level level,
        Object2IntMap<Item> results
    ) {
        IItemHandler outputHandler = platform.getOutput();
        Vec3 resultPos = platform.getBlockPos().getCenter();
        List<ItemStack> overflow = new ArrayList<>();
        for (Object2IntMap.Entry<Item> entry : results.object2IntEntrySet()) {
            int count = entry.getIntValue();
            int maxStackSize = entry.getKey().getDefaultMaxStackSize();
            while (count > 0) {
                ItemStack stack = new ItemStack(entry.getKey(), Math.min(count, maxStackSize));
                ItemStack rest = ItemHandlerUtil.insertItem(outputHandler, stack, false);
                int inserted = stack.getCount() - rest.getCount();
                count -= inserted;
                if (inserted == 0) {
                    stack.setCount(count);
                    overflow.add(stack);
                    break;
                }
            }
        }
        if (!overflow.isEmpty()) {
            AnvilUtil.dropItems(overflow, level, resultPos);
        }
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
