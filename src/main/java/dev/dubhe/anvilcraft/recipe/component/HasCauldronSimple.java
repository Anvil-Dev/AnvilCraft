package dev.dubhe.anvilcraft.recipe.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.util.MathUtil;
import dev.dubhe.anvilcraft.recipe.anvil.predicate.block.HasCauldron;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * 简单的炼药锅条件
 *
 * <p>该类用于定义配方中对炼药锅的要求，包括所需流体、消耗量和转换后的流体</p>
 *
 * @param fluid     流体ID
 * @param consume   消耗量
 * @param transform 转换后的流体ID
 * @param produce   产生量
 * @param chance    转换成功的概率
 * @param ignited   是否需要点燃
 * @param fluidTag  流体标签ID，当非null时使用标签匹配而非精确匹配
 */
public record HasCauldronSimple(
    ResourceLocation fluid,
    int consume,
    ResourceLocation transform,
    int produce,
    float chance,
    boolean ignited,
    @Nullable ResourceLocation fluidTag
) {
    /**
     * 构造一个简单的炼药锅条件
     *
     * @param fluid     流体ID
     * @param consume   消耗量
     * @param transform 转换后的流体ID
     * @param produce   产生量
     * @param chance    转换成功的概率
     * @param ignited   是否需要点燃
     */
    public HasCauldronSimple {
    }

    /**
     * HasCauldronSimple的编解码器
     */
    public static final MapCodec<HasCauldronSimple> CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(
            ResourceLocation.CODEC
                .optionalFieldOf("fluid", HasCauldron.EMPTY)
                .forGetter(HasCauldronSimple::fluid),
            Codec.INT
                .optionalFieldOf("consume", 0)
                .forGetter(HasCauldronSimple::consume),
            ResourceLocation.CODEC
                .optionalFieldOf("transform", HasCauldron.NULL)
                .forGetter(HasCauldronSimple::transform),
            Codec.INT
                .optionalFieldOf("produce", 0)
                .forGetter(HasCauldronSimple::produce),
            Codec.FLOAT
                .optionalFieldOf("chance", 1.0f)
                .forGetter(HasCauldronSimple::chance),
            Codec.BOOL
                .optionalFieldOf("ignited", false)
                .forGetter(HasCauldronSimple::ignited),
            ResourceLocation.CODEC
                .optionalFieldOf("fluidTag")
                .forGetter(h -> Optional.ofNullable(h.fluidTag()))
        ).apply(instance, (fluid, consume, transform, produce, chance, ignited, fluidTag) ->
            new HasCauldronSimple(fluid, consume, transform, produce, chance, ignited, fluidTag.orElse(null)))
    );

    /**
     * 将此条件转换为HasCauldron谓词
     *
     * @param offset 偏移量
     * @return HasCauldron谓词
     */
    public HasCauldron toHasCauldron(Vec3 offset) {
        return new HasCauldron(offset, this.fluid, this.consume, this.transform, this.produce, this.chance, this.ignited, this.fluidTag);
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
    public static final StreamCodec<RegistryFriendlyByteBuf, HasCauldronSimple> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public HasCauldronSimple decode(RegistryFriendlyByteBuf buf) {
            ResourceLocation fluid = ResourceLocation.STREAM_CODEC.decode(buf);
            int consume = ByteBufCodecs.INT.decode(buf);
            ResourceLocation transform = ResourceLocation.STREAM_CODEC.decode(buf);
            int produce = ByteBufCodecs.INT.decode(buf);
            float chance = ByteBufCodecs.FLOAT.decode(buf);
            boolean ignited = ByteBufCodecs.BOOL.decode(buf);
            ResourceLocation fluidTag = ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC).decode(buf).orElse(null);
            return new HasCauldronSimple(fluid, consume, transform, produce, chance, ignited, fluidTag);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, HasCauldronSimple h) {
            ResourceLocation.STREAM_CODEC.encode(buf, h.fluid());
            ByteBufCodecs.INT.encode(buf, h.consume());
            ResourceLocation.STREAM_CODEC.encode(buf, h.transform());
            ByteBufCodecs.INT.encode(buf, h.produce());
            ByteBufCodecs.FLOAT.encode(buf, h.chance());
            ByteBufCodecs.BOOL.encode(buf, h.ignited());
            ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC).encode(buf, Optional.ofNullable(h.fluidTag()));
        }
    };

    /**
     * 创建一个空的构建器
     *
     * @return 构建器实例
     */
    public static Builder empty() {
        return Builder.empty();
    }

    /**
     * 创建一个指定流体的构建器
     *
     * @param fluid 流体ID
     * @return 构建器实例
     */
    public static Builder fluid(ResourceLocation fluid) {
        return Builder.of(fluid);
    }

    /**
     * 构建器类，用于构建HasCauldronSimple实例
     */
    public static class Builder {
        private ResourceLocation fluid = HasCauldron.EMPTY;
        private int consume = 0;
        private ResourceLocation transform = HasCauldron.NULL;
        private int produce = 0;
        private float chance = 1f;
        private boolean ignited = false;
        private ResourceLocation fluidTag = null;

        /**
         * 创建一个空的构建器
         *
         * @return 构建器实例
         */
        public static Builder empty() {
            return new Builder();
        }

        /**
         * 创建一个指定流体的构建器
         *
         * @param fluid 流体ID
         * @return 构建器实例
         */
        public static Builder of(ResourceLocation fluid) {
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
        public Builder fluid(ResourceLocation fluid) {
            this.fluid = fluid;
            return this;
        }

        /**
         * 设置转换后的流体ID
         *
         * @param transform 转换后的流体ID
         * @return 构建器实例
         */
        public Builder transform(ResourceLocation transform) {
            this.transform = transform;
            if (!HasCauldron.isNotEmpty(this.fluid)) this.fluid = HasCauldron.NULL;
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
         * 设置产生量
         *
         * @param produce 产生量
         * @return 构建器实例
         */
        public Builder produce(int produce) {
            this.produce = produce;
            return this;
        }

        /**
         * 设置转换成功的概率
         *
         * @param chance 转换成功的概率
         * @return 构建器实例
         */
        public Builder chance(float chance) {
            this.chance = MathUtil.clampWithProportion(chance, 0, 1);
            return this;
        }

        /**
         * 设置需要点燃锅
         *
         * @return 构建器实例
         */
        public Builder ignite() {
            this.ignited = true;
            return this;
        }

        /**
         * 设置流体标签ID，用于标签匹配
         *
         * @param fluidTag 流体标签ID
         * @return 构建器实例
         */
        public Builder fluidTag(ResourceLocation fluidTag) {
            this.fluidTag = fluidTag;
            if (!HasCauldron.isNotEmpty(this.fluid)) this.fluid = HasCauldron.NULL;
            return this;
        }

        /**
         * 构建HasCauldronSimple实例
         *
         * @return HasCauldronSimple实例
         */
        public HasCauldronSimple build() {
            return new HasCauldronSimple(this.fluid, this.consume, this.transform, this.produce, this.chance, this.ignited, this.fluidTag);
        }
    }
}