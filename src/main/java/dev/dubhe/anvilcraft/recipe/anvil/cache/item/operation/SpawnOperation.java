package dev.dubhe.anvilcraft.recipe.anvil.cache.item.operation;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * 生成操作记录类
 */
public record SpawnOperation(ItemStack stack, int count, Vec3 pos) {
}
