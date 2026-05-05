package dev.dubhe.anvilcraft.recipe.anvil.predicate.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import dev.anvilcraft.lib.v2.recipe.cache.BlockCache;
import dev.anvilcraft.lib.v2.recipe.predicate.IRecipePredicate;
import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeContext;
import dev.dubhe.anvilcraft.init.recipe.ModRecipePredicateTypes;
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
 * @param consume   消耗量
 * @param transform 转换后的流体ID
 * @param produce   产生量
 * @param chance    转换成功的概率
 */
public record HasCauldron(
    Vec3 offset,
    ResourceLocation fluid,
    int consume,
    ResourceLocation transform,
    int produce,
    float chance
) implements IRecipePredicate<HasCauldron> {
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
     * @param produce   产生量
     * @param chance    转换成功的概率
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
        return new HasCauldron(offset, EMPTY, 0, NULL, 0, 1.0f);
    }

    @Override
    @SuppressWarnings("RedundantIfStatement")
    public boolean test(InWorldRecipeContext context) {
        /*
         * 由于过去在此出现了非常多的bug，在此罗列，以供测试：
         * 1. 时移不完成宝石转化
         * 2. 无水执行不消耗水的物品膨发
         * 3. 流体不足执行配方
         * 4. 流体不满1B不执行配方
         * 4. 压榨重置炼药锅——永远无法达到满锅的真实
         * 5. 一桶原油完成多份余烬金属的合成
         * 6. 锅满了，仍可以熔融宝石，溢出浪费
         * 7. 流体可以相互替代使用
         */
        Vec3 pos = context.getPos().add(this.offset());
        BlockPos blockPos = BlockPos.containing(pos);
        BlockCache cache = context.computeIfAbsent(BlockCache.BLOCK_CACHE);
        BlockState curState = cache.getBlockState(blockPos);
        if (!curState.is(BlockTags.CAULDRONS)) return false;

        // 需要消耗液体
        if (this.consume > 0) {
            // 不是对应的流体锅 否决
            if (!curState.is(this.getFluidCauldron())) return false;
            Optional<Tuple<IntegerProperty, Integer>> optionalCur = HasCauldron.getFluidLevel(curState);
            if (optionalCur.isPresent()) {
                // 该流体锅可分层，需要更多判断
                // 流体不足 否决
                Tuple<IntegerProperty, Integer> fluidLevel = optionalCur.get();
                int currentLevel = fluidLevel.getB();
                IntegerProperty maxLevel = fluidLevel.getA();
                int currentMb = HasCauldron.layer2Mb(maxLevel, currentLevel);
                if (currentMb < this.consume) return false;
                // 如果要产生流体，而之前的流体不被消耗完 否决（需要完全消耗源流体以便替换为目标流体）
                if (HasCauldron.isNotEmpty(this.transform()) && currentMb != this.consume) return false;
                // 因为产生了不同的流体，因此不用进行剩余容量是否存在的判断
            }
            // 不消耗流体
        } else {
            // 有液体要求且不是对应的流体锅 否决
            if (HasCauldron.isNotEmpty(this.fluid()) && !curState.is(this.getFluidCauldron())) return false;

            if (HasCauldron.isNotEmpty(this.transform())) {
                // 异种液体 否决
                Block targetCauldron = this.getTransformCauldron();
                if (!curState.is(Blocks.CAULDRON) && !curState.is(targetCauldron)) return false;
                // 没有剩余容量 否决
                if (curState.is(targetCauldron)) {
                    BlockState targetState = targetCauldron.defaultBlockState();
                    Optional<Tuple<IntegerProperty, Integer>> optionalTarget = HasCauldron.getFluidLevel(targetState);
                    int max = optionalTarget.map(tuple -> tuple.getA().max).orElse(0);
                    Optional<Tuple<IntegerProperty, Integer>> optionalCur = HasCauldron.getFluidLevel(curState);
                    int cur = optionalCur.map(Tuple::getB).orElse(0);
                    // 当存在 produce 时，要求当前已有量 + produce 不超过最大层数
                    if (this.produce > 0) {
                        int targetMaxMb = optionalTarget.map(tuple -> HasCauldron.layer2Mb(tuple.getA(), tuple.getA().max)).orElse(1000);
                        int curMb = optionalCur.map(tuple -> HasCauldron.layer2Mb(tuple.getA(), tuple.getB())).orElse(0);
                        if (curMb + this.produce > targetMaxMb) return false;
                    } else {
                        if (cur >= max) return false;
                    }
                }
            }
        }
        return true;
    }

    @Override
    public void accept(InWorldRecipeContext context) {
        if (context.getLevel().random.nextFloat() > this.chance) return;
        if (this.fluid.equals(EMPTY) && !HasCauldron.isNotEmpty(this.transform())) return;
        BlockPos blockPos = BlockPos.containing(context.getPos().add(this.offset()));
        BlockCache cache = context.computeIfAbsent(BlockCache.BLOCK_CACHE);
        BlockState curState = cache.getBlockState(blockPos);
        Block emptyCauldron = Blocks.CAULDRON;
        Block fluidCauldron = this.getFluidCauldron();
        Block transformCauldron = this.getTransformCauldron();
        // 计算源流体和目标流体的当前毫升数（mB）
        Optional<Tuple<IntegerProperty, Integer>> optionalCurSource = HasCauldron.getFluidLevel(curState);
        int sourceMb = 0;
        if (curState.is(fluidCauldron) && !curState.is(emptyCauldron)) {
            sourceMb = HasCauldron.layer2Mb(
                optionalCurSource.map(Tuple::getA).orElse(IntegerProperty.create("level", 0, 1)),
                optionalCurSource.map(Tuple::getB).orElse(1)
            );
        }
        int targetCurMb = 0;
        Optional<Tuple<IntegerProperty, Integer>> optionalCurTarget;
        if (curState.is(transformCauldron) && !curState.is(emptyCauldron)) {
            optionalCurTarget = HasCauldron.getFluidLevel(curState);
            targetCurMb = HasCauldron.layer2Mb(
                optionalCurTarget.map(Tuple::getA).orElse(IntegerProperty.create("level", 0, 1)),
                optionalCurTarget.map(Tuple::getB).orElse(1)
            );
        }

        int remainingSourceMb = Math.max(0, sourceMb - Math.max(0, this.consume));
        int producedMb = Math.max(0, this.produce);

        // 决定最终状态：优先显示 transform（目标流体）如果有产生量或已有目标量
        BlockState resultState = emptyCauldron.defaultBlockState();
        if (HasCauldron.isNotEmpty(this.transform())) {
            // 计算目标锅默认的层级信息
            BlockState defaultTarget = transformCauldron.defaultBlockState();
            Optional<Tuple<IntegerProperty, Integer>> optionalTarget = HasCauldron.getFluidLevel(defaultTarget);
            int targetMaxMb = optionalTarget.map(tuple -> HasCauldron.layer2Mb(tuple.getA(), tuple.getA().max)).orElse(1000);
            int finalTargetMb = Math.min(targetMaxMb, targetCurMb + producedMb);

            if (finalTargetMb > 0) {
                resultState = transformCauldron.defaultBlockState();
                if (optionalTarget.isPresent()) {
                    IntegerProperty prop = optionalTarget.get().getA();
                    int layer = HasCauldron.mb2Layer(prop, Math.clamp(finalTargetMb, 1, HasCauldron.layer2Mb(prop, prop.max)));
                    resultState = resultState.setValue(prop, layer);
                }
            } else if (remainingSourceMb > 0) {
                // 没有目标流体，但还有剩余源流体，则保留源流体
                BlockState defaultSource = fluidCauldron.defaultBlockState();
                Optional<Tuple<IntegerProperty, Integer>> optSourceProp = HasCauldron.getFluidLevel(defaultSource);
                resultState = fluidCauldron.defaultBlockState();
                if (optSourceProp.isPresent()) {
                    IntegerProperty prop = optSourceProp.get().getA();
                    int layer = HasCauldron.mb2Layer(prop, Math.clamp(remainingSourceMb, 1, HasCauldron.layer2Mb(prop, prop.max)));
                    resultState = resultState.setValue(prop, layer);
                }
            }
        } else {
            // 没有 transform，结果保持为源流体的减少/增加后的状态
            int finalSourceMb = Math.max(0, sourceMb - Math.max(0, this.consume) + Math.max(0, this.produce));
            if (finalSourceMb > 0) {
                BlockState defaultSource = fluidCauldron.defaultBlockState();
                Optional<Tuple<IntegerProperty, Integer>> optSourceProp = HasCauldron.getFluidLevel(defaultSource);
                resultState = fluidCauldron.defaultBlockState();
                if (optSourceProp.isPresent()) {
                    IntegerProperty prop = optSourceProp.get().getA();
                    int layer = HasCauldron.mb2Layer(prop, Math.clamp(finalSourceMb, 1, HasCauldron.layer2Mb(prop, prop.max)));
                    resultState = resultState.setValue(prop, layer);
                }
            }
        }

        cache.setBlock(blockPos, resultState);
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
                Vec3.CODEC
                    .fieldOf("offset")
                    .forGetter(HasCauldron::offset),
                ResourceLocation.CODEC
                    .optionalFieldOf("fluid", EMPTY)
                    .forGetter(HasCauldron::fluid),
                Codec.INT
                    .optionalFieldOf("consume", 0)
                    .forGetter(HasCauldron::consume),
                ResourceLocation.CODEC
                    .optionalFieldOf("transform", NULL)
                    .forGetter(HasCauldron::transform),
                Codec.INT
                    .optionalFieldOf("produce", 0)
                    .forGetter(HasCauldron::produce),
                Codec.FLOAT
                    .optionalFieldOf("chance", 1.0f)
                    .forGetter(HasCauldron::chance)
            ).apply(instance, HasCauldron::new)
        );

        /**
         * 流编解码器
         */
        public final StreamCodec<RegistryFriendlyByteBuf, HasCauldron> mapCodec = StreamCodec.composite(
            StreamCodecUtil.VEC3,
            HasCauldron::offset,
            ResourceLocation.STREAM_CODEC,
            HasCauldron::fluid,
            ByteBufCodecs.INT,
            HasCauldron::consume,
            ResourceLocation.STREAM_CODEC,
            HasCauldron::transform,
            ByteBufCodecs.INT,
            HasCauldron::produce,
            ByteBufCodecs.FLOAT,
            HasCauldron::chance,
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
        private int produce = 0;
        private float chance = 1;

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
            this.produce = produce;
            return this;
        }

        /**
         * 设置转换成功的概率
         *
         * @param chance 概率
         * @return 构建器实例
         */
        public Builder chance(float chance) {
            this.chance = chance;
            return this;
        }

        /**
         * 构建HasCauldron实例
         *
         * @return HasCauldron实例
         */
        public HasCauldron build() {
            return new HasCauldron(this.offset, this.fluid, this.consume, this.transform, this.produce, this.chance);
        }
    }
}