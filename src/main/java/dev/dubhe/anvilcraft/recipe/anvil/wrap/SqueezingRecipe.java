package dev.dubhe.anvilcraft.recipe.anvil.wrap;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.InWorldRecipeContext;
import dev.dubhe.anvilcraft.recipe.anvil.builder.AbstractRecipeBuilder;
import dev.dubhe.anvilcraft.recipe.anvil.util.BlockStatePredicate;
import dev.dubhe.anvilcraft.recipe.anvil.util.WrapUtils;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.components.ChanceBlockState;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.components.HasCauldronSimple;
import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * 压榨配方类
 * <p>
 * 该配方用于在铁砧下落时压榨方块，需要在铁砧下方放置炼药锅作为收集容器
 * </p>
 */
@Getter
public class SqueezingRecipe extends AbstractProcessRecipe<SqueezingRecipe> {
    /**
     * 炼药锅条件
     */
    private final HasCauldronSimple hasCauldron;

    /**
     * 构造一个压榨配方
     *
     * @param ingredients 原料方块列表
     * @param results     结果方块列表
     * @param hasCauldron 炼药锅条件
     */
    public SqueezingRecipe(
        List<BlockStatePredicate> ingredients,
        List<ChanceBlockState> results,
        HasCauldronSimple hasCauldron
    ) {
        super(
            new Property()
                .setCauldronOffset(new Vec3(0.0, -1.0, 0.0))
                .setHasCauldron(hasCauldron)
                .setBlockInputOffset(new Vec3(0.0, -2.0, 0.0))
                .setInputBlocks(ingredients)
                .setItemInputOffset(new Vec3(0.0, -1.0, 0.0))
                .setResultBlocks(results)
        );
        this.hasCauldron = hasCauldron;
    }

    @Override
    public boolean matches(@NotNull InWorldRecipeContext context, @NotNull Level level) {
        return super.matches(context, level);
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull InWorldRecipeContext context, HolderLookup.@NotNull Provider provider) {
        return super.assemble(context, provider);
    }

    @Override
    public @NotNull RecipeSerializer<SqueezingRecipe> getSerializer() {
        return ModRecipeTypes.SQUEEZING_SERIALIZER.get();
    }

    @Override
    public @NotNull RecipeType<SqueezingRecipe> getType() {
        return ModRecipeTypes.SQUEEZING_TYPE.get();
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
     * 是否产生流体
     *
     * @return 如果产生流体返回true，否则返回false
     */
    public boolean isProduceFluid() {
        return this.hasCauldron.getConsume() < 0;
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
                .listOf()
                .fieldOf("ingredients")
                .forGetter(SqueezingRecipe::getInputBlocks),
            ChanceBlockState.CODEC
                .codec()
                .listOf()
                .fieldOf("results")
                .forGetter(SqueezingRecipe::getResultBlocks),
            HasCauldronSimple.CODEC
                .forGetter(SqueezingRecipe::getHasCauldron)
        ).apply(instance, SqueezingRecipe::new));

        /**
         * 流编解码器
         */
        public static final StreamCodec<RegistryFriendlyByteBuf, SqueezingRecipe> STREAM_CODEC = StreamCodec.composite(
            BlockStatePredicate.STREAM_CODEC.apply(ByteBufCodecs.list()),
            SqueezingRecipe::getInputBlocks,
            ChanceBlockState.STREAM_CODEC.apply(ByteBufCodecs.list()),
            SqueezingRecipe::getResultBlocks,
            HasCauldronSimple.STREAM_CODEC,
            SqueezingRecipe::getHasCauldron,
            SqueezingRecipe::new
        );

        @Override
        public @NotNull MapCodec<SqueezingRecipe> codec() {
            return Serializer.CODEC;
        }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, SqueezingRecipe> streamCodec() {
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
        private final List<BlockStatePredicate> ingredients = new ArrayList<>();

        /**
         * 结果列表
         */
        private final List<ChanceBlockState> results = new ArrayList<>();

        /**
         * 炼药锅条件构建器
         */
        private final HasCauldronSimple.Builder hasCauldron = HasCauldronSimple.empty();

        /**
         * 添加原料方块
         *
         * @param ingredient 原料方块谓词
         * @return 构建器实例
         */
        public Builder requires(BlockStatePredicate ingredient) {
            this.ingredients.add(ingredient);
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
            this.results.add(result);
            return this;
        }

        /**
         * 添加结果方块（指定概率）
         *
         * @param result 结果方块
         * @param chance 概率
         * @return 构建器实例
         */
        public Builder result(@NotNull Block result, float chance) {
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
         * @param produceFluid 是否产生流体
         * @return 构建器实例
         */
        public Builder produceFluid(boolean produceFluid) {
            if (!produceFluid) return this;
            this.hasCauldron.consume(-1);
            return this;
        }

        /**
         * 设置是否消耗流体
         *
         * @param consumeFluid 是否消耗流体
         * @return 构建器实例
         */
        public Builder consumeFluid(boolean consumeFluid) {
            if (!consumeFluid) return this;
            this.hasCauldron.consume(1);
            return this;
        }

        @Override
        public @NotNull SqueezingRecipe buildRecipe() {
            return new SqueezingRecipe(ingredients, results, hasCauldron.build());
        }

        @Override
        public void validate(@NotNull ResourceLocation pId) {
        }

        @Override
        public @NotNull String getType() {
            return "bulging";
        }

        @Override
        public @NotNull Item getResult() {
            return WrapUtils.getItem(this.results);
        }
    }
}