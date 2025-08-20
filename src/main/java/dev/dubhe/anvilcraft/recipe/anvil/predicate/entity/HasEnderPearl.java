package dev.dubhe.anvilcraft.recipe.anvil.predicate.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.reicpe.ModRecipePredicateTypes;
import dev.dubhe.anvilcraft.recipe.anvil.predicate.IRecipePredicate;
import dev.dubhe.anvilcraft.recipe.anvil.util.InWorldRecipeContext;
import lombok.Getter;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

/**
 * 末影珍珠条件谓词
 * <p>
 * 该类用于判断当前实体是否为末影珍珠，并满足指定的条件（如所在维度、速度、高度等）
 * 是维度旅行配方中的条件判断部分
 *
 * @see IRecipePredicate 配方谓词接口
 */
@Getter
public class HasEnderPearl implements IRecipePredicate<HasEnderPearl> {
    private final ResourceKey<Level> dimensionKey;
    private final double speed;
    private final double height;

    /**
     * 构造一个末影珍珠条件谓词
     *
     * @param dimensionKey 所在维度键
     * @param speed 最小速度
     * @param height 最小高度
     */
    public HasEnderPearl(ResourceKey<Level> dimensionKey, double speed, double height) {
        this.dimensionKey = dimensionKey;
        this.speed = speed;
        this.height = height;
    }

    /**
     * 创建一个新的末影珍珠条件构建器
     *
     * @return 构建器实例
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 获取配方谓词类型
     *
     * @return 末影珍珠条件谓词类型
     */
    @Override
    public Type getType() {
        return ModRecipePredicateTypes.HAS_ENDER_PEARL.get();
    }

    /**
     * 测试当前配方上下文是否满足末影珍珠条件
     * <p>
     * 条件包括：实体是末影珍珠、所在维度匹配、速度大于设定值、高度大于设定值
     *
     * @param context 配方上下文
     * @return 是否满足条件
     */
    @Override
    public boolean test(InWorldRecipeContext context) {
        if (!(context.getEntity() instanceof ThrownEnderpearl pearl)) return false;
        if (
            !context.getLevel()
            .dimension()
            .location()
            .equals(this.dimensionKey.location())
        ) {
            return false;
        }
        if (Math.abs(pearl.getDeltaMovement().y) < this.speed) return false;
        return pearl.position().y >= this.height;
    }

    /**
     * 末影珍珠条件谓词类型
     * <p>
     * 定义了末影珍珠条件谓词的序列化和反序列化方式
     */
    public static class Type implements IRecipePredicate.Type<HasEnderPearl> {
        public static final MapCodec<HasEnderPearl> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceKey.codec(Registries.DIMENSION)
                .fieldOf("origin")
                .forGetter(HasEnderPearl::getDimensionKey),
            Codec.DOUBLE
                .fieldOf("speed")
                .forGetter(HasEnderPearl::getSpeed),
            Codec.DOUBLE
                .fieldOf("height")
                .forGetter(HasEnderPearl::getHeight)
        ).apply(instance, HasEnderPearl::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, HasEnderPearl> STREAM_CODEC = StreamCodec.composite(
            ResourceKey.streamCodec(Registries.DIMENSION),
            HasEnderPearl::getDimensionKey,
            ByteBufCodecs.DOUBLE,
            HasEnderPearl::getSpeed,
            ByteBufCodecs.DOUBLE,
            HasEnderPearl::getHeight,
            HasEnderPearl::new
        );

        /**
         * 获取配方谓词的编解码器
         *
         * @return MapCodec编解码器
         */
        @Override
        public @NotNull MapCodec<HasEnderPearl> codec() {
            return CODEC;
        }

        /**
         * 获取配方谓词的流编解码器，用于网络传输
         *
         * @return StreamCodec流编解码器
         */
        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, HasEnderPearl> streamCodec() {
            return STREAM_CODEC;
        }
    }

    /**
     * 末影珍珠条件谓词构建器
     * <p>
     * 提供便捷的方法来构建末影珍珠条件谓词，包括设置所在维度、速度、高度等参数
     */
    @Getter
    public static class Builder {
        private ResourceKey<Level> dimensionKey;
        private double speed;
        private double height;

        /**
         * 创建一个新的构建器实例
         */
        private Builder() {
        }

        /**
         * 设置所在维度
         *
         * @param dimensionKey 所在维度键
         * @return 当前构建器实例
         */
        public Builder dimension(ResourceKey<Level> dimensionKey) {
            this.dimensionKey = dimensionKey;
            return this;
        }

        /**
         * 设置触发条件所需的最小速度
         *
         * @param speed 最小速度
         * @return 当前构建器实例
         */
        public Builder speed(double speed) {
            this.speed = speed;
            return this;
        }

        /**
         * 设置触发条件所需的最小高度
         *
         * @param height 最小高度
         * @return 当前构建器实例
         */
        public Builder height(double height) {
            this.height = height;
            return this;
        }

        /**
         * 构建末影珍珠条件谓词实例
         *
         * @return 末影珍珠条件谓词实例
         */
        public HasEnderPearl build() {
            return new HasEnderPearl(this.dimensionKey, this.speed, this.height);
        }
    }
}