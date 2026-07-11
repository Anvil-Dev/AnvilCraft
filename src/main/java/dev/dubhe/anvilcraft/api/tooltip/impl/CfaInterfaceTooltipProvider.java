package dev.dubhe.anvilcraft.api.tooltip.impl;

import dev.dubhe.anvilcraft.api.tooltip.providers.ITooltipProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;

/**
 * 锻星砧接口铁砧锤提示的类型安全基类。
 */
abstract class CfaInterfaceTooltipProvider<T extends BlockEntity>
    extends ITooltipProvider.BlockEntityTooltipProvider {
    private final Class<T> blockEntityType;

    protected CfaInterfaceTooltipProvider(Class<T> blockEntityType) {
        this.blockEntityType = blockEntityType;
    }

    @Override
    public final boolean accepts(BlockEntity value) {
        return this.blockEntityType.isInstance(value);
    }

    @Override
    public final List<Component> tooltip(BlockEntity value) {
        return this.blockEntityType.isInstance(value)
            ? this.buildTooltip(this.blockEntityType.cast(value))
            : List.of();
    }

    protected abstract List<Component> buildTooltip(T value);

    @Override
    public final int priority() {
        return -1;
    }
}
