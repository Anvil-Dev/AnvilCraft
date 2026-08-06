package dev.dubhe.anvilcraft.recipe.component;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.util.MathUtil;
import dev.dubhe.anvilcraft.recipe.anvil.predicate.block.HasCauldron;
import dev.dubhe.anvilcraft.util.FluidStackPredicate;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * 简单的炼药锅条件
 *
 * <p>该类用于定义配方中对炼药锅的要求，包括流体谓词、消耗量和转换后的流体栈</p>
 *
 * @param fluid     流体谓词
 * @param consume   消耗量
 * @param transforms 转换后的流体栈列表，每个栈的数量为产生量
 * @param chance    转换成功的概率
 * @param ignited   是否需要点燃
 */
public record HasCauldronSimple(
    FluidStackPredicate fluid,
    int consume,
    List<FluidStack> transforms,
    float chance,
    boolean ignited
) {
    private static final Codec<List<FluidStack>> TRANSFORMS_CODEC = Codec
        .either(FluidStack.CODEC, FluidStack.CODEC.listOf())
        .xmap(
            either -> either.map(List::of, Function.identity()),
            transforms -> transforms.size() == 1 ? Either.left(transforms.getFirst()) : Either.right(transforms)
        );
    private static final FluidStackPredicate EMPTY_PREDICATE = FluidStackPredicate.builder().amount(0).build();

    public HasCauldronSimple {
        transforms = transforms.stream().filter(fluidStack -> !fluidStack.isEmpty()).toList();
    }

    /**
     * HasCauldronSimple的编解码器
     */
    public static final MapCodec<HasCauldronSimple> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        FluidStackPredicate.CODEC.optionalFieldOf("fluid", EMPTY_PREDICATE).forGetter(HasCauldronSimple::fluid),
        Codec.INT.optionalFieldOf("consume", 0).forGetter(HasCauldronSimple::consume),
        TRANSFORMS_CODEC.optionalFieldOf("transform", List.of()).forGetter(HasCauldronSimple::transforms),
        Codec.FLOAT.optionalFieldOf("chance", 1.0f).forGetter(HasCauldronSimple::chance),
        Codec.BOOL.optionalFieldOf("ignited", false).forGetter(HasCauldronSimple::ignited)
    ).apply(instance, HasCauldronSimple::new));

    /**
     * HasCauldronSimple的网络流编解码器
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, HasCauldronSimple> STREAM_CODEC = StreamCodec.composite(
        FluidStackPredicate.STREAM_CODEC,
        HasCauldronSimple::fluid,
        ByteBufCodecs.INT,
        HasCauldronSimple::consume,
        FluidStack.STREAM_CODEC.apply(ByteBufCodecs.list()),
        HasCauldronSimple::transforms,
        ByteBufCodecs.FLOAT,
        HasCauldronSimple::chance,
        ByteBufCodecs.BOOL,
        HasCauldronSimple::ignited,
        HasCauldronSimple::new
    );

    /**
     * 将此条件转换为HasCauldron谓词
     *
     * @param offset 偏移量
     * @return HasCauldron谓词
     */
    public HasCauldron toHasCauldron(Vec3 offset) {
        return new HasCauldron(offset, this.fluid, this.consume, this.transforms, this.chance, this.ignited);
    }

    public boolean hasFluid() {
        return this.fluid.fluids().isPresent();
    }

    @SuppressWarnings("unused")
    public boolean requiresEmptyCauldron() {
        return this.fluid.equals(EMPTY_PREDICATE);
    }

    public int produce() {
        return this.transforms.stream().mapToInt(FluidStack::getAmount).sum();
    }

    /**
     * 创建一个空的构建器
     *
     * @return 构建器实例
     */
    public static Builder empty() {
        return Builder.empty();
    }

    public static Builder fluid(Fluid fluid) {
        return Builder.of(fluid);
    }

    public static Builder fluid(Holder<Fluid> fluid) {
        return Builder.of(fluid);
    }

    public static Builder fluid(FluidStackPredicate fluid) {
        return Builder.of(fluid);
    }

    public static Builder fluid(TagKey<Fluid> fluid) {
        return Builder.of(fluid);
    }

    /**
     * 构建器类，用于构建HasCauldronSimple实例
     */
    public static class Builder {
        private FluidStackPredicate fluid = EMPTY_PREDICATE;
        private int consume = 0;
        private final List<FluidStack> transforms = new ArrayList<>();
        private float chance = 1.0f;
        private boolean ignited = false;

        /**
         * 创建一个空的构建器
         *
         * @return 构建器实例
         */
        public static Builder empty() {
            return new Builder();
        }

        public static Builder of(Fluid fluid) {
            return new Builder().fluid(fluid);
        }

        public static Builder of(Holder<Fluid> fluid) {
            return new Builder().fluid(fluid);
        }

        public static Builder of(FluidStackPredicate fluid) {
            return new Builder().fluid(fluid);
        }

        public static Builder of(TagKey<Fluid> fluid) {
            return new Builder().fluid(fluid);
        }

        public Builder fluid(FluidStackPredicate fluid) {
            this.fluid = fluid;
            return this;
        }

        public Builder fluid(Fluid fluid) {
            return this.fluid(FluidStackPredicate.builder().fluid(fluid).build());
        }

        public Builder fluid(Holder<Fluid> fluid) {
            return this.fluid(FluidStackPredicate.builder().fluid(fluid).build());
        }

        public Builder fluid(TagKey<Fluid> fluid) {
            return this.fluid(FluidStackPredicate.builder().fluid(fluid).build());
        }

        public Builder transform(Fluid transform, int produce) {
            return this.transform(new FluidStack(transform, produce));
        }

        public Builder transform(Holder<Fluid> transform, int produce) {
            return this.transform(new FluidStack(transform, produce));
        }

        public Builder transform(FluidStack transform) {
            this.transforms.add(transform);
            if (this.requiresEmptyCauldron()) this.fluid = FluidStackPredicate.ANY;
            return this;
        }

        public Builder transforms(List<FluidStack> transforms) {
            transforms.forEach(this::transform);
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
         * 构建HasCauldronSimple实例
         *
         * @return HasCauldronSimple实例
         */
        public HasCauldronSimple build() {
            return new HasCauldronSimple(
                this.fluid,
                this.consume,
                this.transforms,
                this.chance,
                this.ignited
            );
        }

        private boolean requiresEmptyCauldron() {
            return this.fluid.equals(EMPTY_PREDICATE);
        }
    }
}
