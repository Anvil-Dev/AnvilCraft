package dev.dubhe.anvilcraft.api.depository;

/**
 * 存储
 *
 * @param <T> 被存储的内容
 */
public interface IDepository<T> {
    /**
     * 向存储中插入
     *
     * @param var      插入的内容
     * @param count    插入的数量
     * @param simulate 是否模拟插入
     * @return 剩余的数量
     */
    long insert(T var, long count, boolean simulate);

    /**
     * 从存储中提取
     *
     * @param var      提取的内容
     * @param count    计划提取的数量
     * @param simulate 是否模拟提取
     * @return 实际提取的数量
     */
    long extract(T var, long count, boolean simulate);

    /**
     * 从存储中提取
     *
     * @param count    计划提取的数量
     * @param simulate 是否模拟提取
     * @return 实际提取的数量
     */
    long extract(long count, boolean simulate);

    /**
     * 判断存储是否能存储指定内容
     *
     * @param var 内容
     * @return 存储是否能存储指定内容
     */
    boolean isValid(T var);
}
