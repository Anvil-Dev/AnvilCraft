package dev.dubhe.anvilcraft.api.anvil.impl;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.anvil.AnvilBehavior;
import dev.dubhe.anvilcraft.api.event.entity.AnvilFallOnLandEvent;
import dev.dubhe.anvilcraft.block.CorruptedBeaconBlock;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.TimeWarpRecipe;
import dev.dubhe.anvilcraft.util.AnvilUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TimeWarpBehavior implements AnvilBehavior {
    @SuppressWarnings("DuplicatedCode")
    @Override
    public void handle(
            Level level,
            BlockPos hitBlockPos,
            BlockState hitBlockState,
            float fallDistance,
            AnvilFallOnLandEvent event) {
        AnvilCraft.LOGGER.info("TimeWarpHandle");
        BlockState belowState = level.getBlockState(hitBlockPos.below());
        if (belowState.is(ModBlocks.CORRUPTED_BEACON)
                && belowState.getValue(CorruptedBeaconBlock.LIT)) {
            Map<ItemEntity, ItemStack> items =
                    level.getEntitiesOfClass(ItemEntity.class, new AABB(hitBlockPos)).stream()
                            .map(it -> Map.entry(it, it.getItem()))
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            TimeWarpRecipe.Input input =
                    new TimeWarpRecipe.Input(items.values().stream().toList(), hitBlockState);
            level
                    .getRecipeManager()
                    .getRecipeFor(ModRecipeTypes.TIME_WARP_TYPE.get(), input, level)
                    .ifPresent(recipe -> {
                        ItemStack result = recipe.value().getResult().copy();
                        int times = recipe.value().getMaxCraftTime(input);
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
                        }
                        AnvilUtil.dropItems(List.of(result), level, hitBlockPos.getCenter());
                        if (recipe.value().isConsumeFluid()) {
                            if (hitBlockState.hasProperty(LayeredCauldronBlock.LEVEL)) {
                                int cauldronLevel = hitBlockState.getValue(LayeredCauldronBlock.LEVEL);
                                cauldronLevel--;
                                if (cauldronLevel <= 0) {
                                    level.setBlockAndUpdate(hitBlockPos, Blocks.CAULDRON.defaultBlockState());
                                } else {
                                    level.setBlockAndUpdate(
                                            hitBlockPos,
                                            hitBlockState.setValue(LayeredCauldronBlock.LEVEL, cauldronLevel));
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
                                        hitBlockPos, hitBlockState.setValue(LayeredCauldronBlock.LEVEL, cauldronLevel));
                            } else {
                                level.setBlockAndUpdate(
                                        hitBlockPos, recipe.value().getCauldron().defaultBlockState());
                            }
                        }

                        items.forEach((k, v) -> {
                            if (v.isEmpty()) {
                                k.discard();
                                return;
                            }
                            k.setItem(v.copy());
                        });
                    });
        }
    }
}
