package dev.dubhe.anvilcraft.recipe.anvil.predicate.block;

import dev.dubhe.anvilcraft.init.ModRecipePredicateTypes;
import dev.dubhe.anvilcraft.recipe.anvil.util.BlockStatePredicate;
import lombok.Getter;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.function.Consumer;

/**
 * 方块条件谓词
 * <p>
 * 用于检查指定位置是否存在特定方块的谓词条件
 * </p>
 */
@Getter
public class HasBlock extends HasBlockBase<HasBlock> {
    /**
     * 构造一个方块条件谓词
     *
     * @param offset    偏移量
     * @param predicate 方块状态谓词
     */
    public HasBlock(Vec3 offset, BlockStatePredicate predicate) {
        super(offset, predicate);
    }

    @Override
    public Type getType() {
        return ModRecipePredicateTypes.HAS_BLOCK.get();
    }

    /**
     * 创建一个构建器
     *
     * @return 构建器实例
     */
    public static @NotNull Builder builder() {
        return new Builder();
    }

    /**
     * HasBlock的类型
     */
    public static class Type extends AbstractType<HasBlock> {
        @Override
        public HasBlock of(Vec3 offset, BlockStatePredicate predicate) {
            return new HasBlock(offset, predicate);
        }
    }

    /**
     * 构建器类，用于构建HasBlock实例
     */
    public static class Builder {
        private Vec3 offset = Vec3.ZERO;
        private final BlockStatePredicate.Builder predicate = BlockStatePredicate.builder();

        /**
         * 设置偏移量
         *
         * @param offset 偏移量
         * @return 构建器实例
         */
        public Builder offset(Vec3 offset) {
            this.offset = offset;
            return this;
        }

        /**
         * 设置偏移量
         *
         * @param x X坐标偏移
         * @param y Y坐标偏移
         * @param z Z坐标偏移
         * @return 构建器实例
         */
        public Builder offset(double x, double y, double z) {
            this.offset = new Vec3(x, y, z);
            return this;
        }

        /**
         * 设置向下偏移
         *
         * @param below 向下偏移量
         * @return 构建器实例
         */
        public Builder below(double below) {
            return this.offset(Vec3.ZERO.subtract(0, below, 0));
        }

        /**
         * 设置向下偏移1格
         *
         * @return 构建器实例
         */
        public Builder below() {
            return this.below(1);
        }

        /**
         * 设置向上偏移
         *
         * @param above 向上偏移量
         * @return 构建器实例
         */
        public Builder above(double above) {
            return this.offset(Vec3.ZERO.add(0, above, 0));
        }

        /**
         * 设置向上偏移1格
         *
         * @return 构建器实例
         */
        public Builder above() {
            return this.above(1);
        }

        /**
         * 设置谓词构建器
         *
         * @param consumer 谓词构建器消费者
         * @return 构建器实例
         */
        public Builder predicate(@NotNull Consumer<BlockStatePredicate.Builder> consumer) {
            consumer.accept(this.predicate);
            return this;
        }

        /**
         * 设置方块
         *
         * @param blocks 方块数组
         * @return 构建器实例
         */
        public Builder of(Block... blocks) {
            this.predicate.of(blocks);
            return this;
        }

        /**
         * 设置方块集合
         *
         * @param blocks 方块集合
         * @return 构建器实例
         */
        public Builder of(Collection<Block> blocks) {
            this.predicate.of(blocks);
            return this;
        }

        /**
         * 设置方块标签
         *
         * @param tag 方块标签
         * @return 构建器实例
         */
        public Builder of(TagKey<Block> tag) {
            this.predicate.of(tag);
            return this;
        }

        /**
         * 设置方块属性
         *
         * @param property 属性
         * @param value    属性值
         * @return 构建器实例
         */
        public Builder with(@NotNull Property<?> property, String value) {
            this.predicate.with(property, value);
            return this;
        }

        /**
         * 设置整数型方块属性
         *
         * @param property 属性
         * @param value    属性值
         * @return 构建器实例
         */
        public Builder with(Property<Integer> property, int value) {
            this.predicate.with(property, value);
            return this;
        }

        /**
         * 设置布尔型方块属性
         *
         * @param property 属性
         * @param value    属性值
         * @return 构建器实例
         */
        public Builder with(Property<Boolean> property, boolean value) {
            this.predicate.with(property, value);
            return this;
        }

        /**
         * 设置方块属性
         *
         * @param property 属性
         * @param value    属性值
         * @param <T>      属性值类型
         * @return 构建器实例
         */
        public <T extends Comparable<T>> Builder with(Property<T> property, @NotNull T value) {
            this.predicate.with(property, value);
            return this;
        }

        /**
         * 设置方块属性范围
         *
         * @param property 属性
         * @param minValue 最小值
         * @param maxValue 最大值
         * @param <T>      属性值类型
         * @return 构建器实例
         */
        public <T extends Comparable<T>> Builder with(
            @NotNull Property<T> property,
            @Nullable T minValue,
            @Nullable T maxValue
        ) {
            this.predicate.with(property, minValue, maxValue);
            return this;
        }

        /**
         * 设置方块属性最小值
         *
         * @param property 属性
         * @param minValue 最小值
         * @param <T>      属性值类型
         * @return 构建器实例
         */
        public <T extends Comparable<T>> Builder withMin(
            @NotNull Property<T> property,
            T minValue
        ) {
            this.predicate.withMin(property, minValue);
            return this;
        }

        /**
         * 设置方块属性最大值
         *
         * @param property 属性
         * @param maxValue 最大值
         * @param <T>      属性值类型
         * @return 构建器实例
         */
        public <T extends Comparable<T>> Builder withMax(
            @NotNull Property<T> property,
            T maxValue
        ) {
            this.predicate.withMax(property, maxValue);
            return this;
        }

        /**
         * 添加OR条件
         *
         * @return 构建器实例
         */
        public Builder or() {
            this.predicate.or();
            return this;
        }

        /**
         * 构建HasBlockIngredient实例
         *
         * @return HasBlockIngredient实例
         */
        public HasBlockIngredient build() {
            return new HasBlockIngredient(offset, predicate.build());
        }
    }
}