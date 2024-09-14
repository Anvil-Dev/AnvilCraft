package dev.dubhe.anvilcraft.api.anvil.impl;

import dev.dubhe.anvilcraft.api.anvil.AnvilBehavior;
import dev.dubhe.anvilcraft.api.event.entity.AnvilFallOnLandEvent;
import dev.dubhe.anvilcraft.block.HeaterBlock;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.input.ItemProcessInput;
import dev.dubhe.anvilcraft.util.AnvilUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SuperHeatingBehavior implements AnvilBehavior {
    @SuppressWarnings("DuplicatedCode")
    @Override
    public void handle(
            Level level,
            BlockPos hitBlockPos,
            BlockState hitBlockState,
            float fallDistance,
            AnvilFallOnLandEvent event) {
        BlockState belowState = level.getBlockState(hitBlockPos.below());
        if (belowState.is(ModBlocks.HEATER) && !belowState.getValue(HeaterBlock.OVERLOAD)) {
            Map<ItemEntity, ItemStack> items =
                    level.getEntitiesOfClass(ItemEntity.class, new AABB(hitBlockPos)).stream()
                            .map(it -> Map.entry(it, it.getItem()))
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            ItemProcessInput input =
                    new ItemProcessInput(items.values().stream().toList());

            level.getRecipeManager()
                    .getRecipeFor(ModRecipeTypes.SUPER_HEATING_TYPE.get(), input, level)
                    .ifPresent(recipe -> {
                        int times = recipe.value().getMaxCraftTime(input);
                        ItemStack result = recipe.value().result.copy();
                        result.setCount(times * result.getCount());
                        for (int i = 0; i < times; i++) {
                            for (Ingredient ingredient : recipe.value().getIngredients()) {
                                for (ItemStack stack : items.values()) {
                                    if (ingredient.test(stack)) {
                                        stack.shrink(1);
                                        break;
                                    }
                                }
                            }
                            if (recipe.value().blockResult != Blocks.AIR) {
                                level.setBlockAndUpdate(
                                        hitBlockPos, recipe.value().blockResult.defaultBlockState());
                            }
                        }
                        AnvilUtil.dropItems(List.of(result), level, hitBlockPos.getCenter());
                    });
            items.forEach((k, v) -> {
                if (v.isEmpty()) {
                    k.discard();
                    return;
                }
                k.setItem(v.copy());
            });
        }
    }
}
