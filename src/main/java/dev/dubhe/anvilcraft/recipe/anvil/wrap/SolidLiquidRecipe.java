package dev.dubhe.anvilcraft.recipe.anvil.wrap;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.util.predicate.ChanceItemStack;
import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeSerializers;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.predicate.block.HasCauldron;
import dev.dubhe.anvilcraft.recipe.anvil.util.WrapUtils;
import dev.dubhe.anvilcraft.recipe.component.HasCauldronSimple;
import lombok.Getter;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/// 膨发配方类
///
/// <p>该配方用于在铁砧下落时使物品在炼药锅中膨发，需要在铁砧下方放置炼药锅作为触发条件</p>
@Getter
public class SolidLiquidRecipe extends AbstractProcessRecipe<SolidLiquidRecipe> {
    public static final RecipeSerializer<SolidLiquidRecipe> SERIALIZER = new RecipeSerializer<>(
        RecordCodecBuilder.mapCodec(instance -> instance.group(
            ItemIngredientPredicate.CODEC.listOf()
                .fieldOf("ingredients")
                .forGetter(SolidLiquidRecipe::getInputItems),
            ChanceItemStack.CODEC.listOf()
                .fieldOf("results")
                .forGetter(SolidLiquidRecipe::getResultItems),
            HasCauldronSimple.CODEC
                .forGetter(SolidLiquidRecipe::getHasCauldron),
            Codec.INT
                .optionalFieldOf("max_efficiency", Integer.MAX_VALUE)
                .forGetter(SolidLiquidRecipe::maxEfficiency)
        ).apply(instance, SolidLiquidRecipe::new)),
        StreamCodec.composite(
            ItemIngredientPredicate.STREAM_CODEC.apply(ByteBufCodecs.list()),
            SolidLiquidRecipe::getInputItems,
            ChanceItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()),
            SolidLiquidRecipe::getResultItems,
            HasCauldronSimple.STREAM_CODEC,
            SolidLiquidRecipe::getHasCauldron,
            ByteBufCodecs.INT,
            SolidLiquidRecipe::maxEfficiency,
            SolidLiquidRecipe::new
        )
    );

    /// 构造一个膨发配方
    ///
    /// @param itemIngredients 物品原料列表
    /// @param results         结果物品列表
    /// @param hasCauldron     炼药锅条件
    public SolidLiquidRecipe(
        List<ItemIngredientPredicate> itemIngredients,
        List<ChanceItemStack> results,
        HasCauldronSimple hasCauldron
    ) {
        this(itemIngredients, results, hasCauldron, Integer.MAX_VALUE);
    }

    public SolidLiquidRecipe(
        List<ItemIngredientPredicate> itemIngredients,
        List<ChanceItemStack> results,
        HasCauldronSimple hasCauldron,
        int maxEfficiency
    ) {
        super(
            new Property()
                .setItemInputOffset(new Vec3(0.0, -0.375, 0.0))
                .setItemInputRange(new Vec3(0.75, 0.75, 0.75))
                .setInputItems(itemIngredients)
                .setItemOutputOffset(new Vec3(0.0, -0.75, 0.0))
                .setResultItems(results)
                .setCauldronOffset(new Vec3i(0, -1, 0))
                .setHasCauldron(hasCauldron),
            maxEfficiency
        );
    }

    @Override
    public RecipeType<SolidLiquidRecipe> getType() {
        return ModRecipeTypes.SOLID_LIQUID.get();
    }

    @Override
    public RecipeSerializer<SolidLiquidRecipe> getSerializer() {
        return ModRecipeSerializers.SOLID_LIQUID.get();
    }

    /// 创建一个构建器实例
    ///
    /// @return 构建器实例
    public static Builder builder() {
        return new Builder();
    }

    /// 是否消耗流体
    ///
    /// @return 如果消耗流体返回true，否则返回false
    public boolean isConsumeFluid() {
        HasCauldronSimple hasCauldron = this.getHasCauldron();
        return HasCauldron.isNotEmpty(hasCauldron.fluid()) && this.getHasCauldron().consume() > 0;
    }

    /// 是否产生流体
    ///
    /// @return 如果产生流体返回true，否则返回false
    public boolean isProduceFluid() {
        HasCauldronSimple hasCauldron = this.getHasCauldron();
        return HasCauldron.isNotEmpty(hasCauldron.transform()) && this.getHasCauldron().produce() > 0;
    }

    /// 是否使用水作为流体
    ///
    /// @return 如果使用水返回true，否则返回false
    public boolean isFromWater() {
        return this.getHasCauldron().fluid().equals(BuiltInRegistries.FLUID.getKey(Fluids.WATER));
    }

    /// 膨发配方构建器
    public static class Builder extends SimpleAbstractBuilder<SolidLiquidRecipe, Builder> {
        /// 炼药锅条件构建器
        private final HasCauldronSimple.Builder hasCauldron = HasCauldronSimple.empty();
        private int maxEfficiency = Integer.MAX_VALUE;

        /// 设置炼药锅流体
        ///
        /// @param fluid 流体ID
        ///
        /// @return 构建器实例
        public Builder cauldron(Identifier fluid) {
            this.hasCauldron.fluid(fluid);
            return this;
        }

        /// 设置炼药锅方块
        ///
        /// @param cauldron 炼药锅方块
        ///
        /// @return 构建器实例
        public Builder cauldron(Block cauldron) {
            this.cauldron(WrapUtils.cauldron2Fluid(cauldron));
            return this;
        }

        /// 设置转换后的流体
        ///
        /// @param transform 转换后的流体ID
        ///
        /// @return 构建器实例
        public Builder transform(Identifier transform) {
            this.hasCauldron.transform(transform);
            return this;
        }

        /// 设置转换后的炼药锅方块
        ///
        /// @param transform 转换后的炼药锅方块
        ///
        /// @return 构建器实例
        public Builder transform(Block transform) {
            this.hasCauldron.transform(WrapUtils.cauldron2Fluid(transform));
            return this;
        }

        /// 设置是否产生流体
        ///
        /// @param produce 是否产生流体
        ///
        /// @return 构建器实例
        public Builder produce(int produce) {
            if (produce <= 0) return this;
            this.hasCauldron.produce(produce);
            return this;
        }

        /// 设置是否消耗流体
        ///
        /// @param consume 消耗流体
        ///
        /// @return 构建器实例
        public Builder consume(int consume) {
            if (consume <= 0) return this;
            this.hasCauldron.consume(consume);
            return this;
        }

        public Builder fluidTag(Identifier fluidTag) {
            this.hasCauldron.fluidTag(fluidTag);
            return this;
        }

        public Builder maxEfficiency(int maxEfficiency) {
            this.maxEfficiency = maxEfficiency;
            return this;
        }

        @Override
        protected SolidLiquidRecipe of(List<ItemIngredientPredicate> itemIngredients, List<ChanceItemStack> results) {
            return new SolidLiquidRecipe(itemIngredients, results, this.hasCauldron.build(), this.maxEfficiency);
        }

        @Override
        public void validate(Identifier id) {
            HasCauldronSimple cauldron = this.hasCauldron.build();
            if (!HasCauldron.isNotEmpty(cauldron.fluid()) && cauldron.fluidTag() == null) {
                throw new IllegalArgumentException("Recipe fluid must not be empty, RecipeId: " + id);
            }
            if (this.results.isEmpty() && !HasCauldron.isNotEmpty(cauldron.transform())) {
                throw new IllegalArgumentException("Recipe must have an item or fluid result, RecipeId: " + id);
            }
        }

        @Override
        public String getType() {
            return "solid_liquid";
        }

        @Override
        protected Builder getThis() {
            return this;
        }
    }
}
