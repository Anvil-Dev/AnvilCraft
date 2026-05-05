package dev.dubhe.anvilcraft.recipe.anvil.wrap;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.anvilcraft.lib.v2.util.predicate.ChanceBlockState;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.builder.AbstractRecipeBuilder;
import dev.dubhe.anvilcraft.recipe.anvil.predicate.block.HasAnvil;
import dev.dubhe.anvilcraft.recipe.anvil.predicate.block.HasCauldron;
import dev.dubhe.anvilcraft.recipe.anvil.util.WrapUtils;
import dev.dubhe.anvilcraft.recipe.component.HasCauldronSimple;
import lombok.Getter;
import net.minecraft.core.Vec3i;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;

import java.util.function.Consumer;

/**
 * 压榨配方类
 *
 * <p>该配方用于在铁砧下落时压榨方块，需要在铁砧下方放置炼药锅作为收集容器</p>
 */
@Getter
public class SqueezingRecipe extends AbstractProcessRecipe<SqueezingRecipe> {
    /**
     * 构造一个压榨配方
     *
     * @param ingredient  原料方块列表
     * @param result      结果方块列表
     * @param hasCauldron 炼药锅条件
     */
    public SqueezingRecipe(
        BlockStatePredicate ingredient,
        ChanceBlockState result,
        HasCauldronSimple hasCauldron,
        HasAnvil hasAnvil
    ) {
        super(
            new Property()
                .setBlockInputOffset(new Vec3i(0, -1, 0))
                .setConsumeInputBlocks(true)
                .setInputBlocks(ingredient)
                .setCauldronOffset(new Vec3i(0, -2, 0))
                .setHasCauldron(hasCauldron)
                .setHasAnvil(hasAnvil)
                .setBlockOutputOffset(new Vec3i(0, -1, 0))
                .setResultBlocks(result)
        );
    }

    @Override
    public RecipeSerializer<SqueezingRecipe> getSerializer() {
        return ModRecipeTypes.SQUEEZING_SERIALIZER.get();
    }

    @Override
    public RecipeType<SqueezingRecipe> getType() {
        return ModRecipeTypes.SQUEEZING_TYPE.get();
    }

    /**
     * 创建一个构建器实例
     *
     * @return 构建器实例
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 是否产生流体
     *
     * @return 如果产生流体返回true，否则返回false
     */
    public boolean isProduceFluid() {
        HasCauldronSimple hasCauldron = this.getHasCauldron();
        return HasCauldron.isNotEmpty(hasCauldron.transform()) && this.getHasCauldron().produce() > 0;
    }

    /**
     * 压榨配方序列化器
     */
    public static class Serializer implements RecipeSerializer<SqueezingRecipe> {
        /**
         * 编解码器
         */
        public static final MapCodec<SqueezingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BlockStatePredicate.CODEC
                .fieldOf("ingredient")
                .forGetter(SqueezingRecipe::getFirstInputBlock),
            ChanceBlockState.CODEC
                .codec()
                .fieldOf("result")
                .forGetter(SqueezingRecipe::getFirstResultBlock),
            HasCauldronSimple.CODEC
                .forGetter(SqueezingRecipe::getHasCauldron),
            HasAnvil.CODEC.codec()
                .optionalFieldOf("anvil", HasAnvil.DEFAULT)
                .forGetter(SqueezingRecipe::getHasAnvil)
        ).apply(instance, SqueezingRecipe::new));

        /**
         * 流编解码器
         */
        public static final StreamCodec<RegistryFriendlyByteBuf, SqueezingRecipe> STREAM_CODEC = StreamCodec.composite(
            BlockStatePredicate.STREAM_CODEC,
            SqueezingRecipe::getFirstInputBlock,
            ChanceBlockState.STREAM_CODEC,
            SqueezingRecipe::getFirstResultBlock,
            HasCauldronSimple.STREAM_CODEC,
            SqueezingRecipe::getHasCauldron,
            HasAnvil.STREAM_CODEC,
            SqueezingRecipe::getHasAnvil,
            SqueezingRecipe::new
        );

        @Override
        public MapCodec<SqueezingRecipe> codec() {
            return Serializer.CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, SqueezingRecipe> streamCodec() {
            return Serializer.STREAM_CODEC;
        }
    }

    /**
     * 压榨配方构建器
     */
    public static class Builder extends AbstractRecipeBuilder<SqueezingRecipe> {
        /**
         * 原料列表
         */
        private BlockStatePredicate ingredient = null;

        /**
         * 结果列表
         */
        private ChanceBlockState result = null;

        /**
         * 炼药锅条件构建器
         */
        private final HasCauldronSimple.Builder hasCauldron = HasCauldronSimple.empty();

        /**
         * 铁砧条件
         */
        private HasAnvil hasAnvil = HasAnvil.DEFAULT;

        /**
         * 添加原料方块
         *
         * @param ingredient 原料方块谓词
         * @return 构建器实例
         */
        public Builder requires(BlockStatePredicate ingredient) {
            this.ingredient = ingredient;
            return this;
        }

        /**
         * 添加原料方块
         *
         * @param ingredient 原料方块
         * @return 构建器实例
         */
        public Builder requires(Block ingredient) {
            return this.requires(BlockStatePredicate.builder().of(ingredient).build());
        }

        /**
         * 添加原料方块（标签形式）
         *
         * @param ingredient 原料方块标签
         * @return 构建器实例
         */
        public Builder requires(TagKey<Block> ingredient) {
            return this.requires(BlockStatePredicate.builder().of(ingredient).build());
        }

        /**
         * 添加结果方块
         *
         * @param result 结果方块
         * @return 构建器实例
         */
        public Builder result(ChanceBlockState result) {
            this.result = result;
            return this;
        }

        /**
         * 添加结果方块（指定概率）
         *
         * @param result 结果方块
         * @param chance 概率
         * @return 构建器实例
         */
        public Builder result(Block result, float chance) {
            return this.result(new ChanceBlockState(result.defaultBlockState(), chance));
        }

        /**
         * 添加结果方块（默认概率为1.0f）
         *
         * @param result 结果方块
         * @return 构建器实例
         */
        public Builder result(Block result) {
            return this.result(result, 1.0f);
        }

        /**
         * 设置炼药锅流体
         *
         * @param fluid 流体ID
         * @return 构建器实例
         */
        public Builder cauldron(ResourceLocation fluid) {
            this.hasCauldron.fluid(fluid);
            return this;
        }

        /**
         * 设置炼药锅方块
         *
         * @param cauldron 炼药锅方块
         * @return 构建器实例
         */
        public Builder cauldron(Block cauldron) {
            this.cauldron(WrapUtils.cauldron2Fluid(cauldron));
            return this;
        }

        /**
         * 设置转换后的流体
         *
         * @param transform 转换后的流体ID
         * @return 构建器实例
         */
        public Builder transform(ResourceLocation transform) {
            this.hasCauldron.transform(transform);
            return this;
        }

        /**
         * 设置转换后的炼药锅方块
         *
         * @param transform 转换后的炼药锅方块
         * @return 构建器实例
         */
        public Builder transform(Block transform) {
            this.hasCauldron.transform(WrapUtils.cauldron2Fluid(transform));
            return this;
        }

        /**
         * 设置是否产生流体
         *
         * @param produce 是否产生流体
         * @return 构建器实例
         */
        public Builder produce(int produce) {
            if (produce <= 0) return this;
            this.hasCauldron.produce(produce);
            return this;
        }

        /**
         * 设置是否消耗流体
         *
         * @param consume 是否消耗流体
         * @return 构建器实例
         */
        public Builder consume(int consume) {
            if (consume <= 0) return this;
            this.hasCauldron.consume(consume);
            return this;
        }

        /**
         * 设置转换成功的概率
         *
         * @param chance 转换成功的概率
         * @return 构建器实例
         */
        public Builder chance(float chance) {
            if (chance <= 0) return this;
            this.hasCauldron.chance(chance);
            return this;
        }

        /**
         * 设置铁砧条件
         *
         * @param anvil 铁砧方块
         * @return 构建器实例
         */
        public Builder anvil(Block anvil) {
            this.hasAnvil = new HasAnvil(BlockStatePredicate.builder().of(anvil));
            return this;
        }

        /**
         * 设置铁砧条件
         *
         * @param anvil 铁砧方块标签
         * @return 构建器实例
         */
        public Builder anvil(TagKey<Block> anvil) {
            this.hasAnvil = new HasAnvil(BlockStatePredicate.builder().of(anvil));
            return this;
        }

        /**
         * 设置铁砧条件
         *
         * @param consumer 铁砧条件谓词消费者
         * @return 构建器实例
         */
        public Builder anvil(Consumer<BlockStatePredicate.Builder> consumer) {
            BlockStatePredicate.Builder builder = BlockStatePredicate.builder();
            consumer.accept(builder);
            this.hasAnvil = new HasAnvil(builder);
            return this;
        }

        /**
         * 设置浮霜铁砧条件
         *
         * @return 构建器实例
         */
        public Builder frostAnvil() {
            this.hasAnvil = HasAnvil.frostOnly();
            return this;
        }

        /**
         * 设置非浮霜铁砧条件
         *
         * @return 构建器实例
         */
        public Builder noFrostAnvil() {
            this.hasAnvil = HasAnvil.noFrost();
            return this;
        }

        @Override
        public SqueezingRecipe buildRecipe() {
            return new SqueezingRecipe(this.ingredient, this.result, hasCauldron.build(), this.hasAnvil);
        }

        @Override
        public void validate(ResourceLocation id) {
        }

        @Override
        public String getType() {
            return "bulging";
        }

        @Override
        public Item getResult() {
            return WrapUtils.getItem(this.result);
        }
    }
}