package dev.dubhe.anvilcraft.api.item;

import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 可用磁盘复制的方块
 */
public interface IDiskCloneable {

    void storeDiskData(CompoundTag tag);

    void applyDiskData(CompoundTag data);

    /**
     * 使用磁盘物品与方块进行交互
     *
     * @param level 当前游戏世界
     * @param player 执行交互的玩家
     * @param hand 玩家使用的手（主手或副手）
     * @param itemStack 玩家手中的物品堆
     * @param hitResult 方块点击结果信息
     * @return 交互结果，PASS表示不处理，SUCCESS表示处理成功
     */
    default InteractionResult useDisk(
        Level level,
        Player player,
        InteractionHand hand,
        ItemStack itemStack,
        BlockHitResult hitResult
    ) {
        // 检查玩家是否具有建造权限
        if (!player.getAbilities().mayBuild) return InteractionResult.PASS;

        // 检查物品是否为磁盘物品，如果是则执行使用逻辑
        if (itemStack.is(ModItems.DISK.get())) {
            return itemStack.useOn(new UseOnContext(level, player, hand, itemStack, hitResult));
        }

        return InteractionResult.PASS;
    }
}
