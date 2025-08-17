package dev.dubhe.anvilcraft.recipe.anvil.wrap;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.util.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.recipe.anvil.util.WrapUtils;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.components.ChanceItemStack;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.components.HasCauldronSimple;
import lombok.Getter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 膨发配方类
 * <p>
 * 该配方用于在铁砧下落时使物品在炼药锅中膨发，需要在铁砧下方放置炼药锅作为触发条件
 * </p>
 */
@Getter
public class BulgingRecipe extends AbstractProcessRecipe<BulgingRecipe> {
    /**
     * 炼药锅条件
     */
    private final HasCauldronSimple hasCauldron;

    /**
     * 构造一个膨发配方
     *
     * @param itemIngredients 物品原料列表
     * @param results         结果物品列表
     * @param hasCauldron     炼药锅条件
     */
    public BulgingRecipe(
        List<ItemIngredientPredicate> itemIngredients,
        List<ChanceItemStack> results,
        HasCauldronSimple hasCauldron
    ) {
        super(
            new Property()
                .setItemInputOffset(new Vec3(0.0, -1.0, 0.0))
                .setInputItems(itemIngredients)
                .setItemOutputOffset(new Vec3(0.0, -1.0, 0.0))
                .setResultItems(results)
                .setCauldronOffset(new Vec3(0.0, -1.0, 0.0))
                .setHasCauldron(hasCauldron)
        );
        this.hasCauldron = hasCauldron;
    }

    @Override
    public @NotNull RecipeSerializer<BulgingRecipe> getSerializer() {
        return ModRecipeTypes.BULGING_SERIALIZER.get();
    }

    @Override
    public @NotNull RecipeType<BulgingRecipe> getType() {
        return ModRecipeTypes.BULGING_TYPE.get();
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
        return this.hasCauldron.getConsume() > 0;
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
     * 是否使用水作为流体
     *
     * @return 如果使用水返回true，否则返回false
     */
    public boolean isFromWater() {
        return this.hasCauldron.getFluid().equals(BuiltInRegistries.FLUID.getKey(Fluids.WATER));
    }

    /**
     * 膨发配方序列化器
     */
    public static class Serializer implements RecipeSerializer<BulgingRecipe> {
        /**
         * 编解码器
         */
        public static final MapCodec<BulgingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ItemIngredientPredicate.CODEC.listOf()
                .fieldOf("ingredients")
                .forGetter(BulgingRecipe::getInputItems),
            ChanceItemStack.CODEC.listOf()
                .fieldOf("results")
                .forGetter(BulgingRecipe::getResultItems),
            HasCauldronSimple.CODEC
                .forGetter(BulgingRecipe::getHasCauldron)
        ).apply(instance, BulgingRecipe::new));

        /**
         * 流编解码器
         */
        public static final StreamCodec<RegistryFriendlyByteBuf, BulgingRecipe> STREAM_CODEC = StreamCodec.composite(
            ItemIngredientPredicate.STREAM_CODEC.apply(ByteBufCodecs.list()),
            BulgingRecipe::getInputItems,
            ChanceItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()),
            BulgingRecipe::getResultItems,
            HasCauldronSimple.STREAM_CODEC,
            BulgingRecipe::getHasCauldron,
            BulgingRecipe::new
        );

        @Override
        public @NotNull MapCodec<BulgingRecipe> codec() {
            return Serializer.CODEC;
        }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, BulgingRecipe> streamCodec() {
            return Serializer.STREAM_CODEC;
        }
    }

    /**
     * 膨发配方构建器
     */
    public static class Builder extends SimpleAbstractBuilder<BulgingRecipe, Builder> {
        /**
         * 炼药锅条件构建器
         */
        private final HasCauldronSimple.Builder hasCauldron = HasCauldronSimple.empty();

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
        protected BulgingRecipe of(List<ItemIngredientPredicate> itemIngredients, List<ChanceItemStack> results) {
            return new BulgingRecipe(itemIngredients, results, this.hasCauldron.build());
        }

        @Override
        public void validate(@NotNull ResourceLocation pId) {
        }

        @Override
        public @NotNull String getType() {
            return "bulging";
        }

        @Override
        protected Builder getThis() {
            return this;
        }
    }
}