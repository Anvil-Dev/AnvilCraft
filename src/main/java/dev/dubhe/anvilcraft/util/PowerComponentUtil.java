package dev.dubhe.anvilcraft.util;

import dev.dubhe.anvilcraft.api.power.IPowerComponent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/// 判断物品是否为「电网组件」，以及统计玩家背包里的电网组件数量。
///
/// <p>电网组件是按方块实体实现的接口 {@link IPowerComponent} 识别的，没有现成的标签，
/// 因此这里按方块实体类型反查并缓存每个方块的结果。</p>
public final class PowerComponentUtil {
    private static final Map<Block, Boolean> CACHE = new ConcurrentHashMap<>();

    private PowerComponentUtil() {
    }

    /// 物品对应的方块是否为电网组件（非方块物品一律为否）。
    public static boolean isPowerComponentItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Block block = Block.byItem(stack.getItem());
        if (block == Blocks.AIR) return false;
        return PowerComponentUtil.CACHE.computeIfAbsent(block, PowerComponentUtil::isPowerComponentBlock);
    }

    /// 统计背包内电网组件的总数量。
    public static int countPowerComponents(Inventory inventory) {
        int count = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (PowerComponentUtil.isPowerComponentItem(stack)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static boolean isPowerComponentBlock(Block block) {
        BlockState state = block.defaultBlockState();
        if (!state.hasBlockEntity()) return false;
        for (BlockEntityType<?> type : BuiltInRegistries.BLOCK_ENTITY_TYPE) {
            if (!type.isValid(state)) continue;
            try {
                return type.create(BlockPos.ZERO, state) instanceof IPowerComponent;
            } catch (RuntimeException exception) {
                return false;
            }
        }
        return false;
    }
}