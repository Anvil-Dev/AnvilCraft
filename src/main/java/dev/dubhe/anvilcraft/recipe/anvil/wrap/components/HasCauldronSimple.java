package dev.dubhe.anvilcraft.recipe.anvil.wrap.components;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.recipe.anvil.predicate.block.HasCauldron;
import lombok.Getter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * 简单的炼药锅条件
 * <p>
 * 该类用于定义配方中对炼药锅的要求，包括所需流体、消耗量和转换后的流体
 * </p>
 */
@Getter
public class HasCauldronSimple {
    /**
     * 流体ID
     */
    private final ResourceLocation fluid;

    /**
     * 消耗量（负数表示产生）
     */
    private final int consume;

    /**
     * 转换后的流体ID
     */
    private final ResourceLocation transform;

    /**
     * 构造一个简单的炼药锅条件
     *
     * @param fluid     流体ID
     * @param consume   消耗量
     * @param transform 转换后的流体ID
     */
    public HasCauldronSimple(ResourceLocation fluid, int consume, ResourceLocation transform) {
        this.fluid = fluid;
        this.consume = consume;
        this.transform = transform;
    }

    /**
     * HasCauldronSimple的编解码器
     */
    public static final MapCodec<HasCauldronSimple> CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(
            ResourceLocation.CODEC
                .optionalFieldOf("fluid", HasCauldron.EMPTY)
                .forGetter(HasCauldronSimple::getFluid),
            Codec.INT
                .optionalFieldOf("consume", 0)
                .forGetter(HasCauldronSimple::getConsume),
            ResourceLocation.CODEC
                .optionalFieldOf("transform", HasCauldron.NULL)
                .forGetter(HasCauldronSimple::getTransform)
        ).apply(instance, HasCauldronSimple::new)
    );

    /**
     * 将此条件转换为HasCauldron谓词
     *
     * @param offset 偏移量
     * @return HasCauldron谓词
     */
    public HasCauldron toHasCauldron(Vec3 offset) {
        return new HasCauldron(offset, fluid, consume, transform);
    }

    /**
     * 获取流体对应的炼药锅方块
     *
     * @return 炼药锅方块
     */
    public Block getFluidCauldron() {
        return HasCauldron.getDefaultCauldron(this.fluid);
    }

    /**
     * 获取转换后的炼药锅方块
     *
     * @return 炼药锅方块
     */
    public Block getTransformCauldron() {
        return HasCauldron.getDefaultCauldron(this.transform);
    }

    /**
     * HasCauldronSimple的网络流编解码器
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, HasCauldronSimple> STREAM_CODEC = StreamCodec.composite(
        ResourceLocation.STREAM_CODEC,
        HasCauldronSimple::getFluid,
        ByteBufCodecs.INT,
        HasCauldronSimple::getConsume,
        ResourceLocation.STREAM_CODEC,
        HasCauldronSimple::getTransform,
        HasCauldronSimple::new
    );

    /**
     * 创建一个空的构建器
     *
     * @return 构建器实例
     */
    public static @NotNull Builder empty() {
        return Builder.empty();
    }

    /**
     * 创建一个指定流体的构建器
     *
     * @param fluid 流体ID
     * @return 构建器实例
     */
    public static @NotNull Builder fluid(ResourceLocation fluid) {
        return Builder.of(fluid);
    }

    /**
     * 构建器类，用于构建HasCauldronSimple实例
     */
    public static class Builder {
        private ResourceLocation fluid = HasCauldron.EMPTY;
        private int consume = 0;
        private ResourceLocation transform = HasCauldron.NULL;

        /**
         * 创建一个空的构建器
         *
         * @return 构建器实例
         */
        public static @NotNull Builder empty() {
            return new Builder();
        }

        /**
         * 创建一个指定流体的构建器
         *
         * @param fluid 流体ID
         * @return 构建器实例
         */
        public static @NotNull Builder of(ResourceLocation fluid) {
            Builder builder = new Builder();
            builder.fluid = fluid;
            return builder;
        }

        /**
         * 设置流体ID
         *
         * @param fluid 流体ID
         * @return 构建器实例
         */
        public @NotNull Builder fluid(ResourceLocation fluid) {
            this.fluid = fluid;
            return this;
        }

        /**
         * 设置转换后的流体ID
         *
         * @param transform 转换后的流体ID
         * @return 构建器实例
         */
        public @NotNull Builder transform(ResourceLocation transform) {
            this.transform = transform;
            return this;
        }

        /**
         * 设置消耗量
         *
         * @param consume 消耗量
         * @return 构建器实例
         */
        public Builder consume(int consume) {
            this.consume = consume;
            return this;
        }

        /**
         * 构建HasCauldronSimple实例
         *
         * @return HasCauldronSimple实例
         */
        public HasCauldronSimple build() {
            return new HasCauldronSimple(fluid, consume, transform);
        }
    }
}