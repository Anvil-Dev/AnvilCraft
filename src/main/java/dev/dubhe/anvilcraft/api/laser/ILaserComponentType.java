package dev.dubhe.anvilcraft.api.laser;

import java.util.List;

public interface ILaserComponentType<T extends ILaserComponent, E> {
    T createInstance(E componentEnvironment);

    /** 合并非空输入，结果不得共享输入组件的可变运行状态。 */
    T mergeIncoming(List<T> instances);

    @SuppressWarnings("unchecked")
    default T mergeIncoming(T... instances) {
        return mergeIncoming(List.of(instances));
    }

    /** 数值越大越先执行；同优先级按组件加入顺序执行。 */
    int priority();
}
