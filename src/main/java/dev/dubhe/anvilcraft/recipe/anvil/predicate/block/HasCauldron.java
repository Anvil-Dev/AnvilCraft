package dev.dubhe.anvilcraft.recipe.anvil.predicate.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.recipe.cache.BlockCache;
import dev.anvilcraft.lib.recipe.predicate.IRecipePredicate;
import dev.anvilcraft.lib.recipe.util.InWorldRecipeContext;
import dev.anvilcraft.lib.util.CodecUtil;
import dev.dubhe.anvilcraft.init.reicpe.ModRecipePredicateTypes;
import dev.dubhe.anvilcraft.recipe.anvil.util.WrapUtils;
import dev.dubhe.anvilcraft.util.CauldronUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * 炼药锅条件谓词
 *
 * <p>用于检查指定位置是否存在特定炼药锅的谓词条件，并在配方完成后处理炼药锅中的流体</p>
 *
 * @param fluid     流体ID
 * @param consume   消耗量（负数表示产生）
 * @param transform 转换后的流体ID
 */
public record HasCauldron(Vec3 offset, ResourceLocation fluid, int consume, ResourceLocation transform)
    implements IRecipePredicate<HasCauldron> {
    /**
     * 空炼药锅标识
     */
    public static final ResourceLocation EMPTY = ResourceLocation.withDefaultNamespace("empty");

    /**
     * 空转换标识
     */
    public static final ResourceLocation NULL = ResourceLocation.withDefaultNamespace("null");

    /**
     * 构造一个炼药锅条件谓词
     *
     * @param offset    偏移量
     * @param fluid     流体ID
     * @param consume   消耗量
     * @param transform 转换后的流体ID
     */
    public HasCauldron {
    }

    /**
     * 创建一个空的炼药锅条件谓词
     *
     * @param offset 偏移量
     * @return HasCauldron实例
     */
    public static HasCauldron empty(Vec3 offset) {
        return new HasCauldron(offset, EMPTY, 0, NULL);
    }

    @Override
    public boolean test(InWorldRecipeContext context) {
        Vec3 pos = context.getPos().add(this.offset());
        BlockPos blockPos = BlockPos.containing(pos);
        BlockCache cache = context.computeIfAbsent(BlockCache.BLOCK_CACHE);
        BlockState curState = cache.getBlockState(blockPos);
        if (!curState.is(BlockTags.CAULDRONS)) return false;
        if (this.consume > 0) {
            Optional<Tuple<IntegerProperty, Integer>> optionalCur = HasCauldron.getFluidLevel(curState);
            if (optionalCur.isPresent()) {
                Tuple<IntegerProperty, Integer> fluidLevel = optionalCur.get();
                int currentLevel = fluidLevel.getB();
                int maxLevel = fluidLevel.getA().max;
                return currentLevel >= maxLevel;
            }
        }
        Block fluidCauldron = this.getFluidCauldron();
        if (curState.is(fluidCauldron)) return true;
        if (HasCauldron.isNotEmpty(this.fluid())) return false;
        if (!HasCauldron.isNotEmpty(this.transform())) return false;
        Block targetCauldron = this.getTransformCauldron();
        if (!curState.is(Blocks.CAULDRON) && !curState.is(targetCauldron)) return false;
        BlockState targetState = targetCauldron.defaultBlockState();
        Optional<Tuple<IntegerProperty, Integer>> optionalTarget = HasCauldron.getFluidLevel(targetState);
        int max = optionalTarget.map(tuple -> tuple.getA().max).orElse(0);
        Optional<Tuple<IntegerProperty, Integer>> optionalCur = HasCauldron.getFluidLevel(curState);
        int cur = optionalCur.map(Tuple::getB).orElse(0);
        return cur < max;
    }

    @Override
    public void accept(InWorldRecipeContext context) {
        if (this.fluid.equals(EMPTY) && !HasCauldron.isNotEmpty(this.transform())) return;
        BlockPos blockPos = BlockPos.containing(context.getPos().add(this.offset()));
        BlockCache cache = context.computeIfAbsent(BlockCache.BLOCK_CACHE);
        BlockState curState = cache.getBlockState(blockPos);
        Block emptyCauldron = Blocks.CAULDRON;
        Block fluidCauldron = this.getFluidCauldron();
        Block transformCauldron = this.getTransformCauldron();
        Block targetCauldron = HasCauldron.isNotEmpty(this.transform()) ? transformCauldron : fluidCauldron;
        BlockState targetState = targetCauldron.defaultBlockState();
        Optional<Tuple<IntegerProperty, Integer>> optionalCur = HasCauldron.getFluidLevel(curState);
        Optional<Tuple<IntegerProperty, Integer>> optionalTarget = HasCauldron.getFluidLevel(targetState);
        int cur;
        if (!curState.is(emptyCauldron) && (curState.is(fluidCauldron) || curState.is(transformCauldron))) {
            cur = HasCauldron.layer2Mb(
                optionalCur.map(Tuple::getA).orElse(IntegerProperty.create("level", 0, 1)),
                optionalCur.map(Tuple::getB).orElse(1)
            );
        } else {
            cur = 0;
        }
        int target = cur - this.consume();
        if (target <= 0) {
            targetState = emptyCauldron.defaultBlockState();
        } else {
            IntegerProperty property = optionalTarget.map(Tuple::getA).orElse(IntegerProperty.create("level", 0, 1));
            Integer max = optionalTarget.map(tuple -> tuple.getA().max).orElse(1);
            target = HasCauldron.mb2Layer(property, Math.clamp(target, 1, HasCauldron.layer2Mb(property, max)));
            if (optionalTarget.isPresent()) {
                targetState = targetState.setValue(property, target);
            }
        }
        cache.setBlock(blockPos, targetState);
        context.putAcceptor(BlockCache.BLOCK_CACHE.location(), BlockCache.DEFAULT_ACCEPTOR);
    }

    /**
     * 创建一个构建器
     *
     * @return 构建器实例
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 根据流体ID获取默认的炼药锅方块
     *
     * @param fluid 流体ID
     * @return 炼药锅方块
     */
    public static Block getDefaultCauldron(ResourceLocation fluid) {
        if (fluid.equals(HasCauldron.EMPTY) || fluid.equals(HasCauldron.NULL)) return Blocks.CAULDRON;
        String namespace = fluid.getNamespace();
        String path = fluid.getPath();
        ResourceLocation cauldron = ResourceLocation.fromNamespaceAndPath(namespace, "%s_cauldron".formatted(path));
        Holder.Reference<Block> reference = BuiltInRegistries.BLOCK.getHolder(cauldron).orElse(null);
        Block block = Blocks.WATER_CAULDRON;
        if (reference != null) block = reference.value();
        return block;
    }

    public static boolean isNotEmpty(ResourceLocation fluid) {
        return !fluid.equals(HasCauldron.NULL) && !fluid.equals(HasCauldron.EMPTY);
    }

    public static Optional<Tuple<IntegerProperty, Integer>> getFluidLevel(BlockState state) {
        IntegerProperty property = CauldronUtil.LEVEL_4;
        Optional<Integer> value = state.getOptionalValue(property);
        if (value.isEmpty()) {
            property = CauldronUtil.LEVEL_3;
            value = state.getOptionalValue(property);
        }
        return value.isPresent() ? Optional.of(new Tuple<>(property, value.get())) : Optional.empty();
    }

    public static int mb2Layer(IntegerProperty property, int mb) {
        int max = property.max;
        double mbPreLayer = 1000.0 / max;
        return (int) Math.round(mb / mbPreLayer);
    }

    public static int layer2Mb(IntegerProperty property, int layer) {
        int max = property.max;
        double mbPreLayer = 1000.0 / max;
        return (int) Math.round(layer * mbPreLayer);
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

    @Override
    public Type getType() {
        return ModRecipePredicateTypes.HAS_CAULDRON.get();
    }

    /**
     * HasCauldron的类型
     */
    public static class Type implements IRecipePredicate.Type<HasCauldron> {
        /**
         * 编解码器
         */
        public final MapCodec<HasCauldron> codec = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Vec3.CODEC.fieldOf("offset").forGetter(HasCauldron::offset),
                ResourceLocation.CODEC.optionalFieldOf("fluid", EMPTY).forGetter(HasCauldron::fluid),
                Codec.INT.optionalFieldOf("consume", 0).forGetter(HasCauldron::consume),
                ResourceLocation.CODEC.optionalFieldOf("transform", NULL).forGetter(HasCauldron::transform)
            ).apply(instance, HasCauldron::new)
        );

        /**
         * 流编解码器
         */
        public final StreamCodec<RegistryFriendlyByteBuf, HasCauldron> mapCodec = StreamCodec.composite(
            CodecUtil.VEC3_STREAM_CODEC,
            HasCauldron::offset,
            ResourceLocation.STREAM_CODEC,
            HasCauldron::fluid,
            ByteBufCodecs.INT,
            HasCauldron::consume,
            ResourceLocation.STREAM_CODEC,
            HasCauldron::transform,
            HasCauldron::new
        );

        @Override
        public MapCodec<HasCauldron> codec() {
            return this.codec;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, HasCauldron> streamCodec() {
            return this.mapCodec;
        }
    }

    /**
     * 构建器类，用于构建HasCauldron实例
     */
    public static class Builder {
        private Vec3 offset = Vec3.ZERO;
        private ResourceLocation fluid = HasCauldron.EMPTY;
        private int consume = 0;
        private ResourceLocation transform = HasCauldron.NULL;

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
         * 设置为空炼药锅
         *
         * @return 构建器实例
         */
        public Builder empty() {
            this.fluid = HasCauldron.EMPTY;
            return this;
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
         * 设置炼药锅方块
         *
         * @param cauldron 炼药锅方块
         * @return 构建器实例
         */
        public Builder cauldron(Block cauldron) {
            this.fluid = WrapUtils.cauldron2Fluid(cauldron);
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
            return this;
        }

        /**
         * 设置消耗指定单位流体
         *
         * @param consume 消耗量
         * @return 构建器实例
         */
        public Builder consume(int consume) {
            this.consume = consume;
            return this;
        }

        /**
         * 设置产生指定单位流体
         *
         * @param produce 产生量
         * @return 构建器实例
         */
        public Builder produce(int produce) {
            this.consume = -produce;
            return this;
        }

        /**
         * 构建HasCauldron实例
         *
         * @return HasCauldron实例
         */
        public HasCauldron build() {
            return new HasCauldron(this.offset, this.fluid, this.consume, this.transform);
        }
    }
}