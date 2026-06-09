package dev.dubhe.anvilcraft.recipe.anvil.wrap;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.recipe.outcome.IRecipeOutcome;
import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeContext;
import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeData;
import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.anvilcraft.lib.v2.util.predicate.ChanceItemStack;
import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.BurningHeaterBlock;
import dev.dubhe.anvilcraft.block.HeaterBlock;
import dev.dubhe.anvilcraft.block.entity.BurningHeaterBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeOutcomeTypes;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.outcome.RoyalPreferenceOutcome;
import dev.dubhe.anvilcraft.recipe.anvil.predicate.block.HasCauldron;
import dev.dubhe.anvilcraft.recipe.anvil.util.WrapUtils;
import dev.dubhe.anvilcraft.recipe.component.HasCauldronSimple;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 超级加热配方类
 *
 * <p>该配方用于在铁砧下落时超级加热物品，需要在铁砧下方放置加热器作为热源</p>
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
        super(createProperty(itemIngredients, results, hasCauldron));
    }

    private static Property createProperty(
        List<ItemIngredientPredicate> itemIngredients,
        List<ChanceItemStack> results,
        HasCauldronSimple hasCauldron
    ) {
        Property property = new Property()
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
                    .of(ModBlocks.HEATER.get(), ModBlocks.BURNING_HEATER.get())
                    .with(HeaterBlock.OVERLOAD, false)
                    .or()
                    .with(BurningHeaterBlock.LEVEL, 2)
                    .build()
            );

        property.addOutcome(new ConsumeFuel());

        for (ChanceItemStack result : results) {
            if (result.stack().is(ModItems.ROYAL_STEEL_INGOT.get()) || result.stack().is(ModBlocks.ROYAL_STEEL_BLOCK.get().asItem())) {
                property.addOutcome(new RoyalPreferenceOutcome(result));
            }
        }
        return property;
    }

    @Override
    public RecipeSerializer<SuperHeatingRecipe> getSerializer() {
        return ModRecipeTypes.SUPER_HEATING_SERIALIZER.get();
    }

    @Override
    public RecipeType<SuperHeatingRecipe> getType() {
        return ModRecipeTypes.SUPER_HEATING_TYPE.get();
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
     * 是否消耗流体
     *
     * @return 如果消耗流体返回true，否则返回false
     */
    public boolean isConsumeFluid() {
        HasCauldronSimple hasCauldron = this.getHasCauldron();
        return HasCauldron.isNotEmpty(hasCauldron.fluid()) && this.getHasCauldron().consume() > 0;
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
        public MapCodec<SuperHeatingRecipe> codec() {
            return Serializer.CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, SuperHeatingRecipe> streamCodec() {
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
        public Builder fluid(ResourceLocation fluid) {
            this.hasCauldron.fluid(fluid);
            return this;
        }

        /**
         * 设置炼药锅方块
         *
         * @param cauldron 炼药锅方块
         * @return 构建器实例
         */
        public Builder fluid(Block cauldron) {
            this.fluid(WrapUtils.cauldron2Fluid(cauldron));
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
         * @param cauldron 转换后的炼药锅方块
         * @return 构建器实例
         */
        public Builder transform(Block cauldron) {
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
            this.hasCauldron.produce(produce);
            return this;
        }

        @Override
        protected SuperHeatingRecipe of(List<ItemIngredientPredicate> itemIngredients, List<ChanceItemStack> results) {
            return new SuperHeatingRecipe(itemIngredients, results, this.hasCauldron.build());
        }

        @Override
        public void validate(ResourceLocation id) {
            if (itemIngredients.isEmpty()) {
                throw new IllegalArgumentException("Recipe ingredients must not be empty, RecipeId: " + id);
            }
        }

        @Override
        public String getType() {
            return "super_heating";
        }

        @Override
        protected Builder getThis() {
            return this;
        }
    }

    /**
     * 消耗燃烧加热器燃料的配方结果
     *
     * <p>当铁砧合成成功时，检查下方是否存在点燃状态的燃烧加热器，
     * 如果存在则消耗240秒（4800tick）的燃烧时间</p>
     */
    public record ConsumeFuel() implements IRecipeOutcome<ConsumeFuel> {
        /**
         * 每次合成消耗的燃烧时间（tick），240秒 = 4800tick
         */
        public static final int FUEL_COST_TICKS = 240 * 20;

        /**
         * 标记一次铁砧落地事件中是否已消耗过燃料，防止多个物品匹配时重复消耗
         */
        public static final InWorldRecipeData<Boolean> FUEL_CONSUMED =
            InWorldRecipeData.of(AnvilCraft.of("super_heating_fuel_consumed"), false);

        @Override
        public IRecipeOutcome.Type<ConsumeFuel> getType() {
            return ModRecipeOutcomeTypes.CONSUME_BURNING_HEATER_FUEL.get();
        }

        @Override
        public void accept(InWorldRecipeContext context) {
            if (context.get(FUEL_CONSUMED)) return;
            context.put(FUEL_CONSUMED, true);
            ServerLevel level = context.getLevel();
            Vec3 pos = context.getPos();
            BlockPos anvilBlockPos = BlockPos.containing(pos.x(), pos.y() + 0.5, pos.z());
            BlockPos heaterPos = anvilBlockPos.below(2);
            BlockState state = level.getBlockState(heaterPos);
            if (!(state.getBlock() instanceof BurningHeaterBlock)) return;
            if (state.getValue(BurningHeaterBlock.LEVEL) != 2) return;
            if (!(level.getBlockEntity(heaterPos) instanceof BurningHeaterBlockEntity be)) return;
            be.consumeBurnTime(FUEL_COST_TICKS);
        }

        public static class Type implements IRecipeOutcome.Type<ConsumeFuel> {
            @Override
            public MapCodec<ConsumeFuel> codec() {
                return MapCodec.unit(new ConsumeFuel());
            }

            @Override
            public StreamCodec<RegistryFriendlyByteBuf, ConsumeFuel> streamCodec() {
                return StreamCodec.unit(new ConsumeFuel());
            }
        }
    }
}