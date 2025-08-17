package dev.dubhe.anvilcraft.recipe.anvil.wrap;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.block.HeaterBlock;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.util.BlockStatePredicate;
import dev.dubhe.anvilcraft.recipe.anvil.util.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.recipe.anvil.util.WrapUtils;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.components.ChanceItemStack;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.components.HasCauldronSimple;
import lombok.Getter;
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
 * 超级加热配方类
 * <p>
 * 该配方用于在铁砧下落时超级加热物品，需要在铁砧下方放置加热器作为热源
 * </p>
 */
@Getter
public class SuperHeatingRecipe extends AbstractProcessRecipe<SuperHeatingRecipe> {
    /**
     * 构造一个超级加热配方
     *
     * @param itemIngredients 物品原料列表
     * @param results         结果物品列表
     * @param hasCauldron     炼药锅条件
     */
    public SuperHeatingRecipe(
        List<ItemIngredientPredicate> itemIngredients,
        List<ChanceItemStack> results,
        HasCauldronSimple hasCauldron
    ) {
        super(
            new Property()
                .setItemInputOffset(Vec3.ZERO)
                .setInputItems(itemIngredients)
                .setItemOutputOffset(new Vec3(0.0, -1.0, 0.0))
                .setResultItems(results)
                .setCauldronOffset(new Vec3(0.0, -1.0, 0.0))
                .setHasCauldron(hasCauldron)
                .setBlockInputOffset(new Vec3(0.0, -2.0, 0.0))
                .setInputBlocks(
                    BlockStatePredicate.builder()
                        .of(ModBlocks.HEATER.get())
                        .with(HeaterBlock.OVERLOAD, false)
                        .build()
                )
        );
    }

    @Override
    public @NotNull RecipeSerializer<SuperHeatingRecipe> getSerializer() {
        return ModRecipeTypes.SUPER_HEATING_SERIALIZER.get();
    }

    @Override
    public @NotNull RecipeType<SuperHeatingRecipe> getType() {
        return ModRecipeTypes.SUPER_HEATING_TYPE.get();
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
     * 超级加热配方序列化器
     */
    public static class Serializer implements RecipeSerializer<SuperHeatingRecipe> {
        /**
         * 编解码器
         */
        private static final MapCodec<SuperHeatingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ItemIngredientPredicate.CODEC.listOf()
                .optionalFieldOf("ingredients", List.of())
                .forGetter(SuperHeatingRecipe::getInputItems),
            ChanceItemStack.CODEC.listOf()
                .optionalFieldOf("results", List.of())
                .forGetter(SuperHeatingRecipe::getResultItems),
            HasCauldronSimple.CODEC
                .forGetter(SuperHeatingRecipe::getHasCauldron)
        ).apply(instance, SuperHeatingRecipe::new));

        /**
         * 流编解码器
         */
        private static final StreamCodec<RegistryFriendlyByteBuf, SuperHeatingRecipe> STREAM_CODEC = StreamCodec.composite(
            ItemIngredientPredicate.STREAM_CODEC.apply(ByteBufCodecs.list()),
            SuperHeatingRecipe::getInputItems,
            ChanceItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()),
            SuperHeatingRecipe::getResultItems,
            HasCauldronSimple.STREAM_CODEC,
            SuperHeatingRecipe::getHasCauldron,
            SuperHeatingRecipe::new
        );

        @Override
        public @NotNull MapCodec<SuperHeatingRecipe> codec() {
            return Serializer.CODEC;
        }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, SuperHeatingRecipe> streamCodec() {
            return Serializer.STREAM_CODEC;
        }
    }

    /**
     * 超级加热配方构建器
     */
    public static class Builder extends SimpleAbstractBuilder<SuperHeatingRecipe, Builder> {
        /**
         * 炼药锅条件构建器
         */
        HasCauldronSimple.Builder hasCauldron = HasCauldronSimple.empty();

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

        @Override
        protected SuperHeatingRecipe of(List<ItemIngredientPredicate> itemIngredients, List<ChanceItemStack> results) {
            return new SuperHeatingRecipe(itemIngredients, results, this.hasCauldron.build());
        }

        @Override
        public void validate(@NotNull ResourceLocation pId) {
            if (itemIngredients.isEmpty()) {
                throw new IllegalArgumentException("Recipe ingredients must not be empty, RecipeId: " + pId);
            }
        }

        @Override
        public @NotNull String getType() {
            return "super_heating";
        }

        @Override
        protected Builder getThis() {
            return this;
        }
    }
}