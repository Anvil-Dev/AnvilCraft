package dev.dubhe.anvilcraft.api.laser;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import javax.annotation.Nullable;

public interface ILaserComponentOwner {
    int getLaserMaxLength();

    void setLaserMaxLength(int value);

    List<ILaserComponent> allComponents();

    @Nullable
    <T extends ILaserComponent> T getComponent(ILaserComponentType<T, ?> type);

    /** 实例优先；已有组件时直接返回；两项创建参数均为空且组件不存在时返回 null。 */
    @Nullable
    <T extends ILaserComponent, E> T setOrCreateComponent(
        ILaserComponentType<T, E> type, @Nullable T instance, @Nullable E creationEnvironment
    );

    BlockPos getLaserSourcePos();

    BlockPos getLaserOrigin();

    Direction getLaserDirection();

    int getLaserTicks();

    void deliverLaserDrops(List<ItemStack> drops, BlockPos sourceBlockPos);
}
