package dev.dubhe.anvilcraft.api.injection.tooltip;

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
public interface IInjectedTooltipProvider {
    /**
     * 获取需要渲染的tooltip<br>
     * 此方法用于{@link BlockEntity}及其它类
     *
     * @return 一个按顺序存储了所有需要渲染的tooltip的List
     * @see IInjectedTooltipProvider#anvilcraft$getTooltip(BlockState)
     */
    List<Component> anvilcraft$getTooltip();

    /**
     * 获取需要渲染的tooltip<br>
     * 此方法用于{@link Block}类
     *
     * @return 一个按顺序存储了所有需要渲染的tooltip的List
     * @see IInjectedTooltipProvider#anvilcraft$getTooltip()
     */
    List<Component> anvilcraft$getTooltip(BlockState state);
}
