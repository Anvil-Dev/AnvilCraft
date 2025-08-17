package dev.dubhe.anvilcraft.recipe.anvil.predicate.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.ModRecipePredicateTypes;
import dev.dubhe.anvilcraft.recipe.anvil.IRecipePredicate;
import dev.dubhe.anvilcraft.recipe.anvil.InWorldRecipeContext;
import dev.dubhe.anvilcraft.recipe.anvil.cache.BlockCache;
import dev.dubhe.anvilcraft.recipe.anvil.util.BlockStatePredicate;
import dev.dubhe.anvilcraft.recipe.anvil.util.WrapUtils;
import dev.dubhe.anvilcraft.util.CauldronUtil;
import dev.dubhe.anvilcraft.util.RecipeUtil;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * 炼药锅条件谓词
 * <p>
 * 用于检查指定位置是否存在特定炼药锅的谓词条件，并在配方完成后处理炼药锅中的流体
 * </p>
 */
@Getter
public class HasCauldron extends HasBlockBase<HasCauldron> {
    /**
     * 空炼药锅标识
     */
    public static final ResourceLocation EMPTY = ResourceLocation.withDefaultNamespace("empty");

    /**
     * 空转换标识
     */
    public static final ResourceLocation NULL = ResourceLocation.withDefaultNamespace("null");

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
     * 构造一个炼药锅条件谓词
     *
     * @param offset    偏移量
     * @param fluid     流体ID
     * @param consume   消耗量
     * @param transform 转换后的流体ID
     */
    public HasCauldron(Vec3 offset, ResourceLocation fluid, int consume, ResourceLocation transform) {
        super(offset, HasCauldron.ofFluid(fluid, consume));
        this.fluid = fluid;
        this.consume = consume;
        this.transform = transform;
    }

    /**
     * 创建一个空的炼药锅条件谓词
     *
     * @param offset 偏移量
     * @return HasCauldron实例
     */
    public static @NotNull HasCauldron empty(Vec3 offset) {
        return new HasCauldron(offset, EMPTY, 0, NULL);
    }

    /**
     * 根据流体和消耗量创建方块状态谓词
     *
     * @param fluid   流体ID
     * @param consume 消耗量
     * @return 方块状态谓词
     */
    public static BlockStatePredicate ofFluid(@NotNull ResourceLocation fluid, int consume) {
        if (fluid.equals(EMPTY)) {
            return BlockStatePredicate.builder()
                .of(Blocks.CAULDRON)
                .build();
        }
        Block block = HasCauldron.getDefaultCauldron(fluid);
        BlockState state = block.defaultBlockState();
        IntegerProperty property = CauldronUtil.LEVEL_4;
        Optional<Integer> optionalValue = state.getOptionalValue(CauldronUtil.LEVEL_4);
        if (optionalValue.isEmpty()) {
            property = CauldronUtil.LEVEL_3;
        }
        if (consume > 0) {
            return BlockStatePredicate.builder()
                .of(block)
                .withMin(property, consume)
                .build();
        }
        return BlockStatePredicate.builder()
            .of(block, Blocks.CAULDRON)
            .build();
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
    public void accept(@NotNull InWorldRecipeContext context) {
        if (this.fluid.equals(EMPTY)) return;
        BlockPos blockPos = BlockPos.containing(context.getPos().add(this.offset));
        BlockCache cache = context.computeIfAbsent(BlockCache.BLOCK_CACHE);
        BlockState state = cache.getBlockState(blockPos);
        if (state.is(Blocks.CAULDRON)) {
            Block block = this.getFluidCauldron();
            state = block.defaultBlockState();
        }
        IntegerProperty property = CauldronUtil.LEVEL_4;
        Optional<Integer> fluidLevel = state.getOptionalValue(property);
        if (fluidLevel.isEmpty()) {
            property = CauldronUtil.LEVEL_3;
            fluidLevel = state.getOptionalValue(property);
        }
        if (fluidLevel.isPresent()) {
            fluidLevel = Optional.of(Math.clamp(fluidLevel.orElse(0) - this.consume, 0, property.max));
            if (fluidLevel.orElse(0) == 0) {
                state = Blocks.CAULDRON.defaultBlockState();
            } else {
                state = state.setValue(property, fluidLevel.orElse(0));
            }
        }
        if (
            fluidLevel.orElse(0) > 0
                && this.transform != null
                && !this.transform.equals(this.fluid)
                && !this.transform.equals(HasCauldron.NULL)
        ) {
            Block block = this.getTransformCauldron();
            state = block.defaultBlockState();
            property = CauldronUtil.LEVEL_4;
            Optional<Integer> transformLevel = state.getOptionalValue(property);
            if (transformLevel.isEmpty()) property = CauldronUtil.LEVEL_3;
            transformLevel = Optional.of(Math.clamp(fluidLevel.orElse(0), 1, property.max));
            state = state.setValue(property, transformLevel.orElse(1));
        }
        cache.setBlock(blockPos, state);
        context.putAcceptor(BlockCache.BLOCK_CACHE.location(), BlockCache.DEFAULT_ACCEPTOR);
    }

    /**
     * 根据流体ID获取默认的炼药锅方块
     *
     * @param fluid 流体ID
     * @return 炼药锅方块
     */
    public static Block getDefaultCauldron(@NotNull ResourceLocation fluid) {
        if (fluid.equals(HasCauldron.EMPTY) || fluid.equals(HasCauldron.NULL)) return Blocks.CAULDRON;
        String namespace = fluid.getNamespace();
        String path = fluid.getPath();
        ResourceLocation cauldron = ResourceLocation.fromNamespaceAndPath(namespace, "%s_cauldron".formatted(path));
        Holder.Reference<Block> reference = BuiltInRegistries.BLOCK.getHolder(cauldron).orElse(null);
        Block block = Blocks.WATER_CAULDRON;
        if (reference != null) block = reference.value();
        return block;
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
                Vec3.CODEC.fieldOf("offset").forGetter(HasCauldron::getOffset),
                ResourceLocation.CODEC.optionalFieldOf("fluid", EMPTY).forGetter(HasCauldron::getFluid),
                Codec.INT.optionalFieldOf("consume", 0).forGetter(HasCauldron::getConsume),
                ResourceLocation.CODEC.optionalFieldOf("transform", NULL).forGetter(HasCauldron::getTransform)
            ).apply(instance, HasCauldron::new)
        );

        /**
         * 流编解码器
         */
        public final StreamCodec<RegistryFriendlyByteBuf, HasCauldron> mapCodec = StreamCodec.composite(
            RecipeUtil.VEC3_STREAM_CODEC,
            HasCauldron::getOffset,
            ResourceLocation.STREAM_CODEC,
            HasCauldron::getFluid,
            ByteBufCodecs.INT,
            HasCauldron::getConsume,
            ResourceLocation.STREAM_CODEC,
            HasCauldron::getTransform,
            HasCauldron::new
        );

        @Override
        public @NotNull MapCodec<HasCauldron> codec() {
            return this.codec;
        }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, HasCauldron> streamCodec() {
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
         * 设置消耗1单位流体
         *
         * @return 构建器实例
         */
        public Builder consume() {
            this.consume = 1;
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
         * 设置产生1单位流体
         *
         * @return 构建器实例
         */
        public Builder produce() {
            this.consume = -1;
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