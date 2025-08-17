package dev.dubhe.anvilcraft.recipe.anvil;

import org.jetbrains.annotations.NotNull;

/**
 * 优先级接口，用于定义具有优先级的对象
 * 实现该接口的类可以通过 getPriority 方法获取优先级值
 * 优先级值越小表示优先级越高
 */
public interface IPrioritized extends Comparable<IPrioritized> {
    /**
     * 获取优先级值，默认为1
     *
     * @return 优先级值
     */
    default int getPriority() {
        return 1;
    }

    /**
     * 比较两个优先级对象
     *
     * @param o 要比较的对象
     * @return 比较结果
     */
    default int compareTo(@NotNull IPrioritized o) {
        if (this.equals(o)) return 0;
        int compared = Integer.compare(this.getPriority(), o.getPriority());
        return compared == 0 ? 1 : -compared;
    }
}