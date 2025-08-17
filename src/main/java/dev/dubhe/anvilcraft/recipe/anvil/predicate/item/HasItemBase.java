package dev.dubhe.anvilcraft.recipe.anvil.predicate.item;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.recipe.anvil.IRecipePredicate;
import dev.dubhe.anvilcraft.recipe.anvil.InWorldRecipeContext;
import dev.dubhe.anvilcraft.recipe.anvil.InWorldRecipeData;
import dev.dubhe.anvilcraft.recipe.anvil.cache.ItemCache;
import dev.dubhe.anvilcraft.recipe.anvil.util.IItemStackPredicate;
import dev.dubhe.anvilcraft.util.RecipeUtil;
import lombok.Getter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * 物品条件基类
 * <p>
 * 所有物品条件谓词的基类，提供基本的物品检测功能
 * </p>
 *
 * @param <T> 具体的子类类型
 * @param <P> 物品堆栈谓词类型
 */
@Getter
public abstract class HasItemBase<T extends HasItemBase<T, P>, P extends IItemStackPredicate> implements IRecipePredicate<T> {
    /**
     * 偏移量
     */
    private final Vec3 offset;

    /**
     * 检测范围
     */
    private final Vec3 range;

    /**
     * 物品谓词
     */
    protected final P item;

    /**
     * 构造一个物品条件基类
     *
     * @param offset 偏移量
     * @param range  范围
     * @param item   物品谓词
     */
    public HasItemBase(Vec3 offset, Vec3 range, P item) {
        this.offset = offset;
        this.range = range;
        this.item = item;
    }

    @Override
    public boolean test(@NotNull InWorldRecipeContext context) {
        return this.item.testCount(this.getItem(context).getCount());
    }

    /**
     * 获取物品缓存输入
     *
     * @param context 配方上下文
     * @return 物品缓存输入
     */
    public ItemCache.ICacheInput getItem(@NotNull InWorldRecipeContext context) {
        final InWorldRecipeData<ItemCache.ICacheInput> cacheInput = InWorldRecipeData.of(
            AnvilCraft.of("item_cache_input/%s".formatted(this.hashCode())),
            (ctx, key) -> {
                ItemCache itemCache = ctx.computeIfAbsent(ItemCache.ITEM_CACHE);
                return itemCache.getInput(this.item.testIgnoreCount(), context.getPos().add(this.offset), this.range);
            }
        );
        return context.computeIfAbsent(cacheInput);
    }

    /**
     * 抽象类型类，用于定义序列化相关功能
     *
     * @param <T> 具体的子类类型
     * @param <P> 物品堆栈谓词类型
     */
    public abstract static class AbstractType<T extends HasItemBase<T, P>, P extends IItemStackPredicate>
        implements IRecipePredicate.Type<T> {
        /**
         * 编解码器
         */
        private final MapCodec<T> codec = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                Vec3.CODEC.fieldOf("offset").forGetter(T::getOffset),
                Vec3.CODEC.fieldOf("range").forGetter(T::getRange),
                this.itemCodec()
            ).apply(instance, this::create)
        );

        /**
         * 流编解码器
         */
        private final StreamCodec<RegistryFriendlyByteBuf, T> streamCodec = StreamCodec.composite(
            RecipeUtil.VEC3_STREAM_CODEC,
            T::getOffset,
            RecipeUtil.VEC3_STREAM_CODEC,
            T::getRange,
            StreamCodec.of(this::encodeItem, this::decodeItem),
            T::getItem,
            this::create
        );

        /**
         * 创建实例
         *
         * @param offset 偏移量
         * @param range  范围
         * @param item   物品谓词
         * @return 实例
         */
        protected abstract T create(Vec3 offset, Vec3 range, P item);

        /**
         * 解码物品谓词
         *
         * @param buf 缓冲区
         * @return 物品谓词
         */
        protected abstract P decodeItem(RegistryFriendlyByteBuf buf);

        /**
         * 编码物品谓词
         *
         * @param buf  缓冲区
         * @param item 物品谓词
         */
        protected abstract void encodeItem(RegistryFriendlyByteBuf buf, P item);

        /**
         * 获取物品谓词编解码器
         *
         * @return 物品谓词编解码器
         */
        protected abstract RecordCodecBuilder<T, P> itemCodec();

        @Override
        public @NotNull MapCodec<T> codec() {
            return this.codec;
        }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, T> streamCodec() {
            return this.streamCodec;
        }
    }
}