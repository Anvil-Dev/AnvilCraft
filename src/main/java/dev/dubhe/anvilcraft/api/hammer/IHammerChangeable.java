package dev.dubhe.anvilcraft.api.hammer;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 可被锤子改变的
 */
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

    default boolean checkBlockState(BlockState blockState) {
        return true;
    }

    @Nullable
    Property<?> getChangeableProperty(BlockState blockState);
}
