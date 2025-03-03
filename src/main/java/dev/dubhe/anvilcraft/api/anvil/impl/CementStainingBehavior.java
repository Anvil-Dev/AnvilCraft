package dev.dubhe.anvilcraft.api.anvil.impl;

import dev.dubhe.anvilcraft.api.anvil.IAnvilBehavior;
import dev.dubhe.anvilcraft.api.event.anvil.AnvilFallOnLandEvent;
import dev.dubhe.anvilcraft.block.state.Color;
import dev.dubhe.anvilcraft.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.Map;
import java.util.stream.Collectors;

public class CementStainingBehavior implements IAnvilBehavior {
    @Override
    public boolean handle(
        Level level,
        BlockPos hitBlockPos,
        BlockState hitBlockState,
        float fallDistance,
        AnvilFallOnLandEvent event) {
        Map<ItemEntity, ItemStack> items = level.getEntitiesOfClass(ItemEntity.class, new AABB(hitBlockPos)).stream()
            .map(it -> Map.entry(it, it.getItem()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        if (items.isEmpty()) return false;
        Item firstItem = items.values().stream().toList().getFirst().getItem();
        if (items.values().stream().allMatch(s -> s.getItem() == firstItem)) {
            for (ItemStack stack : items.values()) {
                Color color = Color.getColorByDyeItem(stack.getItem());
                if (color != null) {
                    level.setBlockAndUpdate(hitBlockPos, ModBlocks.CEMENT_CAULDRONS.get(color).getDefaultState());
                    stack.shrink(1);
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
            return true;
        }
        return false;
    }
}
