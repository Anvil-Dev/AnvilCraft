package dev.dubhe.anvilcraft.api.hammer;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

/**
 * 可被锤子改变的
 */
@FunctionalInterface
public interface IHammerChangeable {
    /**
     * 改变状态
     *
     * @param player      玩家
     * @param blockPos    坐标
     * @param level       世界
     * @param anvilHammer 铁砧锤物品
     * @return 是否改变成功
     */
    boolean change(Player player, BlockPos blockPos, @NotNull Level level, ItemStack anvilHammer);
}
