package dev.dubhe.anvilcraft.api.depository;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.HopperBlockEntity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ItemDepository implements Depository<ItemStack> {
    private Container container = null;

    @Override
    public boolean canInject(@Nonnull ItemStack thing, long count) {
        if (null == container) return false;
        ItemStack thingType = thing.copy();
        thingType.setCount(1);
        for (int i = 0; i < container.getContainerSize(); i++) {
            if (!container.canPlaceItem(i, thingType)) continue;
            ItemStack item = container.getItem(i).copy();
            if (item.isEmpty()) {
                count -= container.getMaxStackSize();
                continue;
            }
            if (!ItemStack.isSameItemSameTags(thingType, item)) continue;
            count -= item.getMaxStackSize() - item.getCount();
        }
        return count <= 0;
    }

    @Override
    public long inject(@Nonnull ItemStack thing, long count) {
        if (null == container) return count;
        ItemStack thingType = thing.copy();
        thingType.setCount(1);
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            if (count <= 0) {
                count = 0;
                break;
            }
            if (!container.canPlaceItem(slot, thingType)) continue;
            ItemStack item = container.getItem(slot).copy();
            if (!item.isEmpty() && !ItemStack.isSameItemSameTags(thingType, item)) continue;
            int injectCount = (int) Math.min(count, item.getMaxStackSize() - item.getCount());
            count -= injectCount;
            ItemStack stack = thingType.copy();
            stack.setCount(injectCount + item.getCount());
            container.setItem(slot, stack);
        }
        return count;
    }

    @Override
    public boolean canTake() {
        return true;
    }

    @Override
    public @Nonnull Thing<ItemStack> take(@Nonnull ItemStack thing, long count) {
        ItemStack thingType = thing.copy();
        thingType.setCount(1);
        long takeCount = 0;
        if (null == container) return new Thing<>(thingType.copy(), 0);
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack item = container.getItem(slot).copy();
            if (item.isEmpty()) continue;
            if (!ItemStack.isSameItemSameTags(thingType, item)) continue;
            int getCount = (int) Math.min(count, item.getCount());
            count += getCount;
            int hasCount = item.getCount() - getCount;
            item.setCount(hasCount);
            container.setItem(slot, item);
        }
        return new Thing<>(thingType.copy(), takeCount);
    }

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

    public static @Nullable ItemDepository getVanillaDepository(Level level, BlockPos pos) {
        ItemDepository depository = new ItemDepository();
        Container container = HopperBlockEntity.getContainerAt(level, pos);
        if (container == null) return null;
        depository.container = container;
        return depository;
    }
}
