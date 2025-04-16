package dev.dubhe.anvilcraft.api.injection.tooltip;

import dev.dubhe.anvilcraft.api.injection.UnimplementedException;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * 头戴铁砧锤时需要显示tooltip，用于接口注入至原版内容
 */
@MethodsReturnNonnullByDefault
public interface IInjectedTooltipProducer {
    /**
     * 获取需要渲染的tooltip<br>
     * 此方法用于{@link BlockEntity}及其它类
     *
     * @return 一个按顺序存储了所有需要渲染的tooltip的List
     * @see IInjectedTooltipProducer#anvilcraft$getTooltip(BlockState)
     */
    default List<Component> anvilcraft$getTooltip() {
        if (this instanceof BlockEntity) throw new UnimplementedException(
            "This method \"IInjectedTooltipProducer.anvilcraft$getTooltip()\" is not implemented");
        return List.of();
    }

    /**
     * 获取需要渲染的tooltip<br>
     * 此方法用于{@link Block}类
     *
     * @return 一个按顺序存储了所有需要渲染的tooltip的List
     * @see IInjectedTooltipProducer#anvilcraft$getTooltip()
     */
    default List<Component> anvilcraft$getTooltip(BlockState state) {
        if (this instanceof Block) throw new UnimplementedException(
            "This method \"IInjectedTooltipProducer.anvilcraft$getTooltip(BlockState)\" is not implemented in " + this.getClass().getName());
        return List.of();
    }
}
