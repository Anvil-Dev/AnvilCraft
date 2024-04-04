package dev.dubhe.anvilcraft.api.depository;

import lombok.Getter;

import javax.annotation.Nonnull;

@SuppressWarnings("unused")
public interface Depository<T> {
    /**
     * 该存储能否存入
     *
     * @param thing 存入的事物
     * @param count 存入的数量
     * @return 该存储能否存入
     */
    boolean canInject(@Nonnull T thing, long count);

    /**
     * 向存储中存入
     *
     * @param thing    存入的事物
     * @param count    存入的数量
     * @param simulate 是否模拟存入
     * @return 无法存入的数量
     */
    long inject(@Nonnull T thing, long count, boolean simulate);

    /**
     * 该存储能否取出
     *
     * @return 该存储能否取出
     */
    boolean canTake();

    /**
     * 从存储中取出
     *
     * @param thing 取出的事物
     * @param count 取出的数量
     * @return 取出的结果
     */
    @Nonnull
    Thing<T> take(@Nonnull T thing, long count);

    @Getter
    @SuppressWarnings("ClassCanBeRecord")
    class Thing<I> {
        private final I thing;
        private final long count;

        public Thing(I thing, long count) {
            this.thing = thing;
            this.count = count;
        }
    }
}
