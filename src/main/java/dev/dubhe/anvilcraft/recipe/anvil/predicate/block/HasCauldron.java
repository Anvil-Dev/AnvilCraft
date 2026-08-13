package dev.dubhe.anvilcraft.recipe.anvil.predicate.block;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import dev.anvilcraft.lib.v2.recipe.cache.BlockCache;
import dev.anvilcraft.lib.v2.recipe.predicate.IRecipePredicate;
import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeContext;
import dev.anvilcraft.lib.v2.util.MathUtil;
import dev.dubhe.anvilcraft.api.block.ICauldron;
import dev.dubhe.anvilcraft.api.block.IIgnitableCauldron;
import dev.dubhe.anvilcraft.api.entity.IEntityCauldron;
import dev.dubhe.anvilcraft.api.fluid.IFluidHandlerHolder;
import dev.dubhe.anvilcraft.api.fluid.LargeCauldronFluidHandler;
import dev.dubhe.anvilcraft.block.entity.LargeCauldronBlockEntity;
import dev.dubhe.anvilcraft.init.recipe.ModRecipePredicateTypes;
import dev.dubhe.anvilcraft.recipe.anvil.util.WrapUtils;
import dev.dubhe.anvilcraft.util.CauldronUtil;
import dev.dubhe.anvilcraft.util.CompatUtil;
import dev.dubhe.anvilcraft.util.FluidStackPredicate;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import javax.annotation.Nullable;

/**
 * 炼药锅条件谓词
 *
 * <p>用于检查指定位置是否存在特定炼药锅的谓词条件，并在配方完成后处理炼药锅中的流体</p>
 *
 * @param offset    偏移量
 * @param fluid     流体谓词
 * @param consume   消耗量
 * @param transforms 转换后的流体栈列表，每个栈的数量为产生量
 * @param chance    转换成功的概率
 * @param ignited   是否需要点燃
 */
public record HasCauldron(
    Vec3 offset,
    FluidStackPredicate fluid,
    int consume,
    List<FluidStack> transforms,
    float chance,
    boolean ignited
) implements IRecipePredicate<HasCauldron> {
    private static final List<EntityCauldronSelector> ENTITY_CAULDRON_SELECTORS = new CopyOnWriteArrayList<>();

    /**
     * 在最近实体锅查找之后调用。查询不得改变世界状态。
     */
    public static void registerEntityCauldronSelector(EntityCauldronSelector selector) {
        ENTITY_CAULDRON_SELECTORS.add(selector);
    }

    @FunctionalInterface
    public interface EntityCauldronSelector {
        @Nullable
        IEntityCauldron select(InWorldRecipeContext context, BlockPos pos, @Nullable IEntityCauldron current);
    }

    private static final Codec<List<FluidStack>> TRANSFORMS_CODEC = Codec
        .either(FluidStack.CODEC, FluidStack.CODEC.listOf())
        .xmap(
            either -> either.map(List::of, Function.identity()),
            transforms -> transforms.size() == 1 ? Either.left(transforms.getFirst()) : Either.right(transforms)
        );
    private static final FluidStackPredicate EMPTY_PREDICATE = FluidStackPredicate.builder().amount(0).build();

    /**
     * 构造一个炼药锅条件谓词
     *
     * @param offset    偏移量
     * @param fluid     流体谓词
     * @param consume   消耗量
     * @param transforms 转换后的流体栈列表
     * @param chance    转换成功的概率
     * @param ignited   是否需要点燃
     */
    public HasCauldron {
        transforms = transforms.stream().filter(fluidStack -> !fluidStack.isEmpty()).toList();
    }

    /**
     * 创建一个空的炼药锅条件谓词
     *
     * @param offset 偏移量
     * @return HasCauldron实例
     */
    public static HasCauldron empty(Vec3 offset) {
        return new HasCauldron(offset, EMPTY_PREDICATE, 0, List.of(), 1.0f, false);
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

        // 消耗为负 否决
        if (this.consume() < 0) return false;
        // 概率不在0-1之间 否决
        if (this.chance() < 0 || this.chance() > 1) return false;
        // 消耗量比允许的现有量大 否决
        if (this.fluid().amount().flatMap(MinMaxBounds.Ints::max).map(max -> this.consume() > max).orElse(false)) return false;

        // 不是锅 否决
        BlockPos pos = BlockPos.containing(context.getPos().add(this.offset()));
        BlockCache cache = context.computeIfAbsent(BlockCache.BLOCK_CACHE);
        IEntityCauldron entityCauldron = findEntityCauldron(context, pos);
        BlockState state = cache.getBlockState(pos);
        if (!state.is(BlockTags.CAULDRONS) && entityCauldron == null) return false;
        if (this.hasMultipleFluidOutputs() && !HasCauldron.supportsMultipleFluidOutputs(cache, pos)) return false;

        if (cache.getBlockEntity(pos) instanceof LargeCauldronBlockEntity cauldron) {
            if (
                this.consume() > LargeCauldronFluidHandler.TANK_CAPACITY
                || this.transforms().stream()
                    .anyMatch(transform -> transform.getAmount() > LargeCauldronFluidHandler.TANK_CAPACITY)
            ) {
                return false;
            }
            return cauldron.testFluidRecipe(context, this);
        }

        // 消耗/产生比锅容量大 否决
        double capacity = HasCauldron.getCapacity(cache, pos, entityCauldron);
        if (this.consume() > capacity || this.produce() > capacity) return false;

        // 锅中流体检查不通过 否决
        FluidStack curFluid = HasCauldron.getCurFluidStack(cache, pos, entityCauldron);
        if (this.hasCheck() && !this.matchesFluid(curFluid)) return false;

        // 如果锅必须为可点燃锅
        if (this.ignited) {
            if (entityCauldron != null) {
                if (!entityCauldron.anvilcraft$isIgnited()) return false;
            } else {
                if (!(cache.getBlockState(pos).getBlock() instanceof IIgnitableCauldron cauldron)) return false;
                if (!cauldron.isIgnited(cache, pos)) return false;
            }
        }

        // 不消耗且不产生 通过
        if (this.consume() == 0 && this.produce() == 0) return true;

        // 消耗量大于存量 否决
        double cur = HasCauldron.getCur(cache, pos, entityCauldron);
        if (this.consume() > cur) return false;

        // 最终总量超出容量 否决
        double afterConsume = cur - this.consume();
        if (afterConsume + this.produce() > capacity) return false;

        // 锅中有流体 且 转换有效 且 前后流体类型不同 且 锅中流体没有消耗完 否决
        if (
            cur > 0
            && !this.transforms().isEmpty()
            && !FluidStack.isSameFluidSameComponents(curFluid, this.transforms().getFirst())
            && afterConsume != 0
        ) {
            return false;
        }

        // 全部通过
        return true;
    }

    @Override
    public void accept(InWorldRecipeContext context) {
        BlockPos pos = BlockPos.containing(context.getPos().add(this.offset()));
        BlockCache cache = context.computeIfAbsent(BlockCache.BLOCK_CACHE);
        if (cache.getBlockEntity(pos) instanceof LargeCauldronBlockEntity cauldron) {
            cauldron.acceptFluidRecipe(context);
            return;
        }
        if (context.getLevel().getRandom().nextFloat() > this.chance()) return;
        if (this.consume() == 0 && this.transforms().isEmpty()) return;

        IEntityCauldron entityCauldron = findEntityCauldron(context, pos);
        FluidStack curFluid = HasCauldron.getCurFluidStack(cache, pos, entityCauldron);
        double cur = HasCauldron.getCur(cache, pos, entityCauldron);
        double afterConsume = cur - this.consume();
        double amount = afterConsume + this.produce();

        FluidStack newFluid = this.transforms().isEmpty() ? curFluid : this.transforms().getFirst();
        if (amount > 0 && !newFluid.isEmpty()) {
            HasCauldron.applyFluid(cache, pos, newFluid, amount, this.ignited, entityCauldron);
        } else {
            HasCauldron.applyEmpty(cache, pos, entityCauldron);
        }

        context.putAcceptor(BlockCache.BLOCK_CACHE.location(), BlockCache.DEFAULT_ACCEPTOR);
    }

    @Override
    public void snapshot(InWorldRecipeContext context) {
        LargeCauldronBlockEntity cauldron = this.getLargeCauldron(context);
        if (cauldron != null) cauldron.snapshotFluidRecipe(context, this);
    }

    @Override
    public void rollback(InWorldRecipeContext context) {
        LargeCauldronBlockEntity cauldron = this.getLargeCauldron(context);
        if (cauldron != null) cauldron.rollbackFluidRecipe(context);
    }

    @Override
    public void clearStack(InWorldRecipeContext context) {
        LargeCauldronBlockEntity cauldron = this.getLargeCauldron(context);
        if (cauldron != null) cauldron.clearFluidRecipeStack(context);
    }

    private @Nullable LargeCauldronBlockEntity getLargeCauldron(InWorldRecipeContext context) {
        BlockPos pos = BlockPos.containing(context.getPos().add(this.offset()));
        BlockCache cache = context.computeIfAbsent(BlockCache.BLOCK_CACHE);
        return cache.getBlockEntity(pos) instanceof LargeCauldronBlockEntity cauldron ? cauldron : null;
    }

    /**
     * 创建一个构建器
     *
     * @return 构建器实例
     */
    public static Builder builder() {
        return new Builder();
    }

    public int produce() {
        return this.transforms.stream().mapToInt(FluidStack::getAmount).sum();
    }

    public boolean hasMultipleFluidOutputs() {
        return this.transforms.size() > 1;
    }

    public boolean hasCheck() {
        return this.fluid().fluids().isPresent()
               || this.fluid().component().map(predicate -> !predicate.patch().isEmpty() || predicate.isNegate()).orElse(false)
               || this.fluid().amount().isPresent()
               || this.fluid().isNegate();
    }

    public boolean requiresEmptyCauldron() {
        return this.fluid().equals(EMPTY_PREDICATE);
    }

    /**
     * 检查当前流体是否匹配条件
     *
     * @param curFluid 当前流体栈
     * @return 是否匹配
     */
    public boolean matchesFluid(FluidStack curFluid) {
        return this.fluid().test(curFluid);
    }

    private static boolean supportsMultipleFluidOutputs(BlockCache cache, BlockPos pos) {
        if (cache.getBlockEntity(pos) instanceof ICauldron cauldron) {
            return cauldron.supportsMultipleFluidOutputs();
        }
        return cache.getBlockState(pos).getBlock() instanceof ICauldron cauldron
               && cauldron.supportsMultipleFluidOutputs();
    }

    public static double getCapacity(BlockCache cache, BlockPos pos) {
        return getCapacity(cache, pos, null);
    }

    private static double getCapacity(BlockCache cache, BlockPos pos, @Nullable IEntityCauldron entityCauldron) {
        IFluidHandler handler = getFluidHandler(cache, pos, entityCauldron);
        return handler == null ? 1000 : handler.getTankCapacity(0);
    }

    private static FluidStack getCurFluidStack(
        BlockCache cache,
        BlockPos pos,
        @Nullable IEntityCauldron entityCauldron
    ) {
        IFluidHandler handler = getFluidHandler(cache, pos, entityCauldron);
        if (handler != null) return handler.getFluidInTank(0);
        Fluid fluid = cache.getBlockState(pos).getBlock() instanceof IIgnitableCauldron cauldron
                      ? cauldron.getFluid(cache, pos)
                      : BuiltInRegistries.FLUID.get(WrapUtils.cauldron2Fluid(cache.getBlockState(pos).getBlock()));
        return new FluidStack(fluid, (int) Math.round(getCur(cache, pos)));
    }

    private static double getCur(BlockCache cache, BlockPos pos, @Nullable IEntityCauldron entityCauldron) {
        IFluidHandler handler = getFluidHandler(cache, pos, entityCauldron);
        if (handler != null) return handler.getFluidInTank(0).getAmount();
        BlockState state = cache.getBlockState(pos);
        if (state.is(Blocks.CAULDRON)) return 0.0;
        IntegerProperty property = CauldronUtil.LEVEL_4;
        Optional<Integer> value = state.getOptionalValue(property);
        if (value.isEmpty()) {
            property = CauldronUtil.LEVEL_3;
            value = state.getOptionalValue(property);
        }
        IntegerProperty finalProperty = property;
        return value.map(layer -> (double) layer / finalProperty.max * 1000.0).orElse(1000.0);
    }

    private static double getCur(BlockCache cache, BlockPos pos) {
        return getCur(cache, pos, null);
    }

    private static void applyEmpty(BlockCache cache, BlockPos pos, @Nullable IEntityCauldron entityCauldron) {
        IFluidHandler handler = getFluidHandler(cache, pos, entityCauldron);
        if (handler != null) {
            handler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE);
        } else {
            cache.setBlock(pos, Blocks.CAULDRON);
        }
    }

    private static void applyFluid(
        BlockCache cache,
        BlockPos pos,
        FluidStack fluid,
        double mb,
        boolean ignited,
        @Nullable IEntityCauldron entityCauldron
    ) {
        if (cache.getBlockState(pos).getBlock() instanceof IIgnitableCauldron cauldron) {
            if (cauldron.isIgnited(cache, pos)) ignited = true;
        }
        if (entityCauldron != null && entityCauldron.anvilcraft$isIgnited()) ignited = true;
        IFluidHandler handler = getFluidHandler(cache, pos, entityCauldron);
        if (handler != null) {
            FluidStack fluidInTank = handler.getFluidInTank(0);
            if (FluidStack.isSameFluidSameComponents(fluidInTank, fluid)) {
                int diff = (int) Math.round(mb) - fluidInTank.getAmount();
                if (diff < 0) {
                    handler.drain(-diff, IFluidHandler.FluidAction.EXECUTE);
                } else {
                    handler.fill(fluid.copyWithAmount(diff), IFluidHandler.FluidAction.EXECUTE);
                }
            } else {
                handler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE);
                handler.fill(fluid.copyWithAmount((int) Math.round(mb)), IFluidHandler.FluidAction.EXECUTE);
            }
        } else {
            BlockState cauldron = HasCauldron.getDefaultCauldron(fluid.getFluid())
                .defaultBlockState();
            IntegerProperty property = CauldronUtil.LEVEL_4;
            if (cauldron.getOptionalValue(property).isEmpty()) property = CauldronUtil.LEVEL_3;
            if (cauldron.getOptionalValue(property).isEmpty()) property = null;
            if (property != null) {
                long layer = Math.round(mb / 1000 * property.max);
                if (layer == 0) {
                    cauldron = Blocks.CAULDRON.defaultBlockState();
                } else {
                    cauldron = cauldron.setValue(property, (int) layer);
                }
            }
            cache.setBlock(pos, cauldron);
        }
        if (cache.getBlockState(pos).getBlock() instanceof IIgnitableCauldron cauldron) {
            if (cauldron.isIgnited(cache, pos) != ignited) cauldron.setIgnited(cache, pos, ignited);
        }
        if (entityCauldron != null && entityCauldron.anvilcraft$isIgnited() != ignited) {
            entityCauldron.anvilcraft$setIgnited(ignited);
        }
    }

    private static @Nullable IFluidHandler getFluidHandler(
        BlockCache cache,
        BlockPos pos,
        @Nullable IEntityCauldron entityCauldron
    ) {
        if (entityCauldron != null) return entityCauldron.getFluidHandler();
        return cache.getBlockEntity(pos) instanceof IFluidHandlerHolder holder ? holder.getFluidHandler() : null;
    }

    private static @Nullable IEntityCauldron findEntityCauldron(InWorldRecipeContext context, BlockPos pos) {
        Vec3 center = pos.getCenter();
        IEntityCauldron closest = null;
        double closestDistance = Double.POSITIVE_INFINITY;
        for (Entity entity : context.getLevel().getEntitiesOfClass(
            Entity.class,
            new AABB(pos).inflate(0.0625),
            entity -> entity.isAlive() && entity instanceof IEntityCauldron
        )) {
            double distance = entity.getBoundingBox().getCenter().distanceToSqr(center);
            if (distance >= closestDistance) continue;
            closest = (IEntityCauldron) entity;
            closestDistance = distance;
        }
        for (EntityCauldronSelector selector : ENTITY_CAULDRON_SELECTORS) {
            closest = selector.select(context, pos, closest);
        }
        return closest;
    }

    /**
     * 根据流体ID获取默认的炼药锅方块
     *
     * @param fluid 流体
     * @return 炼药锅方块
     */
    public static Block getDefaultCauldron(Fluid fluid) {
        ResourceLocation fluidId = BuiltInRegistries.FLUID.getKey(fluid);
        if (CompatUtil.F2C_TRANSFORM.containsKey(fluidId)) return CompatUtil.F2C_TRANSFORM.get(fluidId).get();
        String namespace = fluidId.getNamespace();
        String path = fluidId.getPath();
        ResourceLocation cauldron = ResourceLocation.fromNamespaceAndPath(namespace, "%s_cauldron".formatted(path));
        Holder.Reference<Block> reference = BuiltInRegistries.BLOCK.getHolder(cauldron).orElse(null);
        Block block = Blocks.WATER_CAULDRON;
        if (reference != null) block = reference.value();
        return block;
    }

    public static Block getDefaultCauldron(FluidStackPredicate fluid) {
        return fluid.fluids().stream()
            .flatMap(HolderSet::stream)
            .map(Holder::value)
            .findFirst()
            .map(HasCauldron::getDefaultCauldron)
            .orElse(Blocks.CAULDRON);
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
                FluidStackPredicate.CODEC.optionalFieldOf("fluid", FluidStackPredicate.ANY)
                    .forGetter(HasCauldron::fluid),
                Codec.INT.optionalFieldOf("consume", 0).forGetter(HasCauldron::consume),
                TRANSFORMS_CODEC.optionalFieldOf("transform", List.of())
                    .forGetter(HasCauldron::transforms),
                Codec.FLOAT.optionalFieldOf("chance", 1.0f).forGetter(HasCauldron::chance),
                Codec.BOOL.optionalFieldOf("ignited", false).forGetter(HasCauldron::ignited)
            ).apply(instance, HasCauldron::new)
        );

        /**
         * 流编解码器
         */
        public final StreamCodec<RegistryFriendlyByteBuf, HasCauldron> mapCodec = StreamCodec.composite(
            StreamCodecUtil.VEC3,
            HasCauldron::offset,
            FluidStackPredicate.STREAM_CODEC,
            HasCauldron::fluid,
            ByteBufCodecs.INT,
            HasCauldron::consume,
            FluidStack.STREAM_CODEC.apply(ByteBufCodecs.list()),
            HasCauldron::transforms,
            ByteBufCodecs.FLOAT,
            HasCauldron::chance,
            ByteBufCodecs.BOOL,
            HasCauldron::ignited,
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
        private FluidStackPredicate fluid = FluidStackPredicate.ANY;
        private int consume = 0;
        private final List<FluidStack> transforms = new ArrayList<>();
        private float chance = 1.00F;
        private boolean ignited = false;

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
            this.fluid = EMPTY_PREDICATE;
            return this;
        }

        /**
         * 设置流体ID
         *
         * @param fluid 流体ID
         * @return 构建器实例
         */
        public Builder fluid(FluidStackPredicate fluid) {
            this.fluid = fluid;
            return this;
        }

        public Builder fluid(Fluid fluid) {
            this.fluid = FluidStackPredicate.builder().fluid(fluid).build();
            return this;
        }

        public Builder fluid(Holder<Fluid> fluid) {
            this.fluid = FluidStackPredicate.builder().fluid(fluid).build();
            return this;
        }

        public Builder fluid(TagKey<Fluid> fluid) {
            this.fluid = FluidStackPredicate.builder().fluid(fluid).build();
            return this;
        }

        /**
         * 设置炼药锅方块
         *
         * @param cauldron 炼药锅方块
         * @return 构建器实例
         */
        public Builder cauldron(Block cauldron) {
            if (cauldron == Blocks.CAULDRON) return this.empty();
            return this.fluid(BuiltInRegistries.FLUID.get(WrapUtils.cauldron2Fluid(cauldron)));
        }

        public Builder transform(Fluid transform, int produce) {
            return this.transform(new FluidStack(transform, produce));
        }

        public Builder transform(Holder<Fluid> transform, int produce) {
            return this.transform(new FluidStack(transform, produce));
        }

        public Builder transform(FluidStack transform) {
            this.transforms.add(transform);
            return this;
        }

        public Builder transforms(List<FluidStack> transforms) {
            this.transforms.addAll(transforms);
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
         * 设置转换成功的概率
         *
         * @param chance 概率
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
        @SuppressWarnings("unused")
        public Builder ignite() {
            this.ignited = true;
            return this;
        }

        /**
         * 构建HasCauldron实例
         *
         * @return HasCauldron实例
         */
        public HasCauldron build() {
            return new HasCauldron(
                this.offset,
                this.fluid,
                this.consume,
                this.transforms,
                this.chance,
                this.ignited
            );
        }
    }
}
