package dev.dubhe.anvilcraft.recipe.anvil.wrap;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.api.heat.HeatTier;
import dev.dubhe.anvilcraft.block.CorruptedBeaconBlock;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.outcome.ProduceHeat;
import dev.dubhe.anvilcraft.recipe.anvil.predicate.block.HasCauldron;
import dev.dubhe.anvilcraft.recipe.anvil.util.Distance;
import dev.dubhe.anvilcraft.recipe.anvil.util.WrapUtils;
import dev.dubhe.anvilcraft.recipe.component.BlockStatePredicate;
import dev.dubhe.anvilcraft.recipe.component.ChanceItemStack;
import dev.dubhe.anvilcraft.recipe.component.HasCauldronSimple;
import dev.dubhe.anvilcraft.recipe.component.ItemIngredientPredicate;
import lombok.Getter;
import net.minecraft.core.Vec3i;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 时移配方类
 * <p>
 * 该配方用于在铁砧下落时产生时移效果，需要在铁砧下方放置腐化信标作为触发条件
 * </p>
 */
@Getter
public class TimeWarpRecipe extends AbstractProcessRecipe<TimeWarpRecipe> {
    /**
     * 构造一个时移配方
     *
     * @param itemIngredients 物品原料列表
     * @param results         结果物品列表
     * @param hasCauldron     炼药锅条件
     * @param produceHeat     产热信息
     */
    public TimeWarpRecipe(
        List<ItemIngredientPredicate> itemIngredients,
        List<ChanceItemStack> results,
        HasCauldronSimple hasCauldron,
        ProduceHeat produceHeat
    ) {
        super(
            new Property()
                .setItemInputOffset(new Vec3(0.0, -0.375, 0.0))
                .setItemInputRange(new Vec3(0.75, 0.75, 0.75))
                .setInputItems(itemIngredients)
                .setItemOutputOffset(new Vec3(0.0, -0.75, 0.0))
                .setResultItems(results)
                .setCauldronOffset(new Vec3i(0, -1, 0))
                .setHasCauldron(hasCauldron)
                .setBlockInputOffset(new Vec3i(0, -2, 0))
                .setInputBlocks(
                    BlockStatePredicate.builder()
                        .of(ModBlocks.CORRUPTED_BEACON.get())
                        .with(CorruptedBeaconBlock.LIT, true)
                        .build()
                )
                .setProduceHeat(produceHeat)
        );
    }

    @Override
    public @NotNull RecipeSerializer<TimeWarpRecipe> getSerializer() {
        return ModRecipeTypes.TIME_WARP_SERIALIZER.get();
    }

    @Override
    public @NotNull RecipeType<TimeWarpRecipe> getType() {
        return ModRecipeTypes.TIME_WARP_TYPE.get();
    }

    /**
     * 创建一个构建器实例
     *
     * @return 构建器实例
     */
    public static @NotNull Builder builder() {
        return new Builder();
    }

    /**
     * 是否消耗流体
     *
     * @return 如果消耗流体返回true，否则返回false
     */
    public boolean isConsumeFluid() {
        HasCauldronSimple hasCauldron = this.getHasCauldron();
        return HasCauldron.isNotEmpty(hasCauldron.getFluid()) && this.getHasCauldron().getConsume() > 0;
    }

    /**
     * 是否产生流体
     *
     * @return 如果产生流体返回true，否则返回false
     */
    public boolean isProduceFluid() {
        HasCauldronSimple hasCauldron = this.getHasCauldron();
        return HasCauldron.isNotEmpty(hasCauldron.getTransform()) && this.getHasCauldron().getConsume() < 0;
    }

    /**
     * 时移配方序列化器
     */
    public static class Serializer implements RecipeSerializer<TimeWarpRecipe> {
        /**
         * 编解码器
         */
        private static final MapCodec<TimeWarpRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ItemIngredientPredicate.CODEC.listOf()
                .optionalFieldOf("ingredients", List.of())
                .forGetter(TimeWarpRecipe::getInputItems),
            ChanceItemStack.CODEC.listOf()
                .optionalFieldOf("results", List.of())
                .forGetter(TimeWarpRecipe::getResultItems),
            HasCauldronSimple.CODEC
                .forGetter(TimeWarpRecipe::getHasCauldron),
            ProduceHeat.Type.MAP_CODEC
                .forGetter(TimeWarpRecipe::getProduceHeat)
        ).apply(instance, TimeWarpRecipe::new));

        /**
         * 流编解码器
         */
        private static final StreamCodec<RegistryFriendlyByteBuf, TimeWarpRecipe> STREAM_CODEC = StreamCodec.composite(
            ItemIngredientPredicate.STREAM_CODEC.apply(ByteBufCodecs.list()),
            TimeWarpRecipe::getInputItems,
            ChanceItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()),
            TimeWarpRecipe::getResultItems,
            HasCauldronSimple.STREAM_CODEC,
            TimeWarpRecipe::getHasCauldron,
            ProduceHeat.Type.STREAM_CODEC,
            TimeWarpRecipe::getProduceHeat,
            TimeWarpRecipe::new
        );

        @Override
        public @NotNull MapCodec<TimeWarpRecipe> codec() {
            return Serializer.CODEC;
        }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, TimeWarpRecipe> streamCodec() {
            return Serializer.STREAM_CODEC;
        }
    }

    /**
     * 时移配方构建器
     */
    public static class Builder extends SimpleAbstractBuilder<TimeWarpRecipe, Builder> {
        /**
         * 炼药锅条件构建器
         */
        HasCauldronSimple.Builder hasCauldron = HasCauldronSimple.empty();

        /**
         * 产热信息构建器
         */
        ProduceHeat.Builder produceHeat = ProduceHeat.builder();

        /**
         * 添加热量
         *
         * @param tier     热量等级
         * @param duration 持续时间
         * @return 构建器实例
         */
        public Builder heat(HeatTier tier, int duration) {
            this.produceHeat.heat(tier, duration);
            return this;
        }

        /**
         * 设置距离
         *
         * @param distance 距离
         * @return 构建器实例
         */
        public Builder distance(Distance distance) {
            this.produceHeat.distance(distance);
            return this;
        }

        /**
         * 设置距离（指定类型、距离和方向）
         *
         * @param type         距离类型
         * @param distance     距离
         * @param isHorizontal 是否水平方向
         * @return 构建器实例
         */
        public Builder distance(Distance.Type type, int distance, boolean isHorizontal) {
            this.produceHeat.distance(type, distance, isHorizontal);
            return this;
        }

        /**
         * 设置欧几里得距离（指定距离和方向）
         *
         * @param distance     距离
         * @param isHorizontal 是否水平方向
         * @return 构建器实例
         */
        public Builder distanceEuclidean(int distance, boolean isHorizontal) {
            this.produceHeat.distanceEuclidean(distance, isHorizontal);
            return this;
        }

        /**
         * 设置欧几里得距离（默认距离为1）
         *
         * @param isHorizontal 是否水平方向
         * @return 构建器实例
         */
        public Builder distanceEuclidean(boolean isHorizontal) {
            this.produceHeat.distanceEuclidean(1, isHorizontal);
            return this;
        }

        /**
         * 设置欧几里得距离（指定距离，默认为水平方向）
         *
         * @param distance 距离
         * @return 构建器实例
         */
        public Builder distanceEuclidean(int distance) {
            this.produceHeat.distanceEuclidean(distance);
            return this;
        }

        /**
         * 设置欧几里得距离（默认距离为1，默认为水平方向）
         *
         * @return 构建器实例
         */
        public Builder distanceEuclidean() {
            this.produceHeat.distanceEuclidean();
            return this;
        }

        /**
         * 设置曼哈顿距离（指定距离和方向）
         *
         * @param distance     距离
         * @param isHorizontal 是否水平方向
         * @return 构建器实例
         */
        public Builder distanceManhattan(int distance, boolean isHorizontal) {
            this.produceHeat.distanceManhattan(distance, isHorizontal);
            return this;
        }

        /**
         * 设置曼哈顿距离（默认距离为1）
         *
         * @param isHorizontal 是否水平方向
         * @return 构建器实例
         */
        public Builder distanceManhattan(boolean isHorizontal) {
            this.produceHeat.distanceManhattan(1, isHorizontal);
            return this;
        }

        /**
         * 设置曼哈顿距离（指定距离，默认为水平方向）
         *
         * @param distance 距离
         * @return 构建器实例
         */
        public Builder distanceManhattan(int distance) {
            this.produceHeat.distanceManhattan(distance);
            return this;
        }

        /**
         * 设置曼哈顿距离（默认距离为1，默认为水平方向）
         *
         * @return 构建器实例
         */
        public Builder distanceManhattan() {
            this.produceHeat.distanceManhattan();
            return this;
        }

        /**
         * 设置切比雪夫距离（指定距离和方向）
         *
         * @param distance    距离
         * @param isHorizontal 是否水平方向
         * @return 构建器实例
         */
        public Builder distanceChebyshev(int distance, boolean isHorizontal) {
            this.produceHeat.distanceChebyshev(distance, isHorizontal);
            return this;
        }

        /**
         * 设置切比雪夫距离（默认距离为1）
         *
         * @param isHorizontal 是否水平方向
         * @return 构建器实例
         */
        public Builder distanceChebyshev(boolean isHorizontal) {
            this.produceHeat.distanceChebyshev(isHorizontal);
            return this;
        }

        /**
         * 设置切比雪夫距离（指定距离，默认为水平方向）
         *
         * @param distance 距离
         * @return 构建器实例
         */
        public Builder distanceChebyshev(int distance) {
            this.produceHeat.distanceChebyshev(distance);
            return this;
        }

        /**
         * 设置切比雪夫距离（默认距离为1，默认为水平方向）
         *
         * @return 构建器实例
         */
        public Builder distanceChebyshev() {
            this.produceHeat.distanceChebyshev();
            return this;
        }

        /**
         * 设置流体
         *
         * @param fluid 流体ID
         * @return 构建器实例
         */
        public @NotNull Builder fluid(ResourceLocation fluid) {
            this.hasCauldron.fluid(fluid);
            return this;
        }

        /**
         * 设置炼药锅方块
         *
         * @param cauldron 炼药锅方块
         * @return 构建器实例
         */
        public @NotNull Builder fluid(Block cauldron) {
            this.fluid(WrapUtils.cauldron2Fluid(cauldron));
            return this;
        }

        /**
         * 设置转换后的流体
         *
         * @param transform 转换后的流体ID
         * @return 构建器实例
         */
        public @NotNull Builder transform(ResourceLocation transform) {
            this.hasCauldron.transform(transform);
            return this;
        }

        /**
         * 设置转换后的炼药锅方块
         *
         * @param cauldron 转换后的炼药锅方块
         * @return 构建器实例
         */
        public @NotNull Builder transform(Block cauldron) {
            this.transform(WrapUtils.cauldron2Fluid(cauldron));
            return this;
        }

        /**
         * 设置消耗量
         *
         * @param consume 消耗量
         * @return 构建器实例
         */
        public Builder consume(int consume) {
            this.hasCauldron.consume(consume);
            return this;
        }

        /**
         * 设置产生量
         *
         * @param produce 产量
         * @return 构建器实例
         */
        public Builder produce(int produce) {
            this.consume(-produce);
            return this;
        }

        @Override
        protected TimeWarpRecipe of(List<ItemIngredientPredicate> itemIngredients, List<ChanceItemStack> results) {
            return new TimeWarpRecipe(itemIngredients, results, this.hasCauldron.build(), this.produceHeat.build());
        }

        @Override
        public void validate(@NotNull ResourceLocation pId) {
            if (itemIngredients.isEmpty()) {
                throw new IllegalArgumentException("Recipe ingredients must not be empty, RecipeId: " + pId);
            }
        }

        @Override
        public @NotNull String getType() {
            return "time_warp";
        }

        @Override
        protected Builder getThis() {
            return this;
        }
    }
}