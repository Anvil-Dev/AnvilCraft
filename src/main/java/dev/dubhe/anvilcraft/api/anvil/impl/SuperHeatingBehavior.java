package dev.dubhe.anvilcraft.api.anvil.impl;

import dev.dubhe.anvilcraft.api.anvil.IAnvilBehavior;
import dev.dubhe.anvilcraft.api.event.anvil.AnvilFallOnLandEvent;
import dev.dubhe.anvilcraft.block.HeaterBlock;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModItemTags;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.ChanceItemStack;
import dev.dubhe.anvilcraft.recipe.anvil.SuperHeatingRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.input.ItemProcessInput;
import dev.dubhe.anvilcraft.util.AnvilUtil;
import dev.dubhe.anvilcraft.util.CauldronUtil;
import dev.dubhe.anvilcraft.util.RecipeUtil;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.BlastingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SuperHeatingBehavior implements IAnvilBehavior {
    @SuppressWarnings("DuplicatedCode")
    @Override
    public boolean handle(
        Level level,
        BlockPos hitBlockPos,
        BlockState hitBlockState,
        float fallDistance,
        AnvilFallOnLandEvent event
    ) {
        BlockState belowState = level.getBlockState(hitBlockPos.below());
        if (belowState.is(ModBlocks.HEATER) && !belowState.getValue(HeaterBlock.OVERLOAD)) {
            Map<ItemEntity, ItemStack> items =
                level.getEntitiesOfClass(ItemEntity.class, new AABB(hitBlockPos)).stream()
                    .map(it -> Map.entry(it, it.getItem()))
                    .collect(Util.toMap());

            ItemProcessInput input = new ItemProcessInput(items.values().stream().toList());

            Optional<SuperHeatingRecipe> recipeOptional = level.getRecipeManager()
                .getRecipeFor(ModRecipeTypes.SUPER_HEATING_TYPE.get(), input, level)
                .map(RecipeHolder::value);
            if (recipeOptional.isPresent()) {
                SuperHeatingRecipe recipe = recipeOptional.get();
                int times = recipe.getMaxCraftTime(input);
                Object2IntMap<Item> results = new Object2IntOpenHashMap<>();
                LootContext context;
                boolean needDoubleResult = false;
                if (level instanceof ServerLevel serverLevel) {
                    context = RecipeUtil.emptyLootContext(serverLevel);
                } else {
                    return false;
                }
                for (int i = 0; i < times; i++) {
                    for (Ingredient ingredient : recipe.getIngredients()) {
                        for (ItemStack stack : items.values()) {
                            if (stack.is(ModItemTags.RAW_ORES) || stack.is(ModItemTags.ORES)) {
                                needDoubleResult = true;
                            }
                            if (ingredient.test(stack)) {
                                stack.shrink(1);
                                break;
                            }
                        }
                    }
                    if (recipe.blockResult != Blocks.AIR) {
                        level.setBlockAndUpdate(
                            hitBlockPos, CauldronUtil.fullState(recipe.blockResult));
                    }
                    for (ChanceItemStack stack : recipe.getResults()) {
                        int amount = stack.getStack().getCount() * stack.getAmount().getInt(context);
                        results.mergeInt(stack.getStack().getItem(), amount, Integer::sum);
                    }
                }
                boolean finalNeedDoubleResult = needDoubleResult;
                AnvilUtil.dropItems(
                    results.object2IntEntrySet().stream()
                        .map(entry -> new ItemStack(entry.getKey(), entry.getIntValue()))
                        .peek(it -> {
                            if (finalNeedDoubleResult && recipe.isGenerated()) {
                                it.setCount(it.getCount() * 2);
                            }
                        })
                        .toList(),
                    level,
                    hitBlockPos.getCenter()
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
            List<ItemStack> resultStacks = new ArrayList<>();
            for (Map.Entry<ItemEntity, ItemStack> entry : items.entrySet()) {
                ItemStack inputStack = entry.getValue();
                ItemEntity itemEntity = entry.getKey();
                SingleRecipeInput cookingInput = new SingleRecipeInput(inputStack);
                Optional<RecipeHolder<BlastingRecipe>> blastingRecipe = level.getRecipeManager()
                    .getRecipeFor(
                        RecipeType.BLASTING,
                        cookingInput,
                        level
                    );
                if (blastingRecipe.isPresent()) {
                    BlastingRecipe recipe = blastingRecipe.get().value();
                    int count = recipe.result.getCount()
                        * inputStack.getCount()
                        * (inputStack.is(ModItemTags.RAW_ORES) || inputStack.is(ModItemTags.ORES) ? 2 : 1);
                    resultStacks.add(recipe.result.copyWithCount(count));
                    itemEntity.discard();
                    continue;
                }
                cookingInput = new SingleRecipeInput(inputStack);
                Optional<RecipeHolder<SmeltingRecipe>> smeltingRecipe = level.getRecipeManager()
                    .getRecipeFor(
                        RecipeType.SMELTING,
                        cookingInput,
                        level
                    );
                if (smeltingRecipe.isPresent()) {
                    SmeltingRecipe recipe = smeltingRecipe.get().value();
                    int count = recipe.result.getCount()
                        * inputStack.getCount()
                        * (inputStack.is(ModItemTags.RAW_ORES) || inputStack.is(ModItemTags.ORES) ? 2 : 1);
                    resultStacks.add(recipe.result.copyWithCount(count));
                    itemEntity.discard();
                }
            }
            if (!resultStacks.isEmpty()) {
                AnvilUtil.dropItems(resultStacks, level, hitBlockPos.getCenter());
            }
        }
        return false;
    }
}
