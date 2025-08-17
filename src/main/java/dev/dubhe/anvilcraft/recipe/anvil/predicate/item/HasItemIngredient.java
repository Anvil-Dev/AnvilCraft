package dev.dubhe.anvilcraft.recipe.anvil.predicate.item;

import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.ModRecipePredicateTypes;
import dev.dubhe.anvilcraft.recipe.anvil.IRecipePredicate;
import dev.dubhe.anvilcraft.recipe.anvil.InWorldRecipeContext;
import dev.dubhe.anvilcraft.recipe.anvil.cache.ItemCache;
import dev.dubhe.anvilcraft.recipe.anvil.util.ItemIngredientPredicate;
import lombok.Getter;
import net.minecraft.advancements.critereon.ItemSubPredicate;
import net.minecraft.core.component.DataComponentPredicate;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * 物品原料条件谓词
 * <p>
 * 用于检查指定区域内是否存在特定物品原料的谓词条件，并在配方完成后消耗这些物品
 * </p>
 */
@Getter
public class HasItemIngredient extends HasItemBase<HasItemIngredient, ItemIngredientPredicate> {
    /**
     * 构造一个物品原料条件谓词
     *
     * @param offset 偏移量
     * @param range  范围
     * @param item   物品原料谓词
     */
    public HasItemIngredient(Vec3 offset, Vec3 range, ItemIngredientPredicate item) {
        super(offset, range, item);
    }

    @Override
    public IRecipePredicate.Type<HasItemIngredient> getType() {
        return ModRecipePredicateTypes.HAS_ITEM_INGREDIENT.get();
    }

    @Override
    public void snapshot(@NotNull InWorldRecipeContext context) {
        ItemCache.ICacheInput input = this.getItem(context);
        input.shrink(this.item.count());
    }

    @Override
    public void rollback(@NotNull InWorldRecipeContext context) {
        ItemCache.ICacheInput input = this.getItem(context);
        input.rollbackShrink();
    }

    @Override
    public void accept(@NotNull InWorldRecipeContext context) {
        context.putAcceptor(ItemCache.ITEM_CACHE.location(), ItemCache.DEFAULT_ACCEPTOR);
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
     * HasItemIngredient的类型
     */
    public static class Type extends AbstractType<HasItemIngredient, ItemIngredientPredicate> {
        @Override
        protected HasItemIngredient create(Vec3 offset, Vec3 range, ItemIngredientPredicate item) {
            return new HasItemIngredient(offset, range, item);
        }

        @Override
        protected ItemIngredientPredicate decodeItem(@NotNull RegistryFriendlyByteBuf buf) {
            return ItemIngredientPredicate.STREAM_CODEC.decode(buf);
        }

        @Override
        protected void encodeItem(@NotNull RegistryFriendlyByteBuf buf, ItemIngredientPredicate item) {
            ItemIngredientPredicate.STREAM_CODEC.encode(buf, item);
        }

        @Override
        protected RecordCodecBuilder<HasItemIngredient, ItemIngredientPredicate> itemCodec() {
            return ItemIngredientPredicate.CODEC.fieldOf("item").forGetter(HasItemIngredient::getItem);
        }
    }

    /**
     * 构建器类，用于构建HasItemIngredient实例
     */
    public static class Builder {
        private Vec3 offset = Vec3.ZERO;
        private Vec3 range = new Vec3(1.0, 1.0, 1.0);
        private final ItemIngredientPredicate.Builder item = ItemIngredientPredicate.Builder.item();

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
         * 设置数量
         *
         * @param count 数量
         * @return 构建器实例
         */
        public Builder count(int count) {
            this.item.withCount(count);
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
         * 构建HasItemIngredient实例
         *
         * @return HasItemIngredient实例
         */
        public HasItemIngredient build() {
            return new HasItemIngredient(offset, range, item.build());
        }
    }
}