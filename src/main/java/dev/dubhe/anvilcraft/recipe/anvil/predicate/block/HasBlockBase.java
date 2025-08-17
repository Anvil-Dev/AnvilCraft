package dev.dubhe.anvilcraft.recipe.anvil.predicate.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.recipe.anvil.IRecipePredicate;
import dev.dubhe.anvilcraft.recipe.anvil.InWorldRecipeContext;
import dev.dubhe.anvilcraft.recipe.anvil.cache.BlockCache;
import dev.dubhe.anvilcraft.recipe.anvil.util.BlockStatePredicate;
import dev.dubhe.anvilcraft.util.RecipeUtil;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * 方块条件基类
 * <p>
 * 所有方块条件谓词的基类，提供基本的方块检测功能
 * </p>
 *
 * @param <T> 具体的子类类型
 */
@Getter
public abstract class HasBlockBase<T extends HasBlockBase<T>> implements IRecipePredicate<T> {
    /**
     * 偏移量
     */
    protected final Vec3 offset;

    /**
     * 方块状态谓词
     */
    protected final BlockStatePredicate predicate;

    /**
     * 构造一个方块条件基类
     *
     * @param offset    偏移量
     * @param predicate 方块状态谓词
     */
    public HasBlockBase(Vec3 offset, BlockStatePredicate predicate) {
        this.offset = offset;
        this.predicate = predicate;
    }

    @Override
    public boolean test(@NotNull InWorldRecipeContext context) {
        Vec3 pos = context.getPos().add(this.offset);
        BlockPos blockPos = BlockPos.containing(pos);
        BlockCache cache = context.computeIfAbsent(BlockCache.BLOCK_CACHE);
        ServerLevel level = context.getLevel();
        return predicate.test(level, cache, blockPos);
    }

    /**
     * 抽象类型类，用于定义序列化相关功能
     *
     * @param <T> 具体的子类类型
     */
    public abstract static class AbstractType<T extends HasBlockBase<T>> implements IRecipePredicate.Type<T> {
        /**
         * 编解码器
         */
        public final MapCodec<T> codec = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Vec3.CODEC.fieldOf("offset").forGetter(T::getOffset),
                BlockStatePredicate.CODEC.fieldOf("predicate").forGetter(T::getPredicate)
            ).apply(instance, this::of)
        );

        /**
         * 流编解码器
         */
        public final StreamCodec<RegistryFriendlyByteBuf, T> mapCodec = StreamCodec.composite(
            RecipeUtil.VEC3_STREAM_CODEC,
            T::getOffset,
            BlockStatePredicate.STREAM_CODEC,
            T::getPredicate,
            this::of
        );

        /**
         * 创建实例
         *
         * @param offset    偏移量
         * @param predicate 方块状态谓词
         * @return 实例
         */
        public abstract T of(Vec3 offset, BlockStatePredicate predicate);

        @Override
        public @NotNull MapCodec<T> codec() {
            return this.codec;
        }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, T> streamCodec() {
            return this.mapCodec;
        }
    }
}