package dev.dubhe.anvilcraft.recipe.anvil.predicate.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import dev.anvilcraft.lib.v2.recipe.cache.BlockCache;
import dev.anvilcraft.lib.v2.recipe.predicate.IRecipePredicate;
import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeContext;
import dev.anvilcraft.lib.v2.util.MathUtil;
import dev.dubhe.anvilcraft.api.block.IIgnitableCauldron;
import dev.dubhe.anvilcraft.api.entity.IEntityCauldron;
import dev.dubhe.anvilcraft.api.fluid.IFluidResourceHandlerHolder;
import dev.dubhe.anvilcraft.api.fluid.LargeCauldronFluidHandler;
import dev.dubhe.anvilcraft.block.entity.LargeCauldronBlockEntity;
import dev.dubhe.anvilcraft.init.recipe.ModRecipePredicateTypes;
import dev.dubhe.anvilcraft.recipe.anvil.util.WrapUtils;
import dev.dubhe.anvilcraft.util.CauldronUtil;
import dev.dubhe.anvilcraft.util.CompatUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
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
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/// 炼药锅条件谓词
///
/// <p>用于检查指定位置是否存在特定炼药锅的谓词条件，并在配方完成后处理炼药锅中的流体</p>
///
/// @param fluid     流体ID
/// @param consume   消耗量
/// @param transform 转换后的流体ID
/// @param produce   产生量
/// @param chance    转换成功的概率
/// @param ignited   是否需要点燃
public record HasCauldron(
    Vec3 offset,
    Identifier fluid,
    int consume,
    Identifier transform,
    int produce,
    float chance,
    boolean ignited,
    @Nullable Identifier fluidTag
) implements IRecipePredicate<HasCauldron> {
    /// 空炼药锅标识
    public static final Identifier EMPTY = Identifier.withDefaultNamespace("empty");

    /// 空转换标识
    public static final Identifier NULL = Identifier.withDefaultNamespace("null");

    /// 构造一个炼药锅条件谓词
    ///
    /// @param offset    偏移量
    /// @param fluid     流体ID
    /// @param consume   消耗量
    /// @param transform 转换后的流体ID
    /// @param produce   产生量
    /// @param chance    转换成功的概率
    /// @param ignited   是否需要点燃
    public HasCauldron {
    }

    /// 创建一个空的炼药锅条件谓词
    ///
    /// @param offset 偏移量
    /// @return HasCauldron实例
    public static HasCauldron empty(Vec3 offset) {
        return new HasCauldron(offset, EMPTY, 0, NULL, 0, 1.0F, false, null);
    }

    @Override
    @SuppressWarnings("RedundantIfStatement")
    public boolean test(InWorldRecipeContext context) {
        // 由于过去在此出现了非常多的bug，在此罗列，以供测试：
        // 1. 时移不完成宝石转化
        // 2. 无水执行不消耗水的物品膨发
        // 3. 流体不足执行配方
        // 4. 流体不满1B不执行配方
        // 4. 压榨重置炼药锅——永远无法达到满锅的真实
        // 5. 一桶原油完成多份余烬金属的合成
        // 6. 锅满了，仍可以熔融宝石，溢出浪费
        // 7. 流体可以相互替代使用

        // 消耗/产生为负 否决
        if (this.consume() < 0 || this.produce() < 0) return false;
        // 概率不在0-1之间 否决
        if (this.chance() < 0 || this.chance() > 1) return false;
        // 转换为空且产生流体 否决
        if (!HasCauldron.isNotEmpty(this.transform()) && this.produce() > 0) return false;

        // 不是锅 否决
        BlockPos pos = BlockPos.containing(context.getPos().add(this.offset()));
        BlockCache cache = context.computeIfAbsent(BlockCache.BLOCK_CACHE);
        IEntityCauldron entityCauldron = findEntityCauldron(context, pos);
        if (!cache.getBlockState(pos).is(BlockTags.CAULDRONS) && entityCauldron == null) return false;

        if (cache.getBlockEntity(pos) instanceof LargeCauldronBlockEntity cauldron) {
            if (this.consume() > LargeCauldronFluidHandler.TANK_CAPACITY
                || this.produce() > LargeCauldronFluidHandler.TANK_CAPACITY) {
                return false;
            }
            return cauldron.testFluidRecipe(context, this);
        }

        // 消耗/产生比锅容量大 否决
        double capacity = HasCauldron.getCapacity(cache, pos, entityCauldron);
        if (this.consume() > capacity || this.produce() > capacity) return false;

        // 锅中流体检查不通过 否决
        Identifier curFluid = HasCauldron.getCurFluid(cache, pos, entityCauldron);
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
        if (cur > 0 && HasCauldron.isNotEmpty(this.transform()) && !curFluid.equals(this.transform()) && afterConsume != 0) return false;

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
        if (this.fluid().equals(EMPTY) && !HasCauldron.isNotEmpty(this.transform())) return;

        IEntityCauldron entityCauldron = findEntityCauldron(context, pos);
        double cur = HasCauldron.getCur(cache, pos, entityCauldron);
        double afterConsume = cur - this.consume();
        double amount = afterConsume + this.produce();

        Identifier newFluid = this.transform();
        if (!HasCauldron.isNotEmpty(newFluid)) newFluid = this.fluid();
        if (!HasCauldron.isNotEmpty(newFluid)) return;
        if (amount > 0 && HasCauldron.isNotEmpty(newFluid)) {
            HasCauldron.applyFluid(context, pos, newFluid, amount, this.ignited, entityCauldron);
        } else {
            HasCauldron.applyEmpty(context, pos, entityCauldron);
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

    /// 创建一个构建器
    ///
    /// @return 构建器实例
    public static Builder builder() {
        return new Builder();
    }

    public static boolean isNotEmpty(Identifier fluid) {
        return !fluid.equals(HasCauldron.NULL) && !fluid.equals(HasCauldron.EMPTY);
    }

    public boolean hasCheck() {
        return !this.fluid().equals(HasCauldron.NULL) || this.fluidTag() != null;
    }

    public boolean matchesFluid(Identifier currentFluid) {
        if (this.fluidTag() != null) {
            TagKey<Fluid> tag = TagKey.create(Registries.FLUID, this.fluidTag());
            ResourceKey<Fluid> key = ResourceKey.create(Registries.FLUID, currentFluid);
            return BuiltInRegistries.FLUID.get(key).map(holder -> holder.is(tag)).orElse(false);
        }
        return this.fluid().equals(currentFluid);
    }

    public static double getCapacity(BlockCache cache, BlockPos pos) {
        return getCapacity(cache, pos, null);
    }

    private static double getCapacity(BlockCache cache, BlockPos pos, @Nullable IEntityCauldron entityCauldron) {
        ResourceHandler<FluidResource> handler = getFluidHandler(cache, pos, entityCauldron);
        return handler == null ? 1000 : handler.getCapacityAsInt(0, handler.getResource(0));
    }

    /// 获取流体对应的炼药锅方块
    ///
    /// @return 炼药锅方块
    public static Identifier getCurFluid(BlockCache cache, BlockPos pos) {
        return getCurFluid(cache, pos, null);
    }

    private static Identifier getCurFluid(
        BlockCache cache,
        BlockPos pos,
        @Nullable IEntityCauldron entityCauldron
    ) {
        ResourceHandler<FluidResource> handler = getFluidHandler(cache, pos, entityCauldron);
        if (handler != null) {
            FluidResource resource = handler.getResource(0);
            return resource.isEmpty() ? EMPTY : resource.typeHolder().getKey().identifier();
        }
        return WrapUtils.cauldron2Fluid(cache.getBlockState(pos).getBlock());
    }

    /// 获取流体对应的炼药锅方块
    ///
    /// @return 炼药锅方块
    public static double getCur(BlockCache cache, BlockPos pos) {
        return getCur(cache, pos, null);
    }

    private static double getCur(BlockCache cache, BlockPos pos, @Nullable IEntityCauldron entityCauldron) {
        ResourceHandler<FluidResource> handler = getFluidHandler(cache, pos, entityCauldron);
        if (handler != null) return handler.getAmountAsInt(0);
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

    public static void applyEmpty(InWorldRecipeContext ctx, BlockPos pos) {
        applyEmpty(ctx, pos, null);
    }

    private static void applyEmpty(
        InWorldRecipeContext ctx,
        BlockPos pos,
        @Nullable IEntityCauldron entityCauldron
    ) {
        ResourceHandler<FluidResource> handler = getFluidHandler(ctx, pos, entityCauldron);
        if (handler == null) return;
        try (Transaction transaction = Transaction.openRoot()) {
            handler.extract(handler.getResource(0), Integer.MAX_VALUE, transaction);
            transaction.commit();
        }
    }

    public static void applyFluid(InWorldRecipeContext ctx, BlockPos pos, Identifier fluid, double mb, boolean ignited) {
        applyFluid(ctx, pos, fluid, mb, ignited, null);
    }

    private static void applyFluid(
        InWorldRecipeContext ctx,
        BlockPos pos,
        Identifier fluid,
        double mb,
        boolean ignited,
        @Nullable IEntityCauldron entityCauldron
    ) {
        ResourceHandler<FluidResource> handler = getFluidHandler(ctx, pos, entityCauldron);
        if (handler == null) return;
        BlockCache cache = new BlockCache(ctx.getLevel());
        if (ctx.getLevel().getBlockState(pos).getBlock() instanceof IIgnitableCauldron cauldron) {
            if (cauldron.isIgnited(cache, pos)) ignited = true;
        }
        if (entityCauldron != null && entityCauldron.anvilcraft$isIgnited()) ignited = true;
        try (Transaction transaction = Transaction.openRoot()) {
            FluidResource resource = FluidResource.of(BuiltInRegistries.FLUID.getOrThrow(ResourceKey.create(Registries.FLUID, fluid)));
            FluidResource handlerResource = handler.getResource(0);
            if (!handlerResource.equals(resource) && !handlerResource.isEmpty()) {
                handler.extract(handlerResource, Integer.MAX_VALUE, transaction);
            }
            int amount = (int) Math.round(mb);
            amount -= handler.getAmountAsInt(0);
            if (amount < 0) {
                handler.extract(resource, -amount, transaction);
            } else {
                handler.insert(resource, amount, transaction);
            }
            transaction.commit();
        }
        cache = new BlockCache(ctx.getLevel());
        if (ctx.getLevel().getBlockState(pos).getBlock() instanceof IIgnitableCauldron cauldron) {
            if (cauldron.isIgnited(cache, pos) != ignited) {
                cauldron.setIgnited(cache, pos, ignited);
                cache.accept();
            }
        }
        if (entityCauldron != null && entityCauldron.anvilcraft$isIgnited() != ignited) {
            entityCauldron.anvilcraft$setIgnited(ignited);
        }
    }

    private static @Nullable ResourceHandler<FluidResource> getFluidHandler(
        BlockCache cache,
        BlockPos pos,
        @Nullable IEntityCauldron entityCauldron
    ) {
        if (entityCauldron != null) return entityCauldron.getFluidHandler();
        return cache.getBlockEntity(pos) instanceof IFluidResourceHandlerHolder holder
               ? holder.getFluidHandler()
               : null;
    }

    private static @Nullable ResourceHandler<FluidResource> getFluidHandler(
        InWorldRecipeContext ctx,
        BlockPos pos,
        @Nullable IEntityCauldron entityCauldron
    ) {
        if (entityCauldron != null) return entityCauldron.getFluidHandler();
        return ctx.getLevel().getCapability(Capabilities.Fluid.BLOCK, pos, null);
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
        return closest;
    }

    /// 根据流体ID获取默认的炼药锅方块
    ///
    /// @param fluid 流体ID
    /// @return 炼药锅方块
    public static Block getDefaultCauldron(Identifier fluid) {
        if (fluid.equals(HasCauldron.EMPTY) || fluid.equals(HasCauldron.NULL)) return Blocks.CAULDRON;
        if (CompatUtil.F2C_TRANSFORM.containsKey(fluid)) return CompatUtil.F2C_TRANSFORM.get(fluid).get();
        String namespace = fluid.getNamespace();
        String path = fluid.getPath();
        Identifier cauldron = Identifier.fromNamespaceAndPath(namespace, "%s_cauldron".formatted(path));
        Holder.Reference<Block> reference = BuiltInRegistries.BLOCK.get(ResourceKey.create(Registries.BLOCK, cauldron)).orElse(null);
        Block block = Blocks.WATER_CAULDRON;
        if (reference != null) block = reference.value();
        return block;
    }

    @Override
    public Type getType() {
        return ModRecipePredicateTypes.HAS_CAULDRON.get();
    }

    /// HasCauldron的类型
    public static class Type implements IRecipePredicate.Type<HasCauldron> {
        /// 编解码器
        public final MapCodec<HasCauldron> codec = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Vec3.CODEC
                    .fieldOf("offset")
                    .forGetter(HasCauldron::offset),
                Identifier.CODEC
                    .optionalFieldOf("fluid", EMPTY)
                    .forGetter(HasCauldron::fluid),
                Codec.INT
                    .optionalFieldOf("consume", 0)
                    .forGetter(HasCauldron::consume),
                Identifier.CODEC
                    .optionalFieldOf("transform", NULL)
                    .forGetter(HasCauldron::transform),
                Codec.INT
                    .optionalFieldOf("produce", 0)
                    .forGetter(HasCauldron::produce),
                Codec.FLOAT
                    .optionalFieldOf("chance", 1.0F)
                    .forGetter(HasCauldron::chance),
                Codec.BOOL
                    .optionalFieldOf("ignited", false)
                    .forGetter(HasCauldron::ignited),
                Identifier.CODEC
                    .optionalFieldOf("fluidTag")
                    .forGetter(hasCauldron -> Optional.ofNullable(hasCauldron.fluidTag()))
            ).apply(instance, (offset, fluid, consume, transform, produce, chance, ignited, fluidTag) ->
                new HasCauldron(
                    offset,
                    fluid,
                    consume,
                    transform,
                    produce,
                    chance,
                    ignited,
                    fluidTag.orElse(null)
                )
            )
        );

        /// 流编解码器
        public final StreamCodec<RegistryFriendlyByteBuf, HasCauldron> mapCodec = new StreamCodec<>() {
            @Override
            public HasCauldron decode(RegistryFriendlyByteBuf buffer) {
                Vec3 offset = StreamCodecUtil.VEC3.decode(buffer);
                Identifier fluid = Identifier.STREAM_CODEC.decode(buffer);
                int consume = ByteBufCodecs.INT.decode(buffer);
                Identifier transform = Identifier.STREAM_CODEC.decode(buffer);
                int produce = ByteBufCodecs.INT.decode(buffer);
                float chance = ByteBufCodecs.FLOAT.decode(buffer);
                boolean ignited = ByteBufCodecs.BOOL.decode(buffer);
                Identifier fluidTag = ByteBufCodecs.optional(Identifier.STREAM_CODEC).decode(buffer).orElse(null);
                return new HasCauldron(offset, fluid, consume, transform, produce, chance, ignited, fluidTag);
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, HasCauldron hasCauldron) {
                StreamCodecUtil.VEC3.encode(buffer, hasCauldron.offset());
                Identifier.STREAM_CODEC.encode(buffer, hasCauldron.fluid());
                ByteBufCodecs.INT.encode(buffer, hasCauldron.consume());
                Identifier.STREAM_CODEC.encode(buffer, hasCauldron.transform());
                ByteBufCodecs.INT.encode(buffer, hasCauldron.produce());
                ByteBufCodecs.FLOAT.encode(buffer, hasCauldron.chance());
                ByteBufCodecs.BOOL.encode(buffer, hasCauldron.ignited());
                ByteBufCodecs.optional(Identifier.STREAM_CODEC).encode(
                    buffer,
                    Optional.ofNullable(hasCauldron.fluidTag())
                );
            }
        };

        @Override
        public MapCodec<HasCauldron> codec() {
            return this.codec;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, HasCauldron> streamCodec() {
            return this.mapCodec;
        }
    }

    /// 构建器类，用于构建HasCauldron实例
    public static class Builder {
        private Vec3 offset = Vec3.ZERO;
        private Identifier fluid = HasCauldron.EMPTY;
        private int consume = 0;
        private Identifier transform = HasCauldron.NULL;
        private int produce = 0;
        private float chance = 1;
        private boolean ignited = false;
        private @Nullable Identifier fluidTag;

        /// 设置偏移量
        ///
        /// @param offset 偏移量
        /// @return 构建器实例
        public Builder offset(Vec3 offset) {
            this.offset = offset;
            return this;
        }

        /// 设置偏移量
        ///
        /// @param x X坐标偏移
        /// @param y Y坐标偏移
        /// @param z Z坐标偏移
        /// @return 构建器实例
        public Builder offset(double x, double y, double z) {
            return this.offset(new Vec3(x, y, z));
        }

        /// 设置向下偏移
        ///
        /// @param below 向下偏移量
        /// @return 构建器实例
        public Builder below(double below) {
            return this.offset(Vec3.ZERO.subtract(0, below, 0));
        }

        /// 设置向下偏移1格
        ///
        /// @return 构建器实例
        public Builder below() {
            return this.below(1);
        }

        /// 设置向上偏移
        ///
        /// @param above 向上偏移量
        /// @return 构建器实例
        public Builder above(double above) {
            return this.offset(Vec3.ZERO.add(0, above, 0));
        }

        /// 设置向上偏移1格
        ///
        /// @return 构建器实例
        public Builder above() {
            return this.above(1);
        }

        /// 设置为空炼药锅
        ///
        /// @return 构建器实例
        public Builder empty() {
            this.fluid = HasCauldron.EMPTY;
            return this;
        }

        /// 设置流体ID
        ///
        /// @param fluid 流体ID
        /// @return 构建器实例
        public Builder fluid(Identifier fluid) {
            this.fluid = fluid;
            return this;
        }

        /// 设置炼药锅方块
        ///
        /// @param cauldron 炼药锅方块
        /// @return 构建器实例
        public Builder cauldron(Block cauldron) {
            this.fluid = WrapUtils.cauldron2Fluid(cauldron);
            return this;
        }

        /// 设置转换后的流体ID
        ///
        /// @param transform 转换后的流体ID
        /// @return 构建器实例
        public Builder transform(Identifier transform) {
            this.transform = transform;
            if (!HasCauldron.isNotEmpty(this.fluid)) this.fluid = HasCauldron.NULL;
            return this;
        }

        /// 设置消耗指定单位流体
        ///
        /// @param consume 消耗量
        /// @return 构建器实例
        public Builder consume(int consume) {
            this.consume = consume;
            return this;
        }

        /// 设置产生指定单位流体
        ///
        /// @param produce 产生量
        /// @return 构建器实例
        public Builder produce(int produce) {
            this.produce = produce;
            return this;
        }

        /// 设置转换成功的概率
        ///
        /// @param chance 概率
        /// @return 构建器实例
        public Builder chance(float chance) {
            this.chance = MathUtil.clampWithProportion(chance, 0, 1);
            return this;
        }

        /// 设置需要点燃锅
        ///
        /// @return 构建器实例
        public Builder ignite() {
            this.ignited = true;
            return this;
        }

        public Builder fluidTag(Identifier fluidTag) {
            this.fluidTag = fluidTag;
            if (!HasCauldron.isNotEmpty(this.fluid)) this.fluid = HasCauldron.NULL;
            return this;
        }

        /// 构建HasCauldron实例
        ///
        /// @return HasCauldron实例
        public HasCauldron build() {
            return new HasCauldron(
                this.offset,
                this.fluid,
                this.consume,
                this.transform,
                this.produce,
                this.chance,
                this.ignited,
                this.fluidTag
            );
        }
    }
}
