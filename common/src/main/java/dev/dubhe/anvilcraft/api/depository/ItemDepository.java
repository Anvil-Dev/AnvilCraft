package dev.dubhe.anvilcraft.api.depository;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

public abstract class ItemDepository implements Depository<ItemStack> {
    /**
     * 从指定位置获取物品存储
     *
     * @param level     维度
     * @param pos       坐标
     * @param direction 输入方向
     * @return 物品存储
     */
    @ExpectPlatform
    public static @Nullable ItemDepository getItemDepository(Level level, BlockPos pos, Direction direction) {
        throw new AssertionError();
    }
}
