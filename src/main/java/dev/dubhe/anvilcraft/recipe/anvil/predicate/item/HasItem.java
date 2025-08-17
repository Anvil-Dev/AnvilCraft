package dev.dubhe.anvilcraft.recipe.anvil.predicate.item;

import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.ModRecipePredicateTypes;
import dev.dubhe.anvilcraft.recipe.anvil.IRecipePredicate;
import dev.dubhe.anvilcraft.recipe.anvil.util.ItemPredicate;
import lombok.Getter;
import net.minecraft.advancements.critereon.ItemSubPredicate;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.core.component.DataComponentPredicate;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * 物品条件谓词
 * <p>
 * 用于检查指定区域内是否存在特定物品的谓词条件
 * </p>
 */
@Getter
public class HasItem extends HasItemBase<HasItem, ItemPredicate> {
    /**
     * 构造一个物品条件谓词
     *
     * @param offset 偏移量
     * @param range  范围
     * @param item   物品谓词
     */
    public HasItem(Vec3 offset, Vec3 range, ItemPredicate item) {
        super(offset, range, item);
    }

    /**
     * 创建一个构建器
     *
     * @return 构建器实例
     */
    public static @NotNull Builder builder() {
        return new Builder();
    }

    @Override
    public IRecipePredicate.Type<HasItem> getType() {
        return ModRecipePredicateTypes.HAS_ITEM.get();
    }

    /**
     * HasItem的类型
     */
    public static class Type extends AbstractType<HasItem, ItemPredicate> {
        @Override
        protected HasItem create(Vec3 offset, Vec3 range, ItemPredicate item) {
            return new HasItem(offset, range, item);
        }

        @Override
        protected ItemPredicate decodeItem(@NotNull RegistryFriendlyByteBuf buf) {
            return ItemPredicate.STREAM_CODEC.decode(buf);
        }

        @Override
        protected void encodeItem(@NotNull RegistryFriendlyByteBuf buf, ItemPredicate item) {
            ItemPredicate.STREAM_CODEC.encode(buf, item);
        }

        @Override
        protected RecordCodecBuilder<HasItem, ItemPredicate> itemCodec() {
            return ItemPredicate.CODEC.fieldOf("item").forGetter(HasItem::getItem);
        }
    }

    /**
     * 构建器类，用于构建HasItem实例
     */
    public static class Builder {
        private Vec3 offset = Vec3.ZERO;
        private Vec3 range = new Vec3(1.0, 1.0, 1.0);
        private final ItemPredicate.Builder item = ItemPredicate.Builder.item();

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
            return this.offset(new Vec3(x, y, z));
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
         * 设置检测范围
         *
         * @param range 范围
         * @return 构建器实例
         */
        public Builder range(Vec3 range) {
            this.range = range;
            return this;
        }

        /**
         * 设置检测范围
         *
         * @param x X轴范围
         * @param y Y轴范围
         * @param z Z轴范围
         * @return 构建器实例
         */
        public Builder range(double x, double y, double z) {
            this.range = new Vec3(x, y, z);
            return this;
        }

        /**
         * 设置检测范围（各轴相同）
         *
         * @param range 范围
         * @return 构建器实例
         */
        public Builder range(double range) {
            this.range = new Vec3(range, range, range);
            return this;
        }

        /**
         * 设置物品
         *
         * @param items 物品数组
         * @return 构建器实例
         */
        public Builder of(ItemLike... items) {
            this.item.of(items);
            return this;
        }

        /**
         * 设置物品标签
         *
         * @param tag 物品标签
         * @return 构建器实例
         */
        public Builder of(TagKey<Item> tag) {
            this.item.of(tag);
            return this;
        }

        /**
         * 设置数量范围
         *
         * @param count 数量范围
         * @return 构建器实例
         */
        public Builder count(MinMaxBounds.Ints count) {
            this.item.withCount(count);
            return this;
        }

        /**
         * 设置最小数量
         *
         * @param min 最小数量
         * @return 构建器实例
         */
        public Builder moreThan(int min) {
            this.item.withCount(MinMaxBounds.Ints.atLeast(min));
            return this;
        }

        /**
         * 设置数量范围
         *
         * @param min 最小数量
         * @param max 最大数量
         * @return 构建器实例
         */
        public Builder between(int min, int max) {
            this.item.withCount(MinMaxBounds.Ints.between(min, max));
            return this;
        }

        /**
         * 设置最大数量
         *
         * @param min 最大数量
         * @return 构建器实例
         */
        public Builder lessThan(int min) {
            this.item.withCount(MinMaxBounds.Ints.atMost(min));
            return this;
        }

        /**
         * 添加子谓词
         *
         * @param type      子谓词类型
         * @param predicate 子谓词
         * @param <T>       子谓词类型
         * @return 构建器实例
         */
        public <T extends ItemSubPredicate> Builder with(ItemSubPredicate.Type<T> type, T predicate) {
            this.item.withSubPredicate(type, predicate);
            return this;
        }

        /**
         * 设置数据组件谓词
         *
         * @param components 数据组件谓词
         * @return 构建器实例
         */
        public Builder has(DataComponentPredicate components) {
            this.item.hasComponents(components);
            return this;
        }

        /**
         * 构建HasItem实例
         *
         * @return HasItem实例
         */
        public HasItem build() {
            return new HasItem(offset, range, item.build());
        }
    }
}