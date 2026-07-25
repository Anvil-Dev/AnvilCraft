package dev.dubhe.anvilcraft.recipe.anvil.wrap;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.util.predicate.ChanceItemStack;
import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.predicate.block.HasCauldron;
import dev.dubhe.anvilcraft.recipe.anvil.util.WrapUtils;
import dev.dubhe.anvilcraft.recipe.component.HasCauldronSimple;
import lombok.Getter;
import net.minecraft.core.Vec3i;
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

import java.util.List;

/** An anvil-processing recipe between item ingredients and cauldron fluid. */
@Getter
public class SolidLiquidRecipe extends AbstractProcessRecipe<SolidLiquidRecipe> {
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
    public RecipeSerializer<SolidLiquidRecipe> getSerializer() {
        return ModRecipeTypes.SOLID_LIQUID_SERIALIZER.get();
    }

    @Override
    public RecipeType<SolidLiquidRecipe> getType() {
        return ModRecipeTypes.SOLID_LIQUID_TYPE.get();
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isConsumeFluid() {
        HasCauldronSimple hasCauldron = this.getHasCauldron();
        return HasCauldron.isNotEmpty(hasCauldron.fluid()) && this.getHasCauldron().consume() > 0;
    }

    public boolean isProduceFluid() {
        HasCauldronSimple hasCauldron = this.getHasCauldron();
        return HasCauldron.isNotEmpty(hasCauldron.transform()) && this.getHasCauldron().produce() > 0;
    }

    public boolean isFromWater() {
        return this.getHasCauldron().fluid().equals(BuiltInRegistries.FLUID.getKey(Fluids.WATER));
    }

    public static class Serializer implements RecipeSerializer<SolidLiquidRecipe> {
        public static final MapCodec<SolidLiquidRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
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
        ).apply(instance, SolidLiquidRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, SolidLiquidRecipe> STREAM_CODEC = StreamCodec.composite(
            ItemIngredientPredicate.STREAM_CODEC.apply(ByteBufCodecs.list()),
            SolidLiquidRecipe::getInputItems,
            ChanceItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()),
            SolidLiquidRecipe::getResultItems,
            HasCauldronSimple.STREAM_CODEC,
            SolidLiquidRecipe::getHasCauldron,
            ByteBufCodecs.INT,
            SolidLiquidRecipe::maxEfficiency,
            SolidLiquidRecipe::new
        );

        @Override
        public MapCodec<SolidLiquidRecipe> codec() {
            return Serializer.CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, SolidLiquidRecipe> streamCodec() {
            return Serializer.STREAM_CODEC;
        }
    }

    public static class Builder extends SimpleAbstractBuilder<SolidLiquidRecipe, Builder> {
        private final HasCauldronSimple.Builder hasCauldron = HasCauldronSimple.empty();
        private int maxEfficiency = Integer.MAX_VALUE;

        public Builder cauldron(ResourceLocation fluid) {
            this.hasCauldron.fluid(fluid);
            return this;
        }

        public Builder cauldron(Block cauldron) {
            this.cauldron(WrapUtils.cauldron2Fluid(cauldron));
            return this;
        }

        public Builder transform(ResourceLocation transform) {
            this.hasCauldron.transform(transform);
            return this;
        }

        public Builder transform(Block transform) {
            this.hasCauldron.transform(WrapUtils.cauldron2Fluid(transform));
            return this;
        }

        public Builder produce(int produce) {
            if (produce <= 0) return this;
            this.hasCauldron.produce(produce);
            return this;
        }

        public Builder consume(int consume) {
            if (consume <= 0) return this;
            this.hasCauldron.consume(consume);
            return this;
        }

        public Builder fluidTag(ResourceLocation fluidTag) {
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
        public void validate(ResourceLocation id) {
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
