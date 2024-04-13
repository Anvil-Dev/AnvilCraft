package dev.dubhe.anvilcraft.api.depository;

/**
 * {@link IDepository} 中单个存储资源的视图
 *
 * @param <T> 被存储的内容
 */
@SuppressWarnings("unused")
public interface IDepositoryView<T> {
    /**
     * 从视图中提取
     *
     * @param var      提取的内容
     * @param count    计划提取的数量
     * @param simulate 是否模拟提取
     * @return 实际提取的数量
     */
    long extract(T var, long count, boolean simulate);

    /**
     * @return 存储在此视图中的资源
     */
    T getResource();

    /**
     * @return 存储在此视图中的资源量
     */
    long getAmount();

    /**
     * @return 此视图可以接受的资源总量
     */
    long getCapacity();
}
