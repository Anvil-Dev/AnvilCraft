package dev.dubhe.anvilcraft.api.anvil.impl;

import dev.dubhe.anvilcraft.api.anvil.AnvilBehavior;
import dev.dubhe.anvilcraft.api.event.anvil.AnvilFallOnLandEvent;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.ChanceItemStack;
import dev.dubhe.anvilcraft.recipe.anvil.BulgingRecipe;
import dev.dubhe.anvilcraft.util.AnvilUtil;

import dev.dubhe.anvilcraft.util.RecipeUtil;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.phys.AABB;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class BulgingBehavior implements AnvilBehavior {
    @SuppressWarnings("DuplicatedCode")
    @Override
    public boolean handle(
        Level level,
        BlockPos hitBlockPos,
        BlockState hitBlockState,
        float fallDistance,
        AnvilFallOnLandEvent event
    ) {
        Map<ItemEntity, ItemStack> items = level.getEntitiesOfClass(ItemEntity.class, new AABB(hitBlockPos)).stream()
            .map(it -> Map.entry(it, it.getItem()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        BulgingRecipe.Input input =
            new BulgingRecipe.Input(items.values().stream().toList(), hitBlockState);
        Optional<RecipeHolder<BulgingRecipe>> recipeOptional =
            level.getRecipeManager().getRecipeFor(ModRecipeTypes.BULGING_TYPE.get(), input, level);
        if (recipeOptional.isPresent()) {
            RecipeHolder<BulgingRecipe> recipe = recipeOptional.get();
            int times = recipe.value().getMaxCraftTime(input);
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
                            stack.shrink(1);
                            break;
                        }
                    }
                }
                for (ChanceItemStack stack : recipe.value().getResults()) {
                    int amount = stack.getStack().getCount() * stack.getAmount().getInt(context);
                    results.mergeInt(stack.getStack().getItem(), amount, Integer::sum);
                }
            }
            AnvilUtil.dropItems(
                results.object2IntEntrySet().stream()
                    .map(entry -> new ItemStack(entry.getKey(), entry.getIntValue()))
                    .toList(),
                level,
                hitBlockPos.getCenter()
            );
            if (recipe.value().isFromWater()) {
                level.setBlockAndUpdate(hitBlockPos, recipe.value().cauldron.defaultBlockState());
            } else {
                if (recipe.value().isConsumeFluid()) {
                    if (hitBlockState.hasProperty(LayeredCauldronBlock.LEVEL)) {
                        int cauldronLevel = hitBlockState.getValue(LayeredCauldronBlock.LEVEL);
                        cauldronLevel--;
                        if (cauldronLevel <= 0) {
                            level.setBlockAndUpdate(hitBlockPos, Blocks.CAULDRON.defaultBlockState());
                        } else {
                            level.setBlockAndUpdate(
                                hitBlockPos,
                                hitBlockState.setValue(LayeredCauldronBlock.LEVEL, cauldronLevel)
                            );
                        }
                    } else {
                        level.setBlockAndUpdate(hitBlockPos, Blocks.CAULDRON.defaultBlockState());
                    }
                }
                if (recipe.value().isProduceFluid()) {
                    if (hitBlockState.hasProperty(LayeredCauldronBlock.LEVEL)) {
                        int cauldronLevel = hitBlockState.getValue(LayeredCauldronBlock.LEVEL);
                        cauldronLevel++;
                        level.setBlockAndUpdate(
                            hitBlockPos,
                            hitBlockState.setValue(LayeredCauldronBlock.LEVEL, cauldronLevel)
                        );
                    } else {
                        level.setBlockAndUpdate(
                            hitBlockPos,
                            recipe.value().getCauldron().defaultBlockState()
                        );
                    }
                }
            }
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
}
